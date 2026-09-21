/**
 * Copyright 2026 Hamid Gholami
 * SPDX-License-Identifier: Apache-2.0
 * */

@Library('jenkins-platform-library')
import io.github.hamidgholami.jenkins.platform.git.GitCheckoutOptions
import io.github.hamidgholami.jenkins.platform.git.GitCheckoutResult
import io.github.hamidgholami.jenkins.platform.git.GitHelper
import io.github.hamidgholami.jenkins.platform.logging.PipelineLogger

final PipelineLogger logger = log.forContext('Restart')
final GitCheckoutOptions.Builder builder = GitCheckoutOptions.builder('http://git:8081/source.git')
        .branch('main').credentialsId('fixture-git')
final GitCheckoutOptions options = builder.build()
final GitHelper helper = new GitHelper(this, logger)

node('integration') {
    final GitCheckoutResult before = helper.checkout(options)
    logger.info('INTEGRATION_WAITING')
    input(id: 'Resume', message: 'Resume the disposable integration test')
    assert options.revisionType.name() == 'BRANCH'
    assert builder.build().revisionType.name() == 'BRANCH'
    assert logger.minimumLevel.name() == 'INFO'
    logger.info('RESUMED_AFTER_RESTART')
    final GitCheckoutResult after = helper.checkout(options)
    assert before.commit == after.commit
    assert after.commit == '@@SOURCE_COMMIT@@'
    logger.info('RESTART_OK')
}
