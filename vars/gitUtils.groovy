/**
 * Copyright 2026 Hamid Gholami
 * SPDX-License-Identifier: Apache-2.0
 * */

import io.github.hamidgholami.jenkins.platform.git.GitCheckoutOptions
import io.github.hamidgholami.jenkins.platform.git.GitCheckoutResult
import io.github.hamidgholami.jenkins.platform.git.GitHelper
import io.github.hamidgholami.jenkins.platform.logging.PipelineLogger

GitCheckoutResult checkout(final GitCheckoutOptions options, final PipelineLogger logger = null) {
    final PipelineLogger checkoutLogger = logger == null ? new PipelineLogger(this, 'GitHelper.checkout') : logger
    return new GitHelper(this, checkoutLogger).checkout(options)
}
