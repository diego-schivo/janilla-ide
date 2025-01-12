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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import com.janilla.web.Handle;

public class EntryApi {

	private Path directory = Path.of(System.getProperty("user.home")).resolve("gittmp");

	@Handle(method = "GET", path = "/api/entries/(.*)")
	public List<Entry> list(String path) throws IOException {
		System.out.println("EntryApi.list, path=" + path);
		return list(directory.resolve(path));
	}

	public static List<Entry> list(Path directory) {
		try (var pp = Files.list(directory)) {
			return pp.map(x -> {
				var ne = Files.isDirectory(x);
				if (ne)
					try (var pp2 = Files.list(x)) {
						ne = pp2.findAny().isPresent();
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				return new Entry(x.getFileName().toString(), ne);
			}).sorted(Comparator.comparing(Entry::name)).toList();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
