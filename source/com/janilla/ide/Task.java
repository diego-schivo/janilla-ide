package com.janilla.ide;

import java.io.BufferedReader;
import java.io.IOException;

public interface Task {

	BufferedReader inputReader();

	int waitFor() throws InterruptedException;

	void destroy();

	interface Builder {

		Task start() throws IOException;
	}
}
