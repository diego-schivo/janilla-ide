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
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.janilla.http.HttpResponse;
import com.janilla.http.HttpWritableByteChannel;
import com.janilla.web.Handle;

public class ExecutionApi {

	protected static final Pattern MODULE = Pattern.compile("^module ([\\w.]+)", Pattern.MULTILINE);

	public Properties configuration;

	protected Queue<Task.Builder> taskBuilders;

	protected Task task;

	protected Lock lock = new ReentrantLock();

	protected Condition started = lock.newCondition();

	protected Condition completed = lock.newCondition();

	@Handle(method = "POST", path = "/api/executions/(.*)")
	public Execution create(Path path) throws IOException, InterruptedException {
		lock.lock();
		try {
			while (task != null)
				completed.await();
			taskBuilders = new ArrayDeque<>();
			var wd = workspaceDirectory();
			var fp = wd.resolve(path);
			if (!Files.isRegularFile(fp) || !fp.getFileName().toString().toLowerCase().endsWith(".java"))
				throw new IllegalArgumentException("path");
			var jd = jdkDirectory();
			var msp = new LinkedHashMap<String, Path>();
			Files.walkFileTree(wd, new SimpleFileVisitor<>() {

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					var p = dir.resolve("module-info.java");
					if (Files.isRegularFile(p)) {
						var s = Files.readString(p);
						var m = MODULE.matcher(s);
						m.find();
						msp.put(m.group(1), wd.relativize(dir));
						return FileVisitResult.SKIP_SUBTREE;
					}
					return FileVisitResult.CONTINUE;
				}
			});
			var msp1 = msp.entrySet().stream().filter(x -> path.startsWith(x.getValue())).findFirst().orElse(null);
			var mn = msp1.getKey();
			var sd = msp1.getValue();
			var c = new ArrayList<String>();
			c.addAll(List.of(jd.resolve("bin/javac").toString(), "-d", "target", "--enable-preview", "--module",
					msp.keySet().stream().collect(Collectors.joining(","))));
			c.addAll(msp.entrySet().stream()
					.flatMap(x -> Stream.of("--module-source-path", x.getKey() + "=" + x.getValue())).toList());
			c.addAll(List.of("-parameters", "--release", "23"));
			var pb = new ProcessBuilder(c);
			pb.directory(wd.toFile()).redirectErrorStream(true);
			taskBuilders.add(new ProcessTask.Builder(pb));
			taskBuilders.add(new ThreadTask.Builder(() -> {
				for (var x : msp.entrySet())
					try {
						Files.walkFileTree(wd.resolve(x.getValue()), new SimpleFileVisitor<>() {

							@Override
							public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
								var n = file.getFileName().toString();
								if (!n.startsWith(".") && !n.endsWith(".java")) {
									var f = wd.resolve("target", x.getKey())
											.resolve(x.getValue().relativize(wd.relativize(file)));
									var d = f.getParent();
									if (!Files.exists(d))
										Files.createDirectories(d);
									var t1 = Files.getLastModifiedTime(file);
									var t2 = Files.exists(f) ? Files.getLastModifiedTime(f) : null;
									if (!t1.equals(t2))
										Files.copy(file, f, StandardCopyOption.REPLACE_EXISTING);
								}
								return FileVisitResult.CONTINUE;
							}
						});
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
			}));
			var ps = sd.relativize(path).toString();
			var cn = ps.substring(0, ps.length() - ".java".length()).replace('/', '.');
			pb = new ProcessBuilder(jd.resolve("bin/java").toString(), "--module-path", "target", "--enable-preview",
					"--module", mn + "/" + cn);
			pb.directory(wd.toFile()).redirectErrorStream(true);
			taskBuilders.add(new ProcessTask.Builder(pb));
			task = taskBuilders.remove().start();
			started.signal();
		} finally {
			lock.unlock();
		}
		return null;
	}

	@Handle(method = "GET", path = "/api/executions/(.*)")
	public void read(Path path, HttpResponse response) throws IOException, InterruptedException {
		response.setHeaderValue(":status", "200");
		response.setHeaderValue("content-type", "text/event-stream");
		response.setHeaderValue("cache-control", "no-cache");
		var ch = (HttpWritableByteChannel) response.getBody();
		lock.lock();
		try {
			while (task == null)
				started.await();
			var ir = task.inputReader();
			for (;;) {
				var l = ir != null ? ir.readLine() : null;
				if (l != null)
					ch.write(ByteBuffer.wrap(("data: " + l + "\n\n").getBytes()));
				else if (task.waitFor() == 0 && !taskBuilders.isEmpty())
					task = taskBuilders.remove().start();
				else
					break;
			}
			ch.write(ByteBuffer.wrap(("data: " + "ok" + "\n\n").getBytes()), true);
			task = null;
			completed.signal();
		} finally {
			lock.unlock();
		}
	}

	@Handle(method = "DELETE", path = "/api/executions/(.*)")
	public Execution delete(Path path) {
		task.destroy();
		return null;
	}

	protected Path jdkDirectory() {
		var ps = configuration.getProperty("janilla-ide.jdk.directory");
		if (ps.startsWith("~"))
			ps = System.getProperty("user.home") + ps.substring(1);
		return Path.of(ps);
	}

	protected Path workspaceDirectory() {
		var ps = configuration.getProperty("janilla-ide.workspace.directory");
		if (ps.startsWith("~"))
			ps = System.getProperty("user.home") + ps.substring(1);
		return Path.of(ps);
	}
}
