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
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.janilla.http.HttpResponse;
import com.janilla.http.HttpWritableByteChannel;
import com.janilla.json.Json;
import com.janilla.web.Handle;

public class OutputApi {

	protected static final BlockingQueue<Event> QUEUE = new ArrayBlockingQueue<>(10);

	protected static String format(Event event) {
//		System.out.println("event=" + event);
		var sb = new StringBuilder();
		if (event.type() != null)
			sb.append("event: ").append(event.type()).append("\n");
		sb.append("data: ").append(event.type() != null ? Json.format(event.data(), true) : event.data())
				.append("\n\n");
		return sb.toString();
	}

	@Handle(method = "GET", path = "/api/output")
	public void read(Path path, HttpResponse response) throws IOException, InterruptedException {
		response.setHeaderValue(":status", "200");
		response.setHeaderValue("content-type", "text/event-stream");
		response.setHeaderValue("cache-control", "no-cache");
		var ch = (HttpWritableByteChannel) response.getBody();
		for (;;) {
			var e = QUEUE.poll(5, TimeUnit.SECONDS);
			if (e != null) {
//				System.out.println("OutputApi.read, e=" + e);
				var s = format(e);
//				System.out.println("OutputApi.read, s=" + s);
				ch.write(ByteBuffer.wrap(s.getBytes()));
			} else
				ch.write(ByteBuffer.wrap(format(new Event("ping", Map.of("time", new Date()))).getBytes()));
		}
	}
}
