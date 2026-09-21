/**
 * Copyright 2026 Hamid Gholami
 * SPDX-License-Identifier: Apache-2.0
 * */

import com.lesfurets.jenkins.unit.MethodCall
import com.lesfurets.jenkins.unit.cps.BasePipelineTestCPS
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static com.lesfurets.jenkins.unit.global.lib.LibraryConfiguration.library
import static com.lesfurets.jenkins.unit.global.lib.ProjectSource.projectSource
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

/**
 * CPS-transformed mock execution complements unit tests; it is not a Jenkins sandbox or restart test.
 * */
class UtilitiesPipelineUTest extends BasePipelineTestCPS {

    private final List<String> messages = []
    private final List<Map<String, Object>> checkouts = []

    @Override
    @BeforeEach
    void setUp() {
        scriptRoots = ['test/fixtures/pipelines']
        super.setUp()
        /**
         * Preserve class identity for typed arguments already on Gradle's test classpath.
         * This transforms the consumer; library classes are the compiled Gradle classes.
         * */
        helper.libLoader.preloadLibraryClasses = false
        helper.libLoader.groovyClassLoader.parseClass(new File('vars/log.groovy'))
        helper.libLoader.groovyClassLoader.parseClass(new File('vars/gitUtils.groovy'))
        helper.registerSharedLibrary(library('jenkins-platform-library')
                .defaultVersion('main').allowOverride(false).implicit(false)
                .targetPath('build/test-libraries').retriever(projectSource()).build())
        helper.registerAllowedMethod('echo', [String], { final String message -> messages.add(message) })
        helper.registerAllowedMethod('scmGit', [Map], { final Map<String, Object> arguments -> return arguments })
        helper.registerAllowedMethod('checkout', [Map], { final Map<String, Object> arguments ->
            checkouts.add(arguments)
            return [GIT_COMMIT: '0123456789abcdef0123456789abcdef01234567']
        })
        helper.registerAllowedMethod('ansiColor', [String, Closure], { final String palette, final Closure body ->
            assertEquals('xterm', palette)
            helper.callClosure(body)
        })
        helper.registerAllowedMethod('timestamps', [Closure], { final Closure body ->
            helper.callClosure(body)
        })
    }

    @Test
    void libraryImportsAndNamedOperationsWorkInACpsTransformedPipeline() {
        runScript('utilities/Jenkinsfile.groovy')
        assertEquals(1, checkouts.size())
        assertEquals(45, checkouts[0].scm.extensions[0].timeout)
        assertEquals('\u001B[32m[INFO]\u001B[0m [Source] Ready\n' +
                '\u001B[32m[INFO]\u001B[0m [Source] Commit 0123456789abcdef0123456789abcdef01234567', messages.last())
        assertTrue(helper.callStack.any { final MethodCall invocation -> return invocation.methodName == 'dir' })
        assertJobStatusSuccess()
    }
}
