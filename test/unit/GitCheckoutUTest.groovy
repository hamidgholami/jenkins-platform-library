import com.lesfurets.jenkins.unit.BasePipelineTest
import io.github.hamidgholami.jenkins.platform.git.GitCheckoutOptions
import io.github.hamidgholami.jenkins.platform.git.GitCheckoutResult
import io.github.hamidgholami.jenkins.platform.logging.LogLevel
import io.github.hamidgholami.jenkins.platform.logging.PipelineLogger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertSame
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

class GitCheckoutUTest extends BasePipelineTest {

    private static final String URL = 'https://git.example.org/team/service.git'
    private static final String COMMIT = '0123456789abcdef0123456789abcdef01234567'

    private Script checkoutScript
    private Script logScript
    private final List<Map<String, Object>> scmCalls = []
    private final List<Map<String, Object>> checkoutCalls = []
    private final List<String> messages = []
    private RuntimeException checkoutFailure
    private boolean unix

    @Override
    @BeforeEach
    void setUp() {
        scriptRoots = ['.']
        super.setUp()
        scmCalls.clear()
        checkoutCalls.clear()
        messages.clear()
        checkoutFailure = null
        unix = true
        helper.registerAllowedMethod('echo', [String], { final String message -> messages.add(message) })
        helper.registerAllowedMethod('scmGit', [Map], { final Map<String, Object> arguments ->
            scmCalls.add(arguments)
            return arguments
        })
        helper.registerAllowedMethod('checkout', [Map], { final Map<String, Object> arguments ->
            checkoutCalls.add(arguments)
            if (checkoutFailure != null) {
                throw checkoutFailure
            }
            return [GIT_COMMIT: COMMIT]
        })
        helper.registerAllowedMethod('isUnix', [], { -> return unix })
        helper.registerAllowedMethod('sh', [Map], { final Map<String, Object> ignored -> return COMMIT + '\n' })
        helper.registerAllowedMethod('bat', [Map], { final Map<String, Object> ignored -> return COMMIT + '\r\n' })
        checkoutScript = loadScript('vars/gitUtils.groovy')
        logScript = loadScript('vars/log.groovy')
    }

    @Test
    void mapsDefaultBranchCheckoutToJenkinsSteps() {
        final GitCheckoutResult result = checkoutScript.checkout(
                GitCheckoutOptions.builder(URL).branch('main').build())

        assertEquals([[name: 'refs/remotes/origin/main']], scmCalls[0].branches)
        assertEquals([[url: URL, name: 'origin', refspec: '+refs/heads/*:refs/remotes/origin/*']],
                scmCalls[0].userRemoteConfigs)
        assertEquals([
                [$class: 'CloneOption', shallow: false, noTags: false, reference: '', timeout: 30, honorRefspec: true],
                [$class: 'CheckoutOption', timeout: 15],
        ], scmCalls[0].extensions)
        assertEquals(false, checkoutCalls[0].poll)
        assertEquals(false, checkoutCalls[0].changelog)
        assertEquals(COMMIT, result.commit)
        assertEquals('origin/main', result.branch)
        assertEquals([
                '[INFO] [GitHelper.checkout] Checking out repository',
                '[INFO] [GitHelper.checkout] Checked out commit ' + COMMIT,
        ], messages)
    }

    @Test
    void mapsExplicitCheckoutControls() {
        final GitCheckoutOptions options = GitCheckoutOptions.builder(URL).branch('release')
                .remoteName('upstream').credentialsId('source-reader')
                .refspec('+refs/heads/release:refs/remotes/upstream/release')
                .fetchTags(false).shallowDepth(5).cleanBeforeCheckout(true)
                .pruneStaleBranches(true).poll(true).changelog(true).build()
        final PipelineLogger logger = logScript.forContext('Source', LogLevel.DEBUG)

        checkoutScript.checkout(options, logger)

        assertEquals('source-reader', scmCalls[0].userRemoteConfigs[0].credentialsId)
        assertEquals('refs/remotes/upstream/release', scmCalls[0].branches[0].name)
        assertTrue((boolean) scmCalls[0].extensions[0].shallow)
        assertEquals(5, scmCalls[0].extensions[0].depth)
        assertTrue((boolean) checkoutCalls[0].poll)
        assertTrue((boolean) checkoutCalls[0].changelog)
        assertFalse(messages.join('\n').contains(URL))
        assertFalse(messages.join('\n').contains('source-reader'))
    }

    @Test
    void mapsTagsAndCommitsWithoutInventingBranchMetadata() {
        final GitCheckoutResult tag = checkoutScript.checkout(GitCheckoutOptions.builder(URL).tag('v1').build())
        final GitCheckoutResult commit = checkoutScript.checkout(GitCheckoutOptions.builder(URL).commit(COMMIT).build())

        assertEquals('refs/tags/v1', scmCalls[0].branches[0].name)
        assertEquals(COMMIT, scmCalls[1].branches[0].name)
        assertEquals(null, tag.branch)
        assertEquals(null, commit.branch)
    }

    @Test
    void readsHeadWithThePlatformAppropriateStep() {
        unix = false

        checkoutScript.checkout(GitCheckoutOptions.builder(URL).branch('main').build())

        assertTrue(helper.callStack.any { final Object call -> return call.methodName == 'bat' })
        assertFalse(helper.callStack.any { final Object call -> return call.methodName == 'sh' })
    }

    @Test
    void propagatesCheckoutFailuresWithoutRetrying() {
        checkoutFailure = new RuntimeException('transport failure')

        final RuntimeException thrown = assertThrows(RuntimeException) { ->
            checkoutScript.checkout(GitCheckoutOptions.builder(URL).branch('main').build())
        }

        assertSame(checkoutFailure, thrown)
        assertEquals(1, checkoutCalls.size())
        assertEquals(['[INFO] [GitHelper.checkout] Checking out repository'], messages)
    }

    @Test
    void rejectsAnUnexpectedExplicitCommit() {
        assertThrows(IllegalStateException) { ->
            checkoutScript.checkout(GitCheckoutOptions.builder(URL).commit('a' * 40).build())
        }
        assertEquals(1, checkoutCalls.size())
    }
}
