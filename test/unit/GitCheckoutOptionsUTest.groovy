/**
 * Copyright 2026 Hamid Gholami
 * SPDX-License-Identifier: Apache-2.0
 * */

import io.github.hamidgholami.jenkins.platform.git.GitCheckoutOptions
import io.github.hamidgholami.jenkins.platform.git.GitCheckoutResult
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertNull
import static org.junit.jupiter.api.Assertions.assertThrows

class GitCheckoutOptionsUTest {

    private static final String URL = 'https://git.example.org/team/service.git'
    private static final String COMMIT = '0123456789abcdef0123456789abcdef01234567'

    @ParameterizedTest
    @ValueSource(strings = ['https://git.example.org/team/service.git', 'http://git.example.org/team/service.git',
            'ssh://git@git.example.org:2222/team/service.git', 'git@git.example.org:team/service.git',
            'git://git.example.org/team/service.git', 'ssh://git@[::1]/team/service.git',
            'https://git.example.org:8443/team/my%20service.git'])
    void acceptsNetworkRepositoryForms(final String url) {
        assertEquals(url, GitCheckoutOptions.builder(url).branch('main').build().repositoryUrl)
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = [' ', '/agent/repo', 'file:///repo', 'ext::command',
            'https://git.example.org', 'https://git.example.org/', 'https://git.example.org/repo?token=hidden',
            'https://git.example.org/repo#hidden', 'https://user:hidden@git.example.org/repo',
            'https://hidden@git.example.org/repo', 'ssh://git:hidden@git.example.org/repo',
            'https://git.example.org/repo\n', 'https://git.example.org/%not-valid', 'https:///repo',
            'https://git.example.org:bad/repo', 'ssh://git@hidden@git.example.org/repo',
            'https://git.example.org/<repo>', 'https://git.example.org/repo\\hidden'])
    void rejectsInvalidOrCredentialBearingUrlsWithoutEchoingThem(final String url) {
        final IllegalArgumentException thrown = assertThrows(IllegalArgumentException) { ->
            GitCheckoutOptions.builder(url).branch('main').build()
        }
        assertFalse(thrown.message.contains('hidden'))
        assertNull(thrown.cause)
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = [' ', 'refs/heads/main', 'HEAD', '-bad', 'bad..name', 'bad@{name', '@',
            'bad.lock', 'bad.lock/child', '.hidden', 'parent/.hidden', '/main', 'main/', 'a//b', 'main.',
            'bad name', 'bad\nname', 'wild*', 'bad?', 'bad[', 'bad\\name', 'bad^', 'bad~', 'bad:name', '${BRANCH}'])
    void rejectsAmbiguousOrInvalidBranchSelectors(final String name) {
        assertThrows(IllegalArgumentException) { -> GitCheckoutOptions.builder(URL).branch(name).build() }
    }

    @Test
    void requiresExplicitRevisionAndValidTagAndCommit() {
        assertThrows(IllegalArgumentException) { -> GitCheckoutOptions.builder(URL).build() }
        assertThrows(IllegalArgumentException) { -> GitCheckoutOptions.builder(URL).tag('refs/tags/v1').build() }
        assertThrows(IllegalArgumentException) { -> GitCheckoutOptions.builder(URL).tag('v1').fetchTags(false).build() }
        assertThrows(IllegalArgumentException) { -> GitCheckoutOptions.builder(URL).commit('123abc').build() }
        assertThrows(IllegalArgumentException) { -> GitCheckoutOptions.builder(URL).commit('z' * 40).build() }
    }

    @Test
    void rejectsInvalidTuningValues() {
        assertThrows(IllegalArgumentException) { -> builder().cloneTimeoutMinutes(0).build() }
        assertThrows(IllegalArgumentException) { -> builder().checkoutTimeoutMinutes(-1).build() }
        assertThrows(IllegalArgumentException) { -> builder().shallowDepth(0).build() }
        assertThrows(IllegalArgumentException) { -> builder().shallowDepth(-5).build() }
        assertThrows(IllegalArgumentException) { -> builder().remoteName('bad/name').build() }
        assertThrows(IllegalArgumentException) { -> builder().remoteName(null).build() }
        assertThrows(IllegalArgumentException) { -> builder().credentialsId(' ').build() }
        assertThrows(IllegalArgumentException) { -> builder().referenceRepository('bad\npath').build() }
        assertThrows(IllegalArgumentException) { -> builder().refspec('').build() }
        assertThrows(IllegalArgumentException) { -> builder().refspec('${REFSPEC}').build() }
    }

    @Test
    void builtOptionsAreIndependentOfLaterBuilderChanges() {
        final GitCheckoutOptions.Builder builder = builder()
        final GitCheckoutOptions first = builder.build()
        final GitCheckoutOptions second = builder.tag('v1').remoteName('upstream').shallowDepth(1).build()
        assertEquals(GitCheckoutOptions.RevisionType.BRANCH, first.revisionType)
        assertEquals('main', first.revision)
        assertEquals('origin', first.remoteName)
        assertNull(first.shallowDepth)
        assertEquals(GitCheckoutOptions.RevisionType.TAG, second.revisionType)
        assertThrows(ReadOnlyPropertyException) { -> first.revision = 'changed' }
    }

    @ParameterizedTest
    @ValueSource(strings = ['not-a-refspec', ':', '+refs/heads/*:refs/remotes/origin/main',
            '+refs/heads/**:refs/remotes/origin/**', '+refs/heads/main:',
            '+refs/heads/main:refs/remotes/origin/../main', '^refs/heads/main'])
    void rejectsMalformedFetchMappings(final String refspec) {
        assertThrows(IllegalArgumentException) { -> builder().refspec(refspec).build() }
    }

    @Test
    void acceptsMultipleExplicitFetchMappings() {
        final String refspec = '+refs/heads/main:refs/remotes/origin/main +refs/tags/*:refs/tags/*'
        assertEquals(refspec, builder().refspec(refspec).build().refspec)
    }

    @Test
    void resultMapsOnlySupportedMetadataAndDoesNotRetainInputMap() {
        final Map<String, Object> metadata = [GIT_COMMIT: COMMIT, GIT_BRANCH: 'origin/main',
                                             GIT_LOCAL_BRANCH: 'main', GIT_URL: URL, OTHER: new Object()]
        final GitCheckoutResult result = GitCheckoutResult.fromCheckout(metadata)
        metadata.clear()
        assertEquals(COMMIT, result.commit)
        assertEquals('origin/main', result.branch)
        assertEquals('main', result.localBranch)
        assertThrows(ReadOnlyPropertyException) { -> result.commit = 'changed' }
    }

    @Test
    void resultRequiresCommitButDoesNotInventBranchMetadata() {
        final GitCheckoutResult result = GitCheckoutResult.fromCheckout([GIT_COMMIT: COMMIT, GIT_BRANCH: ''])
        assertNull(result.branch)
        assertNull(result.localBranch)
        assertThrows(IllegalStateException) { -> GitCheckoutResult.fromCheckout(null) }
        assertThrows(IllegalStateException) { -> GitCheckoutResult.fromCheckout([GIT_COMMIT: '123abc']) }
        assertThrows(IllegalStateException) { -> GitCheckoutResult.fromCheckout([GIT_COMMIT: new Object()]) }
        assertThrows(IllegalStateException) { -> GitCheckoutResult.fromCheckout([GIT_COMMIT: COMMIT, GIT_BRANCH: [:]]) }
    }

    @Test
    void optionsAndResultsCanBeSerializedWithoutRuntimePluginObjects() {
        final GitCheckoutOptions options = (GitCheckoutOptions) roundTrip(builder().shallowDepth(7).build())
        final GitCheckoutResult result = (GitCheckoutResult) roundTrip(GitCheckoutResult.fromCheckout([GIT_COMMIT: COMMIT]))
        assertEquals(URL, options.repositoryUrl)
        assertEquals(7, options.shallowDepth)
        assertEquals(COMMIT, result.commit)
    }

    private static GitCheckoutOptions.Builder builder() {
        return GitCheckoutOptions.builder(URL).branch('main')
    }

    private static Serializable roundTrip(final Serializable value) {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream()
        final ObjectOutputStream output = new ObjectOutputStream(bytes)
        try {
            output.writeObject(value)
        } finally {
            output.close()
        }
        final ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))
        try {
            return (Serializable) input.readObject()
        } finally {
            input.close()
        }
    }
}
