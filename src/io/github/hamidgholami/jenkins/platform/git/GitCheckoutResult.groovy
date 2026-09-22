/**
 * Copyright 2026 Hamid Gholami
 * SPDX-License-Identifier: Apache-2.0
 * */

package io.github.hamidgholami.jenkins.platform.git

import com.cloudbees.groovy.cps.NonCPS

final class GitCheckoutResult implements Serializable {

    private static final long serialVersionUID = 1L

    final String commit
    final String branch
    final String localBranch
    final String authorName
    final String authorEmail
    final String committerName
    final String committerEmail

    private GitCheckoutResult(final String commit, final String branch, final String localBranch,
                              final String authorName, final String authorEmail,
                              final String committerName, final String committerEmail) {
        this.commit = commit
        this.branch = branch
        this.localBranch = localBranch
        this.authorName = authorName
        this.authorEmail = authorEmail
        this.committerName = committerName
        this.committerEmail = committerEmail
    }

    @NonCPS
    static GitCheckoutResult fromCheckout(final Map<String, Object> metadata, final String identityOutput) {
        final Object commit = metadata == null ? null : metadata.get('GIT_COMMIT')
        if (!(commit instanceof String) || !((String) commit).matches('[0-9a-fA-F]{40}')) {
            throw new IllegalStateException('Git checkout did not return a full commit SHA')
        }
        final List<String> identities = parseIdentities(identityOutput)
        return new GitCheckoutResult((String) commit, optionalString(metadata, 'GIT_BRANCH'),
                optionalString(metadata, 'GIT_LOCAL_BRANCH'), identities[0], identities[1], identities[2], identities[3])
    }

    @NonCPS
    private static String optionalString(final Map<String, Object> metadata, final String key) {
        final Object value = metadata.get(key)
        if (value == null || value == '') {
            return null
        }
        if (!(value instanceof String)) {
            throw new IllegalStateException('Git checkout returned invalid branch metadata')
        }
        return (String) value
    }

    @NonCPS
    private static List<String> parseIdentities(final String output) {
        if (output == null) {
            throw new IllegalStateException('Git did not return author and committer metadata')
        }
        final List<String> values = output.readLines()
        if (values.size() != 4 || values.any { final String value -> value.isEmpty() }) {
            throw new IllegalStateException('Git did not return complete author and committer metadata')
        }
        return values
    }
}
