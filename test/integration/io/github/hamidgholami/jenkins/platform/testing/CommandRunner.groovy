/**
 * Copyright 2026 Hamid Gholami
 * SPDX-License-Identifier: Apache-2.0
 * */

package io.github.hamidgholami.jenkins.platform.testing

import java.nio.file.Files
import java.util.concurrent.TimeUnit

final class CommandRunner {

    private final File directory

    CommandRunner(final File directory) {
        this.directory = directory
    }

    String execute(final List<String> command, final byte[] input = new byte[0], final int timeoutSeconds = 120) {
        final File output = Files.createTempFile(directory.toPath(), 'command-', '.txt').toFile()
        final Process process = new ProcessBuilder(command).directory(directory)
                .redirectErrorStream(true).redirectOutput(output).start()
        try {
            process.outputStream.write(input)
            process.outputStream.close()
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                throw new IllegalStateException('Command timed out: ' + command[0])
            }
            final String text = output.getText('UTF-8')
            if (process.exitValue() != 0) {
                throw new IllegalStateException('Command failed (' + process.exitValue() + '): ' + command.join(' ') + '\n' + text)
            }
            return text.trim()
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly()
                process.waitFor(10, TimeUnit.SECONDS)
            }
            Files.deleteIfExists(output.toPath())
        }
    }
}
