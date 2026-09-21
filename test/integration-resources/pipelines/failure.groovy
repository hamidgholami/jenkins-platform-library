/**
 * Copyright 2026 Hamid Gholami
 * SPDX-License-Identifier: Apache-2.0
 * */

@Library('jenkins-platform-library')
import io.github.hamidgholami.jenkins.platform.logging.PipelineLogger

final PipelineLogger logger = log.forContext('Failure')
logger.fail('EXPECTED_NATIVE_FAILURE')
