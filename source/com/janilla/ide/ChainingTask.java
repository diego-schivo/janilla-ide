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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class ChainingTask implements Task {

	protected static final Supplier<BufferedReader> END_READER = () -> new BufferedReader(Reader.nullReader());

	protected final Lock lock = new ReentrantLock();

	protected final Condition terminated = lock.newCondition();

	protected final BlockingQueue<Supplier<BufferedReader>> readers;

	protected Task task;

	protected Integer exitValue;

	public ChainingTask(BlockingQueue<Supplier<BufferedReader>> readers) {
		this.readers = readers;
	}

	@Override
	public BufferedReader inputReader() {
		return new BufferedReader(new Reader() {

			private BufferedReader reader;

			private boolean isEnded;

			private boolean isClosed;

			@Override
			public int read(char[] cbuf, int off, int len) throws IOException {
				if (isClosed)
					throw new IOException("Stream closed");
				if (isEnded)
					return -1;
				int x;
				while ((x = reader != null ? reader.read(cbuf, off, len) : -1) == -1)
					try {
						var r = readers.take();
						if (isEnded = r == END_READER)
							break;
						reader = r.get();
					} catch (Exception e) {
						throw new IOException(e);
					}
				return x;
			}

			@Override
			public void close() {
				isClosed = true;
			}
		});
	}

	@Override
	public int waitFor() throws InterruptedException {
		if (exitValue != null)
			return exitValue;
		lock.lock();
		try {
			while (exitValue == null)
				terminated.await();
			return exitValue;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void destroy() {
		task.destroy();
	}

	public static class Builder implements Task.Builder {

		protected final Task.Builder[] builders;

		public Builder(Task.Builder... builders) {
			this.builders = builders;
		}

		@Override
		public Task start() {
			var ct = new ChainingTask(new ArrayBlockingQueue<>(builders.length));
			Thread.startVirtualThread(() -> {
				ct.lock.lock();
				try {
					var ev = 0;
					try {
						for (var i = 0; i < builders.length; i++) {
							var t = builders[i].start();
							ct.task = t;
							ct.readers.put(() -> t.inputReader());
							ev = t.waitFor();
							if (ev != 0)
								break;
						}
						ct.task = null;
						ct.readers.put(END_READER);
					} catch (InterruptedException e) {
						ev = -1;
					}
					ct.exitValue = ev;
					ct.terminated.signalAll();
				} finally {
					ct.lock.unlock();
				}
			});
			return ct;
		}
	}
}
