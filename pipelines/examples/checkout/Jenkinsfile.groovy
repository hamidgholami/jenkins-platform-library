/**
 * Copyright 2026 Hamid Gholami
 * SPDX-License-Identifier: Apache-2.0
 * */

import io.github.hamidgholami.jenkins.platform.git.GitCheckoutOptions
import io.github.hamidgholami.jenkins.platform.git.GitCheckoutResult
import io.github.hamidgholami.jenkins.platform.logging.LogLevel
import io.github.hamidgholami.jenkins.platform.logging.PipelineLogger
import org.jenkinsci.plugins.workflow.libs.Library

@Library('jenkins-platform-library') _

properties([
        parameters([
                string(name: 'REPOSITORY_URL', defaultValue: '', description: 'Repository URL without embedded credentials'),
                string(name: 'BRANCH', defaultValue: 'main', description: 'Short branch name'),
                string(name: 'CREDENTIALS_ID', defaultValue: '', description: 'Optional Jenkins Git credential ID'),
                string(name: 'AGENT_LABEL', defaultValue: 'linux', description: 'Agent with Git installed'),
                booleanParam(name: 'REFRESH_JOB_PROPERTIES', defaultValue: false,
                        description: 'Apply the current Jenkinsfile properties and exit successfully'),
        ]),
        disableConcurrentBuilds(),
])

if (env.BUILD_NUMBER == '1' || params.get('REFRESH_JOB_PROPERTIES', false) == true) {
    echo(env.BUILD_NUMBER == '1'
            ? 'Job properties initialized; run the pipeline again with parameters'
            : 'Job properties refreshed')
    currentBuild.result = 'SUCCESS'
    return
}

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
