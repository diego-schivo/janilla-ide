package com.janilla.ide;

import java.io.BufferedReader;
import java.io.IOException;

public record ProcessTask(Process process) implements Task {

	@Override
	public BufferedReader inputReader() {
		return process.inputReader();
	}

	@Override
	public int waitFor() throws InterruptedException {
		return process.waitFor();
	}

	@Override
	public void destroy() {
		process.destroy();
	}

	public record Builder(ProcessBuilder builder) implements Task.Builder {

		@Override
		public Task start() throws IOException {
			return new ProcessTask(builder.start());
		}
	}
}
