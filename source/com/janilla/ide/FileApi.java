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
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.janilla.http.HttpRequest;
import com.janilla.http.HttpResponse;
import com.janilla.http.HttpWritableByteChannel;
import com.janilla.web.BadRequestException;
import com.janilla.web.Handle;

public class FileApi {

	private Path directory = Path.of(System.getProperty("user.home")).resolve("gittmp");

	@Handle(method = "GET", path = "/api/files/(.*)")
	public void read(String path, HttpResponse response) throws IOException {
		System.out.println("FileApi.read, path=" + path);
		var p = directory.resolve(path);
		if (!Files.isRegularFile(p))
			throw new BadRequestException(path);
		var s = Files.readString(p);
		response.setStatus(200);
		response.setHeaderValue("content-type", "text/plain");
		((HttpWritableByteChannel) response.getBody()).write(StandardCharsets.UTF_8.encode(s), true);
	}

	@Handle(method = "POST", path = "/api/files/(.*)")
	public void write(String path, HttpRequest request, HttpResponse response) throws IOException {
		System.out.println("FileApi.write, path=" + path);
		var p = directory.resolve(path);
		if (!Files.isRegularFile(p))
			throw new BadRequestException(path);
		var s = new String(Channels.newInputStream((ReadableByteChannel) request.getBody()).readAllBytes());
		var n = p.getFileName().toString();
		var i = n.lastIndexOf('.');
		s = switch (n.substring(i + 1).toLowerCase()) {
		case "css" -> new CssIndent().apply(s);
		case "html" -> new HtmlIndent().apply(s);
		case "java" -> new JavaIndent().apply(s);
		case "js" -> new JavascriptIndent().apply(s);
		case "xml" -> new XmlIndent().apply(s);
		default -> s;
		};
		Files.writeString(p, s);
		response.setStatus(200);
		response.setHeaderValue("content-type", "text/plain");
		((HttpWritableByteChannel) response.getBody()).write(StandardCharsets.UTF_8.encode(s), true);
	}
}