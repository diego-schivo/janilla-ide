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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import com.janilla.http.HttpResponse;
import com.janilla.http.HttpWritableByteChannel;
import com.janilla.persistence.Persistence;
import com.janilla.web.Bind;
import com.janilla.web.Handle;

public class ProjectApi {

	public Persistence persistence;

	private Process process;

	private Lock lock = new ReentrantLock();

	private Condition started = lock.newCondition();

	private Condition completed = lock.newCondition();

	private Path directory = Path.of(System.getProperty("user.home")).resolve("gittmp");

	@Handle(method = "GET", path = "/api/projects")
	public Stream<Project> list() {
		var pc = persistence.crud(Project.class);
		return pc.read(pc.list());
	}

	@Handle(method = "POST", path = "/api/projects/clone")
	public boolean clone(Repository repository) throws IOException, InterruptedException {
		System.out.println("ProjectApi.clone, repository=" + repository);
		lock.lock();
		try {
			while (process != null)
				completed.await();
			var pb = new ProcessBuilder("git", "clone", "--progress", repository.url).redirectErrorStream(true);
			pb.directory(directory.toFile());
			process = pb.start();
			process.onExit().thenAccept(_ -> {
				var i1 = repository.url.lastIndexOf('/') + 1;
				var i2 = repository.url.indexOf('.', i1);
				var n = repository.url.substring(i1, i2);
				persistence.crud(Project.class).create(new Project(null, n));
			});
			started.signal();
		} finally {
			lock.unlock();
		}
		return true;
	}

	@Handle(method = "POST", path = "/api/projects/run")
	public boolean run(Project project) throws IOException, InterruptedException {
		System.out.println("ProjectApi.run, project=" + project);
		project = persistence.crud(Project.class).read(project.id());
		lock.lock();
		try {
			while (process != null)
				completed.await();
			var pb = new ProcessBuilder("/home/diegos/apache-maven-3.9.9/bin/mvn", "compile", "exec:exec")
					.redirectErrorStream(true);
			pb.directory(directory.resolve(project.name()).toFile());
			process = pb.start();
			started.signal();
		} finally {
			lock.unlock();
		}
		return true;
	}

	@Handle(method = "GET", path = "/api/projects/expand")
	public List<String> expand(@Bind("id") long id) throws IOException {
		System.out.println("ProjectApi.expand, id=" + id);
		var project = persistence.crud(Project.class).read(id);
		try (var pp = Files.list(directory.resolve(project.name()))) {
			return pp.map(x -> x.getFileName().toString()).toList();
		}
	}

	@Handle(method = "POST", path = "/api/projects/terminate")
	public boolean terminate() {
		System.out.println("ProjectApi.terminate");
		Stream.concat(process.toHandle().children(), Stream.of(process.toHandle())).forEach(x -> {
			System.out.println("ProjectApi.terminate, pid=" + x.pid());
			x.destroy();
		});
		return false;
	}

	@Handle(method = "GET", path = "/api/projects/output")
	public void output(HttpResponse response) throws IOException, InterruptedException {
		response.setHeaderValue(":status", "200");
		response.setHeaderValue("content-type", "text/event-stream");
		response.setHeaderValue("cache-control", "no-cache");
		var b = (HttpWritableByteChannel) response.getBody();
		for (;;) {
			lock.lock();
			try {
				while (process == null)
					started.await();
				var l = process.inputReader().readLine();
				if (l == null) {
					b.write(ByteBuffer.wrap(("data: " + "ok" + "\n\n").getBytes()), true);
					process = null;
					completed.signal();
					break;
				}
				b.write(ByteBuffer.wrap(("data: " + l + "\n\n").getBytes()));
			} finally {
				lock.unlock();
			}
		}
	}

	public record Repository(String url) {
	}
}
