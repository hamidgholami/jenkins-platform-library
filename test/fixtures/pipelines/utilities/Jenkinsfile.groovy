/**
 * Copyright 2026 Hamid Gholami
 * SPDX-License-Identifier: Apache-2.0
 * */

@Library('jenkins-platform-library')
import io.github.hamidgholami.jenkins.platform.git.GitCheckoutOptions
import io.github.hamidgholami.jenkins.platform.git.GitCheckoutResult
import io.github.hamidgholami.jenkins.platform.logging.LogLevel
import io.github.hamidgholami.jenkins.platform.logging.PipelineLogger

final GitCheckoutOptions options = GitCheckoutOptions.builder('https://git.example.org/team/service.git')
        .branch('main').cloneTimeoutMinutes(45).build()
final PipelineLogger logger = log.forContext('Source', LogLevel.DEBUG, true)

node('test-agent') {
    timestamps() {
        ansiColor('xterm') {
            dir('source') {
                final GitCheckoutResult result = gitUtils.checkout(options, logger)
                logger.info('Ready\nCommit ' + result.commit)
            }
        }
    }
}
