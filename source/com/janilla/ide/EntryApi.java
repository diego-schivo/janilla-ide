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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.janilla.json.MapAndType;
import com.janilla.web.Bind;
import com.janilla.web.Handle;

public class EntryApi {

	protected static final Pattern HEAD = Pattern.compile("^ref: refs/heads/([\\w-]+)", Pattern.MULTILINE);

	protected static final Pattern MODULE = Pattern.compile("^module ([\\w.]+)", Pattern.MULTILINE);

	protected static final Map<Path, Task> RUN_TASKS = new ConcurrentHashMap<>();

	public Properties configuration;

	@Handle(method = "GET", path = "/api/entries/(.*)")
	public Entry read(Path path) throws IOException {
		var p = workspaceDirectory();
		if (path != null)
			p = p.resolve(path);
		var n = path != null ? path.getFileName().toString() : null;
		if (Files.isDirectory(p)) {
			List<Entry> ee;
			try (var pp = Files.list(p)) {
				ee = pp.filter(x -> !x.getFileName().toString().startsWith(".")).map(x -> {
					var n2 = x.getFileName().toString();
					Entry e = Files.isDirectory(x) ? new Directory(n2, null, null) : new File(n2, null);
					return e;
				}).sorted(Comparator.comparing(x -> ((Entry) x).name().toLowerCase())).toList();
			}
			var p2 = p.resolve(".git");
			Repository r;
			if (Files.isDirectory(p2)) {
				var s = Files.readString(p2.resolve("HEAD"));
				var m = HEAD.matcher(s);
				m.find();
				var b = m.group(1);
				List<String> bb;
				try (var pp = Files.list(p2.resolve("refs", "heads"))) {
					bb = pp.map(x -> x.getFileName().toString())
							.sorted(Comparator.comparing(x -> ((String) x).toLowerCase())).toList();
				}
				r = new Repository(b, bb);
			} else
				r = null;
			return new Directory(n, ee, r);
		} else
			return new File(n, Files.readString(p));
	}

	@Handle(method = "PUT", path = "/api/entries/(.*)")
	public Entry update(Path path, @Bind(resolver = MapAndType.DollarTypeResolver.class) Entry entry, Update update)
			throws IOException, InterruptedException {
//		System.out.println("EntryApi.update, path=" + path);
		var wd = workspaceDirectory();
		var p = path != null ? wd.resolve(path) : wd;
		switch (entry) {
		case File f:
			if (update.action == null) {
				if (f.text() != null) {
					var n = path.getFileName().toString();
					var i = n.lastIndexOf('.');
					var i2 = switch (n.substring(i + 1)) {
					case "java" -> new JavaIndent();
					default -> null;
					};
					var s = i2 != null ? i2.apply(f.text()) : f.text();
					Files.writeString(p, s);
					return new File(n, s);
				}
			} else
				switch (update.action) {
				case "run":
					var t = RUN_TASKS.compute(path, (_, v) -> {
						if (v != null)
							v.destroy();
						return runBuilder(path).start();
					});
					Thread.startVirtualThread(() -> {
						try {
							var r = t.inputReader();
							for (;;) {
								var l = r != null ? r.readLine() : null;
								if (l == null)
									break;
								OutputApi.QUEUE.put(new Event(null, l));
							}
							t.waitFor();
						} catch (Exception e) {
							throw new RuntimeException(e);
						} finally {
							RUN_TASKS.remove(path);
						}
					});
					break;
				case "terminate":
					var t2 = RUN_TASKS.remove(path);
//					System.out.println("EntryApi.update, t2=" + t2);
					if (t2 != null)
						t2.destroy();
					break;
				}
			break;
		case Directory _:
			switch (update.action) {
			case "create":
				var f = p.resolve(update.path);
				var d = f.getParent();
				if (!Files.exists(d))
					Files.createDirectories(d);
				Files.createFile(f);
				OutputApi.QUEUE.put(new Event("refresh-directory", path != null ? Map.of("path", path) : null));
				break;
			case "delete":
				for (var n : update.names) {
					var s = p.resolve(n);
					if (Files.isDirectory(s))
						Files.walkFileTree(s, new SimpleFileVisitor<>() {

							@Override
							public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
								Files.delete(file);
								return FileVisitResult.CONTINUE;
							}

							@Override
							public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
								Files.delete(dir);
								return FileVisitResult.CONTINUE;
							}
						});
					else
						Files.delete(s);
				}
				OutputApi.QUEUE.put(new Event("refresh-directory", path != null ? Map.of("path", path) : null));
				break;
			case "clone": {
				var pb = new ProcessBuilder("git", "clone", update.url);
				pb.directory(p.toFile()).redirectErrorStream(true);
				var ir = pb.start().inputReader();
				Thread.startVirtualThread(() -> {
					try {
						for (;;) {
							var l = ir.readLine();
							if (l == null)
								break;
//							System.out.println("EntryApi.update, l=" + l);
							OutputApi.QUEUE.put(new Event(null, l));
						}
						OutputApi.QUEUE.put(new Event("refresh-directory", path != null ? Map.of("path", path) : null));
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				});
				break;
			}
			case "switch": {
				var pb = new ProcessBuilder("git", "switch", update.branch);
				pb.directory(p.toFile()).redirectErrorStream(true);
				var ir = pb.start().inputReader();
				Thread.startVirtualThread(() -> {
					try {
						for (;;) {
							var l = ir.readLine();
							if (l == null)
								break;
//							System.out.println("EntryApi.update, l=" + l);
							OutputApi.QUEUE.put(new Event(null, l));
						}
						OutputApi.QUEUE.put(new Event("refresh-directory", path != null ? Map.of("path", path) : null));
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				});
				break;
			}
			}
			break;
		}
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

	protected Task.Builder runBuilder(Path path) {
		var wd = workspaceDirectory();
		var fp = wd.resolve(path);
		if (!Files.isRegularFile(fp) || !fp.getFileName().toString().endsWith(".java"))
			throw new IllegalArgumentException("path");
		var jd = jdkDirectory();
		var msp = new LinkedHashMap<String, Path>();
		try {
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
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		var msp1 = msp.entrySet().stream().filter(x -> path.startsWith(x.getValue())).findFirst().orElse(null);
		var mn = msp1.getKey();
		var sd = msp1.getValue();
		var c = new ArrayList<String>();
		c.addAll(List.of(jd.resolve("bin/javac").toString(), "-d", "target", "--enable-preview", "--module",
				msp.keySet().stream().collect(Collectors.joining(","))));
		c.addAll(msp.entrySet().stream()
				.flatMap(x -> Stream.of("--module-source-path", x.getKey() + "=" + x.getValue())).toList());
		c.addAll(List.of("-parameters", "--release", "24"));
		var pb1 = new ProcessBuilder(c);
		pb1.directory(wd.toFile()).redirectErrorStream(true);
		var ps = sd.relativize(path).toString();
		var cn = ps.substring(0, ps.length() - ".java".length()).replace('/', '.');
		var pb2 = new ProcessBuilder(jd.resolve("bin/java").toString(), "--module-path", "target", "--enable-preview",
				"--module", mn + "/" + cn);
		pb2.directory(wd.toFile()).redirectErrorStream(true);
		var tbb = new ChainingTask.Builder(new ProcessTask.Builder(pb1), new ThreadTask.Builder(() -> {
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
		}), new ProcessTask.Builder(pb2));
		return tbb;
	}

//	public static class EntryResolver implements UnaryOperator<Converter.MapType> {
//
//		@Override
//		public Converter.MapType apply(Converter.MapType mt) {
//			var t = switch ((String) mt.map().get("$type")) {
//			case "Directory" -> Directory.class;
//			case "File" -> File.class;
//			default -> throw new RuntimeException();
//			};
//			return new Converter.MapType(mt.map(), t);
//		}
//	}

	public record Update(String action, Path path, Set<String> names, String url, String branch) {
	}
}
