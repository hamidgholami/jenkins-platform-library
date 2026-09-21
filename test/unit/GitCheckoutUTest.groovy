/**
 * Copyright 2026 Hamid Gholami
 * SPDX-License-Identifier: Apache-2.0
 * */

import com.lesfurets.jenkins.unit.BasePipelineTest
import com.lesfurets.jenkins.unit.MethodCall
import hudson.AbortException
import hudson.model.Result
import io.github.hamidgholami.jenkins.platform.git.GitCheckoutOptions
import io.github.hamidgholami.jenkins.platform.git.GitCheckoutResult
import io.github.hamidgholami.jenkins.platform.logging.LogLevel
import io.github.hamidgholami.jenkins.platform.logging.PipelineLogger
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertNull
import static org.junit.jupiter.api.Assertions.assertSame
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

class GitCheckoutUTest extends BasePipelineTest {

    private static final String COMMIT = '0123456789abcdef0123456789abcdef01234567'
    private static final String URL = 'https://git.example.org/team/service.git'

    private Script checkoutScript
    private Script logScript
    private final List<Map<String, Object>> scmCalls = []
    private final List<Map<String, Object>> checkoutCalls = []
    private final List<String> messages = []
    private Map<String, Object> checkoutMetadata
    private Exception checkoutFailure
    private String workspaceCommit
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
        workspaceCommit = COMMIT
        unix = true
        checkoutMetadata = [GIT_COMMIT: COMMIT, GIT_BRANCH: 'origin/main']
        helper.registerAllowedMethod('echo', [String], { final String message -> messages.add(message) })
        helper.registerAllowedMethod('scmGit', [Map], { final Map<String, Object> arguments ->
            scmCalls.add(arguments)
            return [configuredScm: arguments]
        })
        helper.registerAllowedMethod('checkout', [Map], { final Map<String, Object> arguments ->
            checkoutCalls.add(arguments)
            if (checkoutFailure != null) {
                throw checkoutFailure
            }
            return checkoutMetadata
        })
        helper.registerAllowedMethod('isUnix', [], { -> return unix })
        helper.registerAllowedMethod('sh', [Map], { final Map<String, Object> arguments ->
            assertEquals('git rev-parse --verify HEAD', arguments.script)
            assertEquals(true, arguments.returnStdout)
            return workspaceCommit + '\n'
        })
        helper.registerAllowedMethod('bat', [Map], { final Map<String, Object> arguments ->
            assertEquals('@git rev-parse --verify HEAD', arguments.script)
            assertEquals(true, arguments.returnStdout)
            return workspaceCommit + '\r\n'
        })
        for (final String forbidden : ['git', 'deleteDir', 'retry', 'error']) {
            helper.registerAllowedMethod(forbidden, [Map], { final Map<String, Object> ignored ->
                throw new AssertionError('Unexpected step: ' + forbidden)
            })
        }
        checkoutScript = loadScript('vars/gitUtils.groovy')
        logScript = loadScript('vars/log.groovy')
    }

    @Test
    void defaultsPreserveHistoryAndMapPluginTimeouts() {
        final GitCheckoutResult result = checkoutScript.checkout(GitCheckoutOptions.builder(URL).branch('main').build())
        assertEquals([[
                branches: [[name: 'refs/remotes/origin/main']],
                userRemoteConfigs: [[url: URL, name: 'origin', refspec: '+refs/heads/*:refs/remotes/origin/*']],
                extensions: [
                        [$class: 'CloneOption', shallow: false, noTags: false, reference: '', timeout: 30, honorRefspec: true],
                        [$class: 'CheckoutOption', timeout: 15],
                ],
        ]], scmCalls)
        assertEquals([[scm: [configuredScm: scmCalls[0]], poll: false, changelog: false]], checkoutCalls)
        assertEquals(COMMIT, result.commit)
        assertEquals('origin/main', result.branch)
        assertNull(result.localBranch)
        assertEquals(['[INFO] [GitHelper.checkout] Checking out repository',
                '[INFO] [GitHelper.checkout] Checked out commit ' + COMMIT], messages)
        assertJobStatusSuccess()
    }

    @Test
    void mapsTuningOptionsAndUsesTheSuppliedLogger() {
        final GitCheckoutOptions options = GitCheckoutOptions.builder('git@git.example.org:team/service.git')
                .branch('release/next').remoteName('upstream').credentialsId('source-reader')
                .refspec('+refs/heads/release/next:refs/remotes/upstream/release/next')
                .honorRefspec(false).fetchTags(false).shallowDepth(25)
                .cloneTimeoutMinutes(90).checkoutTimeoutMinutes(20)
                .referenceRepository('/agent/cache/service.git')
                .cleanBeforeCheckout(true).pruneStaleBranches(true).poll(true).changelog(true).build()
        final PipelineLogger logger = logScript.forContext('Source', LogLevel.DEBUG, true)
        checkoutScript.checkout(options, logger)

        assertEquals([[name: 'refs/remotes/upstream/release/next']], scmCalls[0].branches)
        assertEquals([[
                url: 'git@git.example.org:team/service.git', name: 'upstream', credentialsId: 'source-reader',
                refspec: '+refs/heads/release/next:refs/remotes/upstream/release/next',
        ]], scmCalls[0].userRemoteConfigs)
        assertEquals([
                [$class: 'CloneOption', shallow: true, depth: 25, noTags: true, reference: '/agent/cache/service.git',
                 timeout: 90, honorRefspec: false],
                [$class: 'CheckoutOption', timeout: 20],
                [$class: 'CleanBeforeCheckout', deleteUntrackedNestedRepositories: false],
                [$class: 'PruneStaleBranch'],
        ], scmCalls[0].extensions)
        assertTrue((boolean) checkoutCalls[0].poll)
        assertTrue((boolean) checkoutCalls[0].changelog)
        assertTrue(messages[1].contains('[DEBUG]\u001B[0m [Source] Clone/fetch timeout: 90 min; checkout timeout: 20 min'))
        assertFalse(messages.join('\n').contains('source-reader'))
        assertFalse(messages.join('\n').contains('git.example.org'))
    }

    @Test
    void distinguishesTagsAndCommitsFromBranches() {
        checkoutScript.checkout(GitCheckoutOptions.builder(URL).tag('release/1.0').build())
        checkoutScript.checkout(GitCheckoutOptions.builder(URL).commit(COMMIT).build())
        assertEquals([[name: 'refs/tags/release/1.0']], scmCalls[0].branches)
        assertEquals([[name: COMMIT]], scmCalls[1].branches)
    }

    @Test
    void repeatedCheckoutDelegatesWorkspaceReuseToThePlugin() {
        final GitCheckoutOptions options = GitCheckoutOptions.builder(URL).branch('main').build()
        checkoutScript.checkout(options)
        checkoutScript.checkout(options)
        assertEquals(2, checkoutCalls.size())
        assertEquals(scmCalls[0], scmCalls[1])
        assertFalse(helper.callStack.any { final MethodCall call ->
            return call.methodName in ['deleteDir', 'retry', 'git']
        })
    }

    @Test
    void reportsTheWorkspaceRevisionWhenPluginMetadataIsStale() {
        checkoutMetadata = [GIT_COMMIT: 'a' * 40, GIT_BRANCH: 'origin/old', GIT_LOCAL_BRANCH: 'old']
        final GitCheckoutResult result = checkoutScript.checkout(GitCheckoutOptions.builder(URL).branch('main').build())
        assertEquals(COMMIT, result.commit)
        assertEquals('origin/main', result.branch)
        assertNull(result.localBranch)
    }

    @Test
    void readsTheWorkspaceOnWindowsWithoutEchoingTheCommand() {
        unix = false
        final GitCheckoutResult result = checkoutScript.checkout(GitCheckoutOptions.builder(URL).commit(COMMIT).build())
        assertEquals(COMMIT, result.commit)
        assertNull(result.branch)
        assertTrue(helper.callStack.any { final MethodCall invocation -> return invocation.methodName == 'bat' })
        assertFalse(helper.callStack.any { final MethodCall invocation -> return invocation.methodName == 'sh' })
    }

    @Test
    void propagatesCheckoutFailureWithoutRetryOrLeakingItsMessage() {
        checkoutFailure = new AbortException('sensitive transport diagnostic')
        final AbortException thrown = assertThrows(AbortException) { ->
            checkoutScript.checkout(GitCheckoutOptions.builder(URL).branch('main').build())
        }
        assertSame(checkoutFailure, thrown)
        assertEquals(1, checkoutCalls.size())
        assertEquals(['[INFO] [GitHelper.checkout] Checking out repository'], messages)
    }

    @Test
    void propagatesCancellationWithoutConvertingItToFailure() {
        checkoutFailure = new FlowInterruptedException(Result.ABORTED, true)
        final FlowInterruptedException thrown = assertThrows(FlowInterruptedException) { ->
            checkoutScript.checkout(GitCheckoutOptions.builder(URL).branch('main').build())
        }
        assertSame(checkoutFailure, thrown)
        assertEquals(1, checkoutCalls.size())
    }

    @Test
    void rejectsNullOptionsBeforeLoggingOrCheckout() {
        final PipelineLogger logger = logScript.forContext('Source')
        assertThrows(IllegalArgumentException) { -> checkoutScript.checkout(null, logger) }
        assertTrue(scmCalls.isEmpty())
        assertTrue(checkoutCalls.isEmpty())
        assertTrue(messages.isEmpty())
    }

    @Test
    void invalidResultDoesNotProduceASuccessMessage() {
        workspaceCommit = 'not-a-commit'
        assertThrows(IllegalStateException) { ->
            checkoutScript.checkout(GitCheckoutOptions.builder(URL).branch('main').build())
        }
        assertEquals(['[INFO] [GitHelper.checkout] Checking out repository'], messages)
    }

    @Test
    void refusesToReportSuccessForAnUnexpectedCommit() {
        assertThrows(IllegalStateException) { ->
            checkoutScript.checkout(GitCheckoutOptions.builder(URL).commit('a' * 40).build())
        }
        assertEquals(['[INFO] [GitHelper.checkout] Checking out repository'], messages)
    }
}
