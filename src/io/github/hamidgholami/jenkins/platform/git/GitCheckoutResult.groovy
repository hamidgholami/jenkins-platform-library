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

    private GitCheckoutResult(final String commit, final String branch, final String localBranch) {
        this.commit = commit
        this.branch = branch
        this.localBranch = localBranch
    }

    @NonCPS
    static GitCheckoutResult fromCheckout(final Map<String, Object> metadata) {
        final Object commit = metadata == null ? null : metadata.get('GIT_COMMIT')
        if (!(commit instanceof String) || !((String) commit).matches('[0-9a-fA-F]{40}')) {
            throw new IllegalStateException('Git checkout did not return a full commit SHA')
        }
        return new GitCheckoutResult((String) commit, optionalString(metadata, 'GIT_BRANCH'),
                optionalString(metadata, 'GIT_LOCAL_BRANCH'))
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
}
