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
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.SSLContext;

import com.janilla.http.HttpHandler;
import com.janilla.http.HttpProtocol;
import com.janilla.net.Net;
import com.janilla.net.Server;
import com.janilla.reflect.Factory;
import com.janilla.util.Util;
import com.janilla.web.ApplicationHandlerBuilder;
import com.janilla.web.Handle;
import com.janilla.web.JsonRenderer;
import com.janilla.web.Render;

public class JanillaIde {

	public static void main(String[] args) {
		try {
			var pp = new Properties();
			try (var is = JanillaIde.class.getResourceAsStream("configuration.properties")) {
				pp.load(is);
				if (args.length > 0) {
					var p = args[0];
					if (p.startsWith("~"))
						p = System.getProperty("user.home") + p.substring(1);
					pp.load(Files.newInputStream(Path.of(p)));
				}
			}
			var ji = new JanillaIde(pp);
			Server s;
			{
				var a = new InetSocketAddress(
						Integer.parseInt(ji.configuration.getProperty("janilla-ide.server.port")));
				SSLContext sc;
				try (var is = Net.class.getResourceAsStream("testkeys")) {
					sc = Net.getSSLContext("JKS", is, "passphrase".toCharArray());
				}
				var p = ji.factory.create(HttpProtocol.class,
						Map.of("handler", ji.handler, "sslContext", sc, "useClientMode", false));
				s = new Server(a, p);
			}
			s.serve();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public Properties configuration;

	public Factory factory;

//	public Persistence persistence;

	public HttpHandler handler;

	public JanillaIde(Properties configuration) {
		this.configuration = configuration;
		factory = new Factory();
		factory.setTypes(Util.getPackageClasses(getClass().getPackageName()).toList());
		factory.setSource(this);
//		{
//			var p = configuration.getProperty("janilla-ide.database.file");
//			if (p.startsWith("~"))
//				p = System.getProperty("user.home") + p.substring(1);
//			var pb = factory.create(ApplicationPersistenceBuilder.class, Map.of("databaseFile", Path.of(p)));
//			persistence = pb.build();
//		}
		handler = factory.create(ApplicationHandlerBuilder.class).build();
		try {
			var ps = configuration.getProperty("janilla-ide.workspace.directory");
			if (ps.startsWith("~"))
				ps = System.getProperty("user.home") + ps.substring(1);
			var d = Path.of(ps);
			if (!Files.exists(d))
				Files.createDirectories(d);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public JanillaIde application() {
		return this;
	}

	@Handle(method = "GET", path = "/|/entry/(.+)")
	public Index index(Path path) throws IOException {
		return new Index(new State(visit(workspaceDirectory(), path), path != null ? List.of(path) : List.of(),
				path != null ? 0 : -1));
	}

	protected List<Entry> visit(Path directory, Path path) throws IOException {
		try (var ee = Files.list(directory)) {
			return ee.filter(x -> !x.getFileName().toString().startsWith(".")).map(x -> {
				try {
					var n = x.getFileName();
					Entry e = Files.isDirectory(x) ? new Directory(n.toString(),
							path != null && path.startsWith(n) ? visit(directory.resolve(n), n.relativize(path)) : null,
							null) : new File(n.toString(), null);
					return e;
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}).sorted(Comparator.comparing(x -> ((Entry) x).name().toLowerCase())).toList();
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}

	protected Path workspaceDirectory() {
		var ps = configuration.getProperty("janilla-ide.workspace.directory");
		if (ps.startsWith("~"))
			ps = System.getProperty("user.home") + ps.substring(1);
		return Path.of(ps);
	}

	@Render(template = "index.html")
	public record Index(State state) {
	}

	@Render(renderer = JsonRenderer.class)
	public record State(List<Entry> entries, List<Path> paths, int activeIndex) {
	}
}
