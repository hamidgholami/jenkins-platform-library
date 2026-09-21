/**
 * Copyright 2026 Hamid Gholami
 * SPDX-License-Identifier: Apache-2.0
 * */

package io.github.hamidgholami.jenkins.platform.git

import com.cloudbees.groovy.cps.NonCPS

final class GitCheckoutOptions implements Serializable {

    private static final long serialVersionUID = 1L

    enum RevisionType { BRANCH, TAG, COMMIT }

    final String repositoryUrl
    final RevisionType revisionType
    final String revision
    final String remoteName
    final String credentialsId
    final String refspec
    final boolean honorRefspec
    final boolean fetchTags
    final int cloneTimeoutMinutes
    final int checkoutTimeoutMinutes
    final Integer shallowDepth
    final String referenceRepository
    final boolean cleanBeforeCheckout
    final boolean pruneStaleBranches
    final boolean poll
    final boolean changelog

    private GitCheckoutOptions(final Builder builder) {
        this.repositoryUrl = builder.repositoryUrl
        this.revisionType = builder.revisionType
        this.revision = builder.revision
        this.remoteName = builder.remoteName
        this.credentialsId = builder.credentialsId
        this.refspec = builder.refspec
        this.honorRefspec = builder.honorRefspec
        this.fetchTags = builder.fetchTags
        this.cloneTimeoutMinutes = builder.cloneTimeoutMinutes
        this.checkoutTimeoutMinutes = builder.checkoutTimeoutMinutes
        this.shallowDepth = builder.shallowDepth
        this.referenceRepository = builder.referenceRepository
        this.cleanBeforeCheckout = builder.cleanBeforeCheckout
        this.pruneStaleBranches = builder.pruneStaleBranches
        this.poll = builder.poll
        this.changelog = builder.changelog
    }

    @NonCPS
    static Builder builder(final String repositoryUrl) {
        return new Builder(repositoryUrl)
    }

    /**
     * Mutable only while configuring; build creates an independent immutable snapshot.
     * */
    static final class Builder implements Serializable {

        private static final long serialVersionUID = 1L

        private final String repositoryUrl
        private RevisionType revisionType
        private String revision
        private String remoteName = 'origin'
        private String credentialsId
        private String refspec
        private boolean honorRefspec = true
        private boolean fetchTags = true
        private int cloneTimeoutMinutes = 30
        private int checkoutTimeoutMinutes = 15
        private Integer shallowDepth
        private String referenceRepository
        private boolean cleanBeforeCheckout
        private boolean pruneStaleBranches
        private boolean poll
        private boolean changelog

        private Builder(final String repositoryUrl) {
            this.repositoryUrl = repositoryUrl
        }

        Builder branch(final String name) {
            this.revisionType = RevisionType.BRANCH
            this.revision = name
            return this
        }

        Builder tag(final String name) {
            this.revisionType = RevisionType.TAG
            this.revision = name
            return this
        }

        Builder commit(final String sha) {
            this.revisionType = RevisionType.COMMIT
            this.revision = sha
            return this
        }

        Builder remoteName(final String name) {
            this.remoteName = name
            return this
        }

        Builder credentialsId(final String id) {
            this.credentialsId = id
            return this
        }

        Builder refspec(final String value) {
            this.refspec = value
            return this
        }

        Builder honorRefspec(final boolean enabled) {
            this.honorRefspec = enabled
            return this
        }

        Builder fetchTags(final boolean enabled) {
            this.fetchTags = enabled
            return this
        }

        Builder cloneTimeoutMinutes(final int minutes) {
            this.cloneTimeoutMinutes = minutes
            return this
        }

        Builder checkoutTimeoutMinutes(final int minutes) {
            this.checkoutTimeoutMinutes = minutes
            return this
        }

        Builder shallowDepth(final int depth) {
            this.shallowDepth = depth
            return this
        }

        Builder referenceRepository(final String path) {
            this.referenceRepository = path
            return this
        }

        Builder cleanBeforeCheckout(final boolean enabled) {
            this.cleanBeforeCheckout = enabled
            return this
        }

        Builder pruneStaleBranches(final boolean enabled) {
            this.pruneStaleBranches = enabled
            return this
        }

        Builder poll(final boolean enabled) {
            this.poll = enabled
            return this
        }

        Builder changelog(final boolean enabled) {
            this.changelog = enabled
            return this
        }

        @NonCPS
        GitCheckoutOptions build() {
            validateUrl(repositoryUrl)
            if (revisionType == null || revision == null) {
                throw new IllegalArgumentException('Select a branch, tag or full commit SHA')
            }
            if (revisionType == RevisionType.COMMIT) {
                if (!revision.matches('[0-9a-fA-F]{40}')) {
                    throw new IllegalArgumentException('Commit must be a full 40-character hexadecimal SHA')
                }
            } else {
                validateRefName(revision)
                if (revision.startsWith('refs/') || (revisionType == RevisionType.BRANCH && revision == 'HEAD')) {
                    throw new IllegalArgumentException('Use a short branch or tag name, without refs/ prefixes')
                }
            }
            if (remoteName == null || !remoteName.matches('[A-Za-z0-9][A-Za-z0-9_-]*')) {
                throw new IllegalArgumentException('Remote name must contain letters, digits, underscores or hyphens')
            }
            if (cloneTimeoutMinutes <= 0 || checkoutTimeoutMinutes <= 0) {
                throw new IllegalArgumentException('Clone and checkout timeouts must be positive minutes')
            }
            if (shallowDepth != null && shallowDepth <= 0) {
                throw new IllegalArgumentException('Shallow depth must be positive; omit it for full history')
            }
            if (revisionType == RevisionType.TAG && !fetchTags) {
                throw new IllegalArgumentException('Tag checkout requires fetchTags to be enabled')
            }
            validateOptionalText(credentialsId, 'Credential ID')
            validateOptionalText(referenceRepository, 'Reference repository')
            validateOptionalText(refspec, 'Refspec')
            if (refspec != null) {
                validateRefspec(refspec)
            }
            return new GitCheckoutOptions(this)
        }

        @NonCPS
        private static void validateOptionalText(final String value, final String label) {
            if (value != null && (value.trim().isEmpty() || value != value.trim() ||
                    value.matches('(?s).*\\p{Cntrl}.*'))) {
                throw new IllegalArgumentException(label + ' must be nonblank and contain no control characters')
            }
        }

        @NonCPS
        private static void validateRefName(final String name) {
            if (name.isEmpty() || name == '@' || name.startsWith('-') || name.endsWith('.') ||
                    name.contains('..') || name.contains('@{') || name.contains('$') ||
                    name.matches('(?s).*[\\p{Cntrl}\\s~^:?*\\[\\\\].*')) {
                throw new IllegalArgumentException('Branch or tag must be a literal Git ref name')
            }
            for (final String segment : name.split('/', -1)) {
                if (segment.isEmpty() || segment.startsWith('.') || segment.endsWith('.lock')) {
                    throw new IllegalArgumentException('Branch or tag contains an invalid Git ref component')
                }
            }
        }

        /**
         * Accept explicit fetch mappings, with at most one matching wildcard on each side.
         * */
        @NonCPS
        private static void validateRefspec(final String value) {
            for (final String mapping : value.split(' +')) {
                final String positive = mapping.startsWith('+') ? mapping.substring(1) : mapping
                final String[] sides = positive.split(':', -1)
                if (sides.length != 2 || !sides[0].startsWith('refs/') || !sides[1].startsWith('refs/') ||
                        sides[0].count('*') > 1 || sides[0].count('*') != sides[1].count('*')) {
                    throw new IllegalArgumentException('Refspec must contain space-separated refs/source:refs/destination mappings')
                }
                for (final String side : sides) {
                    validateRefName(side.replace('*', 'wildcard'))
                }
            }
        }

        /**
         * Never retain a parser or include its exception: URL diagnostics can expose credentials.
         * */
        @NonCPS
        private static void validateUrl(final String value) {
            if (value == null || value.isEmpty() || value.matches('(?s).*[\\p{Cntrl}\\s].*')) {
                throw new IllegalArgumentException('Repository URL must be a nonblank network URL')
            }
            if (!value.contains('://')) {
                if (!value.matches('(?:[A-Za-z0-9._-]+@)?(?:[A-Za-z0-9][A-Za-z0-9.-]*|\\[[0-9a-fA-F:]+\\]):[^\\s:?#]+')) {
                    throw new IllegalArgumentException('Use an HTTP(S), SSH or Git URL; local paths are not supported')
                }
                return
            }
            final URI uri
            try {
                uri = new URI(value)
            } catch (final URISyntaxException ignored) {
                throw new IllegalArgumentException('Repository URL is malformed')
            }
            final String scheme = uri.scheme == null ? '' : uri.scheme.toLowerCase(Locale.ROOT)
            if (!(scheme in ['http', 'https', 'ssh', 'git']) || uri.host == null ||
                    uri.path == null || uri.path.length() < 2 || uri.rawQuery != null || uri.rawFragment != null) {
                throw new IllegalArgumentException('Use a network Git URL with a host and path, without query or fragment')
            }
            if (uri.userInfo != null && (scheme != 'ssh' || uri.userInfo.contains(':'))) {
                throw new IllegalArgumentException('Use a Jenkins credential ID instead of embedded URL credentials')
            }
        }
    }
}
