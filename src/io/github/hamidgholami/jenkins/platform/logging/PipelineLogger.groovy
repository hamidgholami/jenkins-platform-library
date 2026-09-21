/**
 * Copyright 2026 Hamid Gholami
 * SPDX-License-Identifier: Apache-2.0
 * */

package io.github.hamidgholami.jenkins.platform.logging

import com.cloudbees.groovy.cps.NonCPS

final class PipelineLogger implements Serializable {

    private static final long serialVersionUID = 1L

    private final Script script
    final String context
    final LogLevel minimumLevel
    final boolean color

    PipelineLogger(final Script script, final String context,
                   final LogLevel minimumLevel = LogLevel.INFO, final boolean color = false) {
        if (script == null || minimumLevel == null) {
            throw new IllegalArgumentException('A Pipeline script and minimum log level are required')
        }
        if (context == null || context.trim().isEmpty() || context != context.trim() ||
                context.matches('(?s).*[\\p{Cntrl}\\[\\]].*')) {
            throw new IllegalArgumentException('Log context must be nonblank and contain no controls or brackets')
        }
        this.script = script
        this.context = context
        this.minimumLevel = minimumLevel
        this.color = color
    }

    void trace(final String message) {
        write(LogLevel.TRACE, message)
    }

    void debug(final String message) {
        write(LogLevel.DEBUG, message)
    }

    void info(final String message) {
        write(LogLevel.INFO, message)
    }

    void warn(final String message) {
        write(LogLevel.WARN, message)
    }

    void error(final String message) {
        write(LogLevel.ERROR, message)
    }

    /**
     * Keep Jenkins failure handling intact: callers may catch the native exception.
     * */
    void fail(final String message) {
        error(message)
        script.error(message)
    }

    private void write(final LogLevel level, final String message) {
        if (message == null) {
            throw new IllegalArgumentException('Log message must not be null')
        }
        if (level.ordinal() >= minimumLevel.ordinal()) {
            script.echo(format(level, message))
        }
    }

    /**
     * Pure formatting finishes before echo; no iterator survives a Pipeline step.
     * */
    @NonCPS
    private String format(final LogLevel level, final String message) {
        final String label = '[' + level.name() + ']'
        final String severity = color ? '\u001B[' + level.ansiCode + 'm' + label + '\u001B[0m' : label
        final String prefix = severity + ' [' + context + '] '
        final String[] lines = message.replace('\r\n', '\n').replace('\r', '\n').split('\n', -1)
        final List<String> formatted = []
        for (final String line : lines) {
            formatted.add(prefix + line)
        }
        return formatted.join('\n')
    }
}
