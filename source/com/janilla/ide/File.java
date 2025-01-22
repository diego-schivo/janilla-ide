package com.janilla.ide;

import java.nio.file.Path;

public record File(Path path, String content) {
}
