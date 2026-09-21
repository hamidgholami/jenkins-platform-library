/**
 * Copyright 2026 Hamid Gholami
 * SPDX-License-Identifier: Apache-2.0
 * */

@Library('jenkins-platform-library')
import io.github.hamidgholami.jenkins.platform.logging.PipelineLogger

final PipelineLogger logger = log.forContext('HandledFailure')
catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
    logger.fail('EXPECTED_HANDLED_FAILURE')
}
assert currentBuild.currentResult == 'UNSTABLE'
logger.info('CONTINUED_AFTER_HANDLED_FAILURE')
