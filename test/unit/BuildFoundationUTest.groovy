// Copyright 2026 Hamid Gholami
// SPDX-License-Identifier: Apache-2.0

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull

class BuildFoundationUTest extends BasePipelineTest {

    @Override
    @BeforeEach
    void setUp() {
        scriptRoots = ['test/fixtures/pipelines']
        super.setUp()
    }

    @Test
    void separateDirectoriesAllowRepeatedJenkinsfileNames() {
        final Script firstPipeline = loadScript('first/Jenkinsfile.groovy')
        final Script secondPipeline = loadScript('nested/second/Jenkinsfile.groovy')

        assertEquals('first', firstPipeline.run())
        assertEquals('second', secondPipeline.run())
    }

    @Test
    void pipelineStepsCanUseTestOnlyResources() {
        helper.registerAllowedMethod('libraryResource', [String], { final String resourceName ->
            final InputStream stream = getClass().getResourceAsStream('/' + resourceName)
            assertNotNull(stream)
            try {
                return stream.getText('UTF-8').trim()
            } finally {
                stream.close()
            }
        })
        final Script pipeline = loadScript('resource/Jenkinsfile.groovy')

        assertEquals('foundation fixture', pipeline.run())
    }
}
