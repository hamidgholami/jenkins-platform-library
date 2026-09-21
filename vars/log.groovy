/**
 * Copyright 2026 Hamid Gholami
 * SPDX-License-Identifier: Apache-2.0
 * */

import io.github.hamidgholami.jenkins.platform.logging.LogLevel
import io.github.hamidgholami.jenkins.platform.logging.PipelineLogger

PipelineLogger forContext(final String context, final LogLevel minimumLevel = LogLevel.INFO,
                          final boolean color = false) {
    return new PipelineLogger(this, context, minimumLevel, color)
}
