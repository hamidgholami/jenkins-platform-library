/**
 * Copyright 2026 Hamid Gholami
 * SPDX-License-Identifier: Apache-2.0
 * */

package io.github.hamidgholami.jenkins.platform.testing

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation)
class LibraryIntegrationTest {

    private JenkinsEnvironment environment

    @BeforeAll
    void startEnvironment() {
        environment = new JenkinsEnvironment()
        environment.start()
    }

    @AfterAll
    void stopEnvironment() {
        if (environment != null) {
            environment.close()
        }
    }

    @Test
    @Order(1)
    void checkoutAndLoggingRunOnAnAgentWithAnUntrustedLibrary() {
        assertEquals('SUCCESS', environment.run('checkout', environment.pipeline('checkout.groovy')))
        final String console = environment.console('checkout').replaceAll('\u001B\\[[0-9;]*m', '')
        assertTrue(console.contains('CHECKOUT_AND_LOGGING_OK'))
        assertTrue(console.contains('[DEBUG] [Integration] DEBUG_VISIBLE'))
        assertFalse(console.contains('TRACE_MUST_BE_HIDDEN'))
        assertTrue(console.contains('[INFO] [Integration] MULTILINE_SECOND'))
        assertTrue(console.contains(environment.fixtures.libraryCommit))
        final String html = environment.request('GET', '/job/checkout/1/console')
        new File(System.getProperty('jpl.projectDir'), 'build/reports/jenkins/checkout.html').setText(html, 'UTF-8')
        assertTrue(html.contains('<span style="color: #00CDCD;">[DEBUG]'))
        final String timestamps = environment.request('GET', '/job/checkout/1/timestamps/?time=HH:mm:ss&appendLog')
        assertTrue(timestamps.matches('(?s).*\\d{2}:\\d{2}:\\d{2}.*CHECKOUT_AND_LOGGING_OK.*'))
    }

    @Test
    @Order(2)
    void failuresKeepNativeJenkinsBehavior() {
        assertEquals('FAILURE', environment.run('failure', environment.pipeline('failure.groovy')))
        assertTrue(environment.console('failure').contains('[ERROR] [Failure] EXPECTED_NATIVE_FAILURE'))
        assertEquals('UNSTABLE', environment.run('handled-failure', environment.pipeline('handled-failure.groovy')))
        assertTrue(environment.console('handled-failure').contains('CONTINUED_AFTER_HANDLED_FAILURE'))
        assertEquals('FAILURE', environment.run('authentication-failure', environment.pipeline('authentication-failure.groovy')))
        assertTrue(environment.console('authentication-failure').contains('Authentication failed'))
        assertFalse(environment.console('authentication-failure').contains('Checked out commit'))
    }

    @Test
    @Order(3)
    void controllerRestartPreservesHelperLoggerOptionsAndResult() {
        environment.createJob('restart', environment.pipeline('restart.groovy'))
        environment.request('POST', '/job/restart/build')
        environment.awaitInput('restart')
        environment.restart()
        environment.request('POST', '/job/restart/1/input/Resume/proceedEmpty')
        assertEquals('SUCCESS', environment.awaitResult('restart'))
        assertTrue(environment.console('restart').contains('[INFO] [Restart] RESTART_OK'))
    }

    @Test
    @Order(4)
    void cancellationStaysAborted() {
        environment.createJob('cancel', environment.pipeline('restart.groovy'))
        environment.request('POST', '/job/cancel/build')
        environment.awaitInput('cancel')
        environment.request('POST', '/job/cancel/1/stop')
        assertEquals('ABORTED', environment.awaitResult('cancel'))
        assertFalse(environment.console('cancel').contains('RESTART_OK'))
    }

    @Test
    @Order(5)
    void sandboxRemainsEnabled() {
        final String script = 'import jenkins.model.Jenkins\nJenkins.get()\n'
        assertEquals('FAILURE', environment.run('sandbox', script))
        assertTrue(environment.console('sandbox').contains('Scripts not permitted'))
    }
}
