/**
 * Copyright 2026 Hamid Gholami
 * SPDX-License-Identifier: Apache-2.0
 * */

@Library('jenkins-platform-library')
import io.github.hamidgholami.jenkins.platform.git.GitCheckoutOptions
import io.github.hamidgholami.jenkins.platform.git.GitCheckoutResult
import io.github.hamidgholami.jenkins.platform.logging.LogLevel
import io.github.hamidgholami.jenkins.platform.logging.PipelineLogger

properties([
        parameters([
                string(name: 'REPOSITORY_URL', defaultValue: '', description: 'Repository URL without embedded credentials'),
                string(name: 'BRANCH', defaultValue: 'main', description: 'Short branch name'),
                string(name: 'CREDENTIALS_ID', defaultValue: '', description: 'Optional Jenkins Git credential ID'),
                string(name: 'AGENT_LABEL', defaultValue: 'linux', description: 'Agent with Git installed'),
        ]),
        disableConcurrentBuilds(),
])

final String repositoryUrl = params.REPOSITORY_URL as String
final String branch = params.BRANCH as String
final String credentialsId = params.CREDENTIALS_ID as String
final String agentLabel = params.AGENT_LABEL as String
final PipelineLogger logger = log.forContext('CheckoutExample', LogLevel.INFO, true)

if (repositoryUrl == null || repositoryUrl.trim().isEmpty()) {
    logger.fail('Set REPOSITORY_URL before running this example')
}
final GitCheckoutOptions options = GitCheckoutOptions.builder(repositoryUrl).branch(branch)
        .credentialsId(credentialsId == null || credentialsId.isEmpty() ? null : credentialsId)
        .cloneTimeoutMinutes(30).checkoutTimeoutMinutes(15).build()

timestamps() {
    ansiColor('xterm') {
        node(agentLabel) {
            stage('Checkout source') {
                dir('source') {
                    final GitCheckoutResult result = gitUtils.checkout(options, logger)
                    logger.info('Source ready at ' + result.commit)
                }
            }
        }
    }
}
