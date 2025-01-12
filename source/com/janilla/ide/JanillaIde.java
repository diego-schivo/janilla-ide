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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import com.janilla.http.HttpHandler;
import com.janilla.http.HttpProtocol;
import com.janilla.net.Net;
import com.janilla.net.Server;
import com.janilla.persistence.ApplicationPersistenceBuilder;
import com.janilla.persistence.Persistence;
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
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			var a = new JanillaIde(pp);
			var hp = a.factory.create(HttpProtocol.class);
			try (var is = Net.class.getResourceAsStream("testkeys")) {
				hp.setSslContext(Net.getSSLContext("JKS", is, "passphrase".toCharArray()));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			hp.setHandler(a.handler);
			var s = new Server();
			s.setAddress(new InetSocketAddress(Integer.parseInt(a.configuration.getProperty("ide.server.port"))));
			s.setProtocol(hp);
			s.serve();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public Properties configuration;

	public Factory factory;

	public HttpHandler handler;

	public Persistence persistence;

	public JanillaIde(Properties configuration) {
		this.configuration = configuration;
		factory = new Factory();
		factory.setTypes(Util.getPackageClasses(getClass().getPackageName()).collect(Collectors.toSet()));
		factory.setSource(this);
		handler = factory.create(ApplicationHandlerBuilder.class).build();
		{
			var pb = factory.create(ApplicationPersistenceBuilder.class);
			var p = configuration.getProperty("ide.database.file");
			if (p.startsWith("~"))
				p = System.getProperty("user.home") + p.substring(1);
			pb.setFile(Path.of(p));
			persistence = pb.build();
		}
	}

	public JanillaIde application() {
		return this;
	}

	private Path directory = Path.of(System.getProperty("user.home")).resolve("gittmp");

	@Handle(method = "GET", path = "/|/edit/(.+)")
	public Index index(Path path) throws IOException {
		return new Index(
				new State(new EntryTree(visit(directory, path), path), path != null ? new FileEditor(path) : null));
	}

	protected Map<String, EntryNode> visit(Path directory, Path path) {
		var ee = EntryApi.list(directory);
		var d = this.directory.relativize(directory);
		var p0 = path != null && path.getNameCount() > 0 ? path.getName(0) : null;
		return ee.stream().map(x -> {
			var p = d.resolve(x.name());
			return new EntryNode(p, x.expandable(),
					x.expandable() && p0 != null && p.endsWith(p0)
							? visit(directory.resolve(x.name()), p0.relativize(path))
							: null);
		}).collect(Collectors.toMap(x -> x.path.getFileName().toString(), x -> x, (x, _) -> x, LinkedHashMap::new));
	}

	@Render(template = "index.html")
	public record Index(State state) {
	}

	@Render(renderer = JsonRenderer.class)
	public record State(EntryTree entryTree, FileEditor fileEditor) {
	}

	public record EntryTree(Map<String, EntryNode> nodes, Path path) {
	}

	public record EntryNode(Path path, boolean expandable, Map<String, EntryNode> children) {
	}

	public record FileEditor(Path path) {
	}
}
