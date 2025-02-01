package com.janilla.ide;

import java.io.BufferedReader;
import java.io.IOException;

public record ThreadTask(Thread thread) implements Task {

	@Override
	public BufferedReader inputReader() {
		return null;
	}

	@Override
	public int waitFor() throws InterruptedException {
		thread.join();
		return 0;
	}

	@Override
	public void destroy() {
		thread.interrupt();
	}

	public record Builder(Runnable runnable) implements Task.Builder {

		@Override
		public Task start() throws IOException {
			return new ThreadTask(Thread.startVirtualThread(runnable));
		}
	}
}
