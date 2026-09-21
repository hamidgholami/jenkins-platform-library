import com.lesfurets.jenkins.unit.BasePipelineTest
import io.github.hamidgholami.jenkins.platform.logging.LogLevel
import io.github.hamidgholami.jenkins.platform.logging.PipelineLogger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertSame
import static org.junit.jupiter.api.Assertions.assertThrows

class PipelineLoggerUTest extends BasePipelineTest {

    private Script logScript
    private final List<String> messages = []
    private final RuntimeException nativeFailure = new RuntimeException('native failure')

    @Override
    @BeforeEach
    void setUp() {
        scriptRoots = ['.']
        super.setUp()
        messages.clear()
        helper.registerAllowedMethod('echo', [String], { final String message -> messages.add(message) })
        helper.registerAllowedMethod('error', [String], { final String ignored -> throw nativeFailure })
        logScript = loadScript('vars/log.groovy')
    }

    @Test
    void filtersMessagesAtTheConfiguredThreshold() {
        final PipelineLogger logger = logScript.forContext('Build', LogLevel.WARN)

        logger.info('hidden')
        logger.warn('warning')
        logger.error('failure')

        assertEquals(['[WARN] [Build] warning', '[ERROR] [Build] failure'], messages)
    }

    @Test
    void defaultsToPlainInfoLogging() {
        final PipelineLogger logger = logScript.forContext('Build')

        logger.debug('hidden')
        logger.info('ready')

        assertEquals(['[INFO] [Build] ready'], messages)
    }

    @Test
    void formatsEveryLineAndNormalizesLineEndings() {
        final PipelineLogger logger = logScript.forContext('Build')

        logger.warn('first\r\nsecond\n')

        assertEquals(['[WARN] [Build] first\n[WARN] [Build] second\n[WARN] [Build] '], messages)
    }

    @Test
    void colorsOnlyTheSeverityLabel() {
        final PipelineLogger logger = logScript.forContext('Build', LogLevel.DEBUG, true)

        logger.debug('ready')

        assertEquals(['\u001B[36m[DEBUG]\u001B[0m [Build] ready'], messages)
    }

    @Test
    void failLogsAndDelegatesToTheNativeErrorStep() {
        final PipelineLogger logger = logScript.forContext('Build')

        final RuntimeException thrown = assertThrows(RuntimeException) { -> logger.fail('cannot continue') }

        assertSame(nativeFailure, thrown)
        assertEquals(['[ERROR] [Build] cannot continue'], messages)
    }

    @Test
    void rejectsInvalidContextAndNullMessages() {
        assertThrows(IllegalArgumentException) { -> logScript.forContext(' ') }
        final PipelineLogger logger = logScript.forContext('Build')
        assertThrows(IllegalArgumentException) { -> logger.info(null) }
        assertEquals([], messages)
    }
}
