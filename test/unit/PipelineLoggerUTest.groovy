/**
 * Copyright 2026 Hamid Gholami
 * SPDX-License-Identifier: Apache-2.0
 * */

import com.lesfurets.jenkins.unit.BasePipelineTest
import hudson.AbortException
import hudson.model.Result
import io.github.hamidgholami.jenkins.platform.logging.LogLevel
import io.github.hamidgholami.jenkins.platform.logging.PipelineLogger
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertSame
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

class PipelineLoggerUTest extends BasePipelineTest {

    private Script logScript
    private final List<String> messages = []
    private final List<String> failures = []
    private AbortException nativeFailure

    @Override
    @BeforeEach
    void setUp() {
        scriptRoots = ['.']
        super.setUp()
        messages.clear()
        failures.clear()
        nativeFailure = new AbortException('native failure')
        helper.registerAllowedMethod('echo', [String], { final String message ->
            messages.add(message)
        })
        helper.registerAllowedMethod('error', [String], { final String message ->
            assertFalse(messages.isEmpty())
            failures.add(message)
            throw nativeFailure
        })
        logScript = loadScript('vars/log.groovy')
    }

    @ParameterizedTest
    @EnumSource(LogLevel)
    void thresholdsIncludeTheirOwnLevelAndHigher(final LogLevel threshold) {
        final PipelineLogger logger = logScript.forContext('Build', threshold)
        logger.trace('trace')
        logger.debug('debug')
        logger.info('info')
        logger.warn('warn')
        logger.error('error')

        final Map<LogLevel, List<String>> expected = [
                (LogLevel.TRACE): ['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR'],
                (LogLevel.DEBUG): ['DEBUG', 'INFO', 'WARN', 'ERROR'],
                (LogLevel.INFO): ['INFO', 'WARN', 'ERROR'],
                (LogLevel.WARN): ['WARN', 'ERROR'],
                (LogLevel.ERROR): ['ERROR'],
        ]
        assertEquals(expected.get(threshold).size(), messages.size())
        expected.get(threshold).eachWithIndex { final String level, final int index ->
            assertTrue(messages[index].startsWith('[' + level + '] [Build] '))
        }
        assertTrue(failures.isEmpty())
        assertJobStatusSuccess()
    }

    @Test
    void defaultsAreInfoAndPlainText() {
        final PipelineLogger logger = logScript.forContext('GitHelper.checkout')
        logger.debug('hidden')
        logger.info('Checking out repository')
        assertEquals(['[INFO] [GitHelper.checkout] Checking out repository'], messages)
    }

    @Test
    void prefixesEveryLineIncludingEmptyLinesAndNormalizesLineEndings() {
        final PipelineLogger logger = logScript.forContext('Build')
        logger.warn('first\r\n\rthird\nfourth\n')
        assertEquals(['[WARN] [Build] first\n[WARN] [Build] \n[WARN] [Build] third\n' +
                '[WARN] [Build] fourth\n[WARN] [Build] '], messages)
    }

    @Test
    void colorsOnlySeverityAndResetsOnEveryLine() {
        final PipelineLogger logger = logScript.forContext('Build', LogLevel.TRACE, true)
        logger.error('first\nsecond')
        assertEquals(['\u001B[31m[ERROR]\u001B[0m [Build] first\n' +
                '\u001B[31m[ERROR]\u001B[0m [Build] second'], messages)
    }

    @Test
    void instancesKeepContextThresholdAndColorIndependent() {
        final PipelineLogger left = logScript.forContext('Left', LogLevel.DEBUG, true)
        final PipelineLogger right = logScript.forContext('Right', LogLevel.ERROR)
        left.debug('visible')
        right.info('hidden')
        right.error('visible')
        left.debug('still visible')
        assertEquals([
                '\u001B[36m[DEBUG]\u001B[0m [Left] visible',
                '[ERROR] [Right] visible',
                '\u001B[36m[DEBUG]\u001B[0m [Left] still visible',
        ], messages)
    }

    @Test
    void failLogsBeforeInvokingNativeErrorAndPropagatesItsException() {
        final PipelineLogger logger = logScript.forContext('Build', LogLevel.ERROR, true)
        final AbortException thrown = assertThrows(AbortException) { -> logger.fail('Cannot continue') }
        assertSame(nativeFailure, thrown)
        assertEquals(['\u001B[31m[ERROR]\u001B[0m [Build] Cannot continue'], messages)
        assertEquals(['Cannot continue'], failures)
        assertJobStatusSuccess()
    }

    @Test
    void errorAllowsRethrowingTheOriginalInterruption() {
        final PipelineLogger logger = logScript.forContext('Build')
        final FlowInterruptedException original = new FlowInterruptedException(Result.ABORTED, true)
        final FlowInterruptedException thrown = assertThrows(FlowInterruptedException) { ->
            try {
                throw original
            } catch (final FlowInterruptedException interruption) {
                logger.error('Build interrupted')
                throw interruption
            }
        }
        assertSame(original, thrown)
        assertTrue(failures.isEmpty())
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = [' ', ' context', 'context ', 'bad\ncontext', '[Build]', 'bad\u001Bcontext'])
    void rejectsInvalidContextBeforeAnyStep(final String context) {
        assertThrows(IllegalArgumentException) { -> logScript.forContext(context) }
        assertTrue(messages.isEmpty())
    }

    @Test
    void rejectsMissingLevelScriptAndMessage() {
        assertThrows(IllegalArgumentException) { -> new PipelineLogger(null, 'Build') }
        assertThrows(IllegalArgumentException) { -> logScript.forContext('Build', null) }
        final PipelineLogger logger = logScript.forContext('Build')
        assertThrows(IllegalArgumentException) { -> logger.info(null) }
        assertTrue(messages.isEmpty())
    }
}
