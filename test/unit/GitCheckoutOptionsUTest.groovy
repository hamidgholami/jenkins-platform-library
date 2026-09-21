import io.github.hamidgholami.jenkins.platform.git.GitCheckoutOptions
import io.github.hamidgholami.jenkins.platform.git.GitCheckoutResult
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertNull
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

class GitCheckoutOptionsUTest {

    private static final String URL = 'https://git.example.org/team/service.git'
    private static final String COMMIT = '0123456789abcdef0123456789abcdef01234567'

    @Test
    void providesSafeCheckoutDefaults() {
        final GitCheckoutOptions options = GitCheckoutOptions.builder(URL).branch('main').build()

        assertEquals(GitCheckoutOptions.RevisionType.BRANCH, options.revisionType)
        assertEquals('main', options.revision)
        assertEquals('origin', options.remoteName)
        assertTrue(options.fetchTags)
        assertTrue(options.honorRefspec)
        assertEquals(30, options.cloneTimeoutMinutes)
        assertEquals(15, options.checkoutTimeoutMinutes)
        assertNull(options.shallowDepth)
        assertFalse(options.cleanBeforeCheckout)
        assertFalse(options.pruneStaleBranches)
        assertFalse(options.poll)
        assertFalse(options.changelog)
    }

    @Test
    void acceptsTagCommitAndCheckoutControls() {
        final GitCheckoutOptions tag = GitCheckoutOptions.builder(URL).tag('v1.0').build()
        final GitCheckoutOptions commit = GitCheckoutOptions.builder(URL).commit(COMMIT)
                .credentialsId('source-reader').shallowDepth(10)
                .cloneTimeoutMinutes(60).checkoutTimeoutMinutes(20)
                .cleanBeforeCheckout(true).pruneStaleBranches(true).build()

        assertEquals(GitCheckoutOptions.RevisionType.TAG, tag.revisionType)
        assertEquals(GitCheckoutOptions.RevisionType.COMMIT, commit.revisionType)
        assertEquals('source-reader', commit.credentialsId)
        assertEquals(10, commit.shallowDepth)
        assertTrue(commit.cleanBeforeCheckout)
        assertTrue(commit.pruneStaleBranches)
    }

    @Test
    void rejectsMissingOrMalformedRevisions() {
        assertThrows(IllegalArgumentException) { -> GitCheckoutOptions.builder(URL).build() }
        assertThrows(IllegalArgumentException) { -> GitCheckoutOptions.builder(URL).branch('refs/heads/main').build() }
        assertThrows(IllegalArgumentException) { -> GitCheckoutOptions.builder(URL).tag('bad tag').build() }
        assertThrows(IllegalArgumentException) { -> GitCheckoutOptions.builder(URL).commit('123abc').build() }
    }

    @Test
    void rejectsUnsafeRepositoryUrlsAndInvalidTuning() {
        assertThrows(IllegalArgumentException) { ->
            GitCheckoutOptions.builder('https://user:secret@git.example.org/repo.git').branch('main').build()
        }
        assertThrows(IllegalArgumentException) { -> GitCheckoutOptions.builder('/local/repo').branch('main').build() }
        assertThrows(IllegalArgumentException) { -> GitCheckoutOptions.builder(URL).branch('main').shallowDepth(0).build() }
        assertThrows(IllegalArgumentException) { ->
            GitCheckoutOptions.builder(URL).branch('main').cloneTimeoutMinutes(0).build()
        }
    }

    @Test
    void mapsCheckoutMetadataIntoAnImmutableResult() {
        final Map<String, Object> metadata = [
                GIT_COMMIT: COMMIT,
                GIT_BRANCH: 'origin/main',
                GIT_LOCAL_BRANCH: 'main',
        ]

        final GitCheckoutResult result = GitCheckoutResult.fromCheckout(metadata)
        metadata.clear()

        assertEquals(COMMIT, result.commit)
        assertEquals('origin/main', result.branch)
        assertEquals('main', result.localBranch)
    }

    @Test
    void requiresAFullCommitInCheckoutMetadata() {
        assertThrows(IllegalStateException) { -> GitCheckoutResult.fromCheckout(null) }
        assertThrows(IllegalStateException) { -> GitCheckoutResult.fromCheckout([GIT_COMMIT: '123abc']) }
    }
}
