/**
 * Copyright 2026 Hamid Gholami
 * SPDX-License-Identifier: Apache-2.0
 * */

@Library('jenkins-platform-library')
import io.github.hamidgholami.jenkins.platform.git.GitCheckoutOptions
import io.github.hamidgholami.jenkins.platform.git.GitCheckoutResult
import io.github.hamidgholami.jenkins.platform.logging.LogLevel
import io.github.hamidgholami.jenkins.platform.logging.PipelineLogger

final String repository = 'http://git:8081/source.git'
final PipelineLogger logger = log.forContext('Integration', LogLevel.DEBUG, true)

node('integration') {
    timestamps() {
        ansiColor('xterm') {
            final String workspace = pwd()
            stage('Full history and workspace reuse') {
                dir('full') {
                    final GitCheckoutOptions options = GitCheckoutOptions.builder(repository)
                            .branch('main').credentialsId('fixture-git').build()
                    final GitCheckoutResult result = gitUtils.checkout(options, logger)
                    assert result.commit == '@@SOURCE_COMMIT@@'
                    assert sh(script: 'git rev-list --count HEAD', returnStdout: true).trim() == '5'
                    assert sh(script: 'git rev-parse --is-shallow-repository', returnStdout: true).trim() == 'false'
                    sh('git show-ref --verify refs/tags/v1')
                    writeFile(file: 'sentinel.txt', text: 'must survive workspace reuse')
                    gitUtils.checkout(options, logger)
                    assert fileExists('sentinel.txt')
                    sh('git update-ref refs/remotes/origin/obsolete HEAD')
                    gitUtils.checkout(GitCheckoutOptions.builder(repository).branch('main')
                            .credentialsId('fixture-git').cleanBeforeCheckout(true).pruneStaleBranches(true).build(), logger)
                    assert !fileExists('sentinel.txt')
                    assert sh(script: 'git show-ref --verify refs/remotes/origin/obsolete', returnStatus: true) != 0
                }
            }
            stage('Shallow and narrow fetch') {
                dir('shallow') {
                    final GitCheckoutResult result = gitUtils.checkout(GitCheckoutOptions.builder(repository)
                            .branch('main').credentialsId('fixture-git').remoteName('upstream')
                            .refspec('+refs/heads/main:refs/remotes/upstream/main')
                            .fetchTags(false).shallowDepth(2).cloneTimeoutMinutes(5).checkoutTimeoutMinutes(3).build(), logger)
                    assert result.commit == '@@SOURCE_COMMIT@@'
                    assert sh(script: 'git rev-list --count HEAD', returnStdout: true).trim() == '2'
                    assert sh(script: 'git rev-parse --is-shallow-repository', returnStdout: true).trim() == 'true'
                    assert sh(script: 'git tag', returnStdout: true).trim().isEmpty()
                    assert sh(script: 'git for-each-ref --format="%(refname)" refs/remotes', returnStdout: true)
                            .trim() == 'refs/remotes/upstream/main'
                }
            }
            stage('Tags, commits and branch names') {
                dir('tag') {
                    final GitCheckoutResult result = gitUtils.checkout(GitCheckoutOptions.builder(repository)
                            .tag('v1').credentialsId('fixture-git').build(), logger)
                    assert result.commit == '@@FIRST_COMMIT@@'
                    assert readFile('version.txt').trim() == 'version-1'
                }
                dir('commit') {
                    final GitCheckoutResult result = gitUtils.checkout(GitCheckoutOptions.builder(repository)
                            .commit('@@FIRST_COMMIT@@').credentialsId('fixture-git').build(), logger)
                    assert result.commit == '@@FIRST_COMMIT@@'
                }
                dir('branch') {
                    final GitCheckoutResult result = gitUtils.checkout(GitCheckoutOptions.builder(repository).branch('feature/example')
                            .credentialsId('fixture-git').build(), logger)
                    assert result.commit == '@@SOURCE_COMMIT@@'
                    assert result.branch == 'origin/feature/example'
                    assert readFile('version.txt').trim() == 'version-5'
                }
            }
            stage('Reference repository') {
                dir('reference') {
                    final GitCheckoutResult result = gitUtils.checkout(GitCheckoutOptions.builder(repository).branch('main')
                            .credentialsId('fixture-git').referenceRepository(workspace + '/full/.git').build(), logger)
                    assert result.commit == '@@SOURCE_COMMIT@@'
                    assert fileExists('.git/objects/info/alternates')
                }
            }
            logger.debug('DEBUG_VISIBLE')
            logger.trace('TRACE_MUST_BE_HIDDEN')
            logger.info('MULTILINE_FIRST\nMULTILINE_SECOND')
            logger.error('LOG_ONLY_ERROR')
            assert currentBuild.currentResult == 'SUCCESS'
            logger.info('CHECKOUT_AND_LOGGING_OK')
        }
    }
}
