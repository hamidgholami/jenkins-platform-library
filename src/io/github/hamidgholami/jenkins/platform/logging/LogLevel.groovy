/**
 * Copyright 2026 Hamid Gholami
 * SPDX-License-Identifier: Apache-2.0
 * */

package io.github.hamidgholami.jenkins.platform.logging

enum LogLevel {
    TRACE(90),
    DEBUG(36),
    INFO(32),
    WARN(33),
    ERROR(31)

    final int ansiCode

    private LogLevel(final int ansiCode) {
        this.ansiCode = ansiCode
    }
}
