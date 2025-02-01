/*
 * MIT License
 *
 * Copyright (c) 2024-2025 Diego Schivo
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.janilla.ide;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.janilla.http.HttpResponse;
import com.janilla.http.HttpWritableByteChannel;
import com.janilla.web.Handle;

public class ImportApi {

	public Properties configuration;

	private Process process;

	private Lock lock = new ReentrantLock();

	private Condition started = lock.newCondition();

	private Condition completed = lock.newCondition();

	@Handle(method = "POST", path = "/api/imports")
	public Import create(Import import1) throws IOException, InterruptedException {
		lock.lock();
		try {
			while (process != null)
				completed.await();
			var wd = workspaceDirectory();
			var pb = new ProcessBuilder("git", "clone", "--progress", import1.url()).redirectErrorStream(true);
			pb.directory(wd.toFile());
			process = pb.start();
			started.signal();
		} finally {
			lock.unlock();
		}
		return null;
	}

	@Handle(method = "GET", path = "/api/imports")
	public void read(HttpResponse response) throws IOException, InterruptedException {
		response.setHeaderValue(":status", "200");
		response.setHeaderValue("content-type", "text/event-stream");
		response.setHeaderValue("cache-control", "no-cache");
		var ch = (HttpWritableByteChannel) response.getBody();
		lock.lock();
		try {
			while (process == null)
				started.await();
			for (;;) {
				var l = process.inputReader().readLine();
				if (l != null)
					ch.write(ByteBuffer.wrap(("data: " + l + "\n\n").getBytes()));
				else
					break;
			}
			ch.write(ByteBuffer.wrap(("data: " + "ok" + "\n\n").getBytes()), true);
			process = null;
			completed.signal();
		} finally {
			lock.unlock();
		}
	}

	protected Path workspaceDirectory() {
		var ps = configuration.getProperty("janilla-ide.workspace.directory");
		if (ps.startsWith("~"))
			ps = System.getProperty("user.home") + ps.substring(1);
		return Path.of(ps);
	}
}
