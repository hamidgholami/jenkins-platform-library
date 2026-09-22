/**
 * Copyright 2026 Hamid Gholami
 * SPDX-License-Identifier: Apache-2.0
 * */

package io.github.hamidgholami.jenkins.platform.git

import com.cloudbees.groovy.cps.NonCPS
import io.github.hamidgholami.jenkins.platform.logging.PipelineLogger

final class GitHelper implements Serializable {

    private static final long serialVersionUID = 1L

    private final Script script
    private final PipelineLogger logger

    GitHelper(final Script script, final PipelineLogger logger) {
        if (script == null || logger == null) {
            throw new IllegalArgumentException('A Pipeline script and logger are required')
        }
        this.script = script
        this.logger = logger
    }

    GitCheckoutResult checkout(final GitCheckoutOptions options) {
        if (options == null) {
            throw new IllegalArgumentException('Git checkout options are required')
        }
        logger.info('Checking out repository')
        logger.debug('Clone/fetch timeout: ' + options.cloneTimeoutMinutes +
                ' min; checkout timeout: ' + options.checkoutTimeoutMinutes + ' min')
        script.checkout(
                scm: script.scmGit(configuration(options)), poll: options.poll, changelog: options.changelog)
        /**
         * Repeated checkouts of one URL can return stale plugin build metadata.
         * Read the actual agent workspace; the command contains no user input.
         * */
        final String commit = script.isUnix()
                ? script.sh(script: 'git rev-parse --verify HEAD', returnStdout: true).trim()
                : script.bat(script: '@git rev-parse --verify HEAD', returnStdout: true).trim()
        final String identityOutput = script.isUnix()
                ? script.sh(script: 'git show -s --format=%aN%n%aE%n%cN%n%cE HEAD', returnStdout: true)
                : script.bat(script: '@git show -s --format=%aN%n%aE%n%cN%n%cE HEAD', returnStdout: true)
        final String branch = options.revisionType == GitCheckoutOptions.RevisionType.BRANCH
                ? options.remoteName + '/' + options.revision : null
        final GitCheckoutResult result = GitCheckoutResult.fromCheckout(
                [GIT_COMMIT: commit, GIT_BRANCH: branch], identityOutput)
        if (options.revisionType == GitCheckoutOptions.RevisionType.COMMIT &&
                !options.revision.equalsIgnoreCase(result.commit)) {
            throw new IllegalStateException('Git checkout returned a different commit than requested')
        }
        logger.info('Checked out commit ' + result.commit)
        return result
    }

    /**
     * Translate typed options only at the Jenkins boundary. No plugin objects are retained.
     * */
    @NonCPS
    private static Map<String, Object> configuration(final GitCheckoutOptions options) {
        final Map<String, Object> remote = [
                url: options.repositoryUrl,
                name: options.remoteName,
                refspec: options.refspec == null ? '+refs/heads/*:refs/remotes/' + options.remoteName + '/*' : options.refspec,
        ]
        if (options.credentialsId != null) {
            remote.put('credentialsId', options.credentialsId)
        }
        final Map<String, Object> clone = [
                $class: 'CloneOption',
                shallow: options.shallowDepth != null,
                noTags: !options.fetchTags,
                reference: options.referenceRepository == null ? '' : options.referenceRepository,
                timeout: options.cloneTimeoutMinutes,
                honorRefspec: options.honorRefspec,
        ]
        if (options.shallowDepth != null) {
            clone.put('depth', options.shallowDepth)
        }
        final List<Map<String, Object>> extensions = [
                clone,
                [$class: 'CheckoutOption', timeout: options.checkoutTimeoutMinutes],
        ]
        if (options.cleanBeforeCheckout) {
            extensions.add([$class: 'CleanBeforeCheckout', deleteUntrackedNestedRepositories: false])
        }
        if (options.pruneStaleBranches) {
            extensions.add([$class: 'PruneStaleBranch'])
        }
        final String selector
        switch (options.revisionType) {
            case GitCheckoutOptions.RevisionType.BRANCH:
                selector = 'refs/remotes/' + options.remoteName + '/' + options.revision
                break
            case GitCheckoutOptions.RevisionType.TAG:
                selector = 'refs/tags/' + options.revision
                break
            default:
                selector = options.revision
        }
        return [branches: [[name: selector]], userRemoteConfigs: [remote], extensions: extensions]
    }
}
