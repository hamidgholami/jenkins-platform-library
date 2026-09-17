// Copyright 2026 Hamid Gholami
// SPDX-License-Identifier: Apache-2.0

import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.maven.PomModuleDescriptor
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.plugins.quality.CodeNarc
import java.security.MessageDigest

plugins {
    groovy
    codenarc
}

group = "io.github.hamidgholami.jenkins"
version = "0.1.0-SNAPSHOT"

/** Select companion JARs without replacing POM dependencies or repeating versions. */
@CacheableRule
abstract class JenkinsPluginJarRule : ComponentMetadataRule {
    override fun execute(context: ComponentMetadataContext) {
        val packaging = context.getDescriptor(PomModuleDescriptor::class.java)?.packaging
        if (packaging == "hpi" || packaging == "jpi") {
            val module = context.details.id
            context.details.allVariants {
                withFiles {
                    removeAllFiles()
                    addFile("${module.name}-${module.version}.jar")
                }
            }
        }
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

sourceSets {
    main {
        java.setSrcDirs(emptyList<String>())
        groovy.setSrcDirs(listOf("src", "vars"))
        resources.setSrcDirs(listOf("resources"))
    }
    test {
        java.setSrcDirs(emptyList<String>())
        groovy.setSrcDirs(listOf("test/unit"))
        resources.setSrcDirs(listOf("test/fixtures/resources"))
    }
}

val integrationTestSource = sourceSets.create("integrationTest") {
    java.setSrcDirs(emptyList<String>())
    groovy.setSrcDirs(listOf("test/integration"))
    resources.setSrcDirs(listOf("test/integration-resources"))
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

val jenkinsApis = configurations.create("jenkinsApis") {
    isCanBeConsumed = false
    isCanBeResolved = false
}
val libraryCompiler = configurations.create("libraryCompiler") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

configurations.compileOnly { extendsFrom(jenkinsApis) }
configurations.testImplementation { extendsFrom(jenkinsApis) }
configurations[integrationTestSource.implementationConfigurationName].extendsFrom(
    configurations.testImplementation.get(),
)
configurations[integrationTestSource.runtimeOnlyConfigurationName].extendsFrom(
    configurations.testRuntimeOnly.get(),
)

dependencies {
    components.all<JenkinsPluginJarRule>()
    implementation(libs.groovy)
    add(libraryCompiler.name, libs.groovy)
    add(jenkinsApis.name, libs.jenkins.core)
    add(jenkinsApis.name, libs.pipeline.groovy.lib)
    testImplementation(libs.pipeline.unit)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
    codenarc(libs.codenarc)
}

dependencyLocking {
    lockAllConfigurations()
    lockMode = LockMode.STRICT
}

tasks.withType<GroovyCompile>().configureEach {
    // Groovy 2.4 emits Java 8 bytecode; dependencies and execution target JDK 21.
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
    groovyClasspath = libraryCompiler
    groovyOptions.configurationScript = file("config/compiler.groovy")
    groovyOptions.encoding = "UTF-8"
    groovyOptions.forkOptions.memoryMaximumSize = "512m"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    maxParallelForks = 1
    maxHeapSize = "512m"
    testLogging {
        events("failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

val integrationSuiteReady = tasks.register("integrationSuiteReady") {
    group = "verification"
    description = "Reject an empty integration suite until the real Jenkins milestone is implemented."
    doLast {
        throw GradleException(
            "Real Jenkins integration tests are deferred to the consumer-test milestone. " +
                "Run foundationCheck for the implemented foundation; do not report full check/build as passing.",
        )
    }
}

val integrationTest = tasks.register<Test>("integrationTest") {
    description = "Run the distinct real Jenkins suite (implementation deferred)."
    group = "verification"
    testClassesDirs = integrationTestSource.output.classesDirs
    classpath = integrationTestSource.runtimeClasspath
    dependsOn(integrationSuiteReady)
    shouldRunAfter(tasks.test)
}

codenarc {
    toolVersion = libs.versions.codenarc.get()
    configFile = file("config/codenarc.groovy")
    isIgnoreFailures = false
}
tasks.withType<CodeNarc>().configureEach {
    // Analyze syntax without mixing Jenkins's Groovy 2.4 into CodeNarc's runtime.
    compilationClasspath = files()
    reports {
        html.required = true
        xml.required = true
    }
}

val pipelineFiles = fileTree("pipelines") { include("**/Jenkinsfile.groovy") }
val fixturePipelines = fileTree("test/fixtures/pipelines") { include("**/Jenkinsfile.groovy") }
val compilePipelines = tasks.register("compilePipelines") {
    group = "verification"
    description = "Compile each consumer and fixture Jenkinsfile into its own output directory."
}

(pipelineFiles.files + fixturePipelines.files).sortedBy { it.relativeTo(projectDir).path }.forEach { pipeline ->
    val relativePath = pipeline.relativeTo(projectDir).invariantSeparatorsPath
    val pathId = MessageDigest.getInstance("SHA-256")
        .digest(relativePath.toByteArray()).joinToString("") { "%02x".format(it) }
    val compilePipeline = tasks.register<GroovyCompile>("compilePipeline_$pathId") {
        source(pipeline)
        classpath = sourceSets.main.get().compileClasspath + sourceSets.main.get().output
        destinationDirectory = layout.buildDirectory.dir("classes/pipelines/$pathId")
        dependsOn(tasks.classes)
    }
    compilePipelines.configure { dependsOn(compilePipeline) }
}

val codenarcPipelines = tasks.register<CodeNarc>("codenarcPipelines") {
    source(pipelineFiles, fixturePipelines)
}

val formatCheck = tasks.register("formatCheck") {
    group = "verification"
    description = "Check repository text formatting without rewriting files."
    val textFiles = fileTree(projectDir) {
        include("**/*.md", "**/*.groovy", "**/*.kts", "**/*.toml", "**/*.yml", "**/*.yaml", "**/*.properties")
        exclude(".git/**", ".gradle/**", "build/**", "gradle/wrapper/**")
    }
    inputs.files(textFiles)
    doLast {
        val violations = mutableListOf<String>()
        textFiles.forEach { source ->
            val content = source.readText()
            val path = source.relativeTo(projectDir).path
            if (content.isNotEmpty() && !content.endsWith("\n")) violations.add("$path: missing final newline")
            content.lineSequence().forEachIndexed { index, line ->
                if (line.contains('\t') || line.endsWith(' ') || line.endsWith('\r')) {
                    violations.add("$path:${index + 1}: tab, trailing whitespace or CRLF")
                }
            }
        }
        check(violations.isEmpty()) { violations.joinToString("\n") }
    }
}

val verifyDependencies = tasks.register("verifyDependencies") {
    group = "verification"
    description = "Verify JAR selection, plugin transitives and the Jenkins Groovy baseline."
    val compileClasspath = configurations.compileClasspath
    val testRuntimeClasspath = configurations.testRuntimeClasspath
    val expectedGroovy = libs.versions.groovy
    inputs.files(compileClasspath, testRuntimeClasspath)
    doLast {
        listOf(compileClasspath.get(), testRuntimeClasspath.get()).forEach { configuration ->
            val archives = configuration.files.filter { it.extension in setOf("hpi", "jpi", "war") }
            check(archives.isEmpty()) { "${configuration.name} contains non-JAR archives: $archives" }
            val modules = configuration.incoming.resolutionResult.allComponents.mapNotNull {
                it.id as? ModuleComponentIdentifier
            }
            val groovyModules = modules.filter { it.group in setOf("org.codehaus.groovy", "org.apache.groovy") }
            check(groovyModules.isNotEmpty() && groovyModules.all { it.version == expectedGroovy.get() }) {
                "${configuration.name} has an incompatible Groovy runtime: $groovyModules"
            }
            check(modules.any { it.group == "org.jenkins-ci.plugins.workflow" && it.module == "workflow-cps" }) {
                "Pipeline plugin transitive dependencies were lost from ${configuration.name}"
            }
            check(configuration.files.any { it.name.startsWith("pipeline-groovy-lib-") && it.extension == "jar" }) {
                "Companion plugin JAR missing from ${configuration.name}"
            }
        }
    }
}

tasks.register("resolveDependencies") {
    group = "build setup"
    description = "Resolve all classpaths when intentionally refreshing locks and verification metadata."
    doLast {
        configurations.filter { it.isCanBeResolved }.forEach { it.resolve() }
    }
}

tasks.register("foundationCheck") {
    group = "verification"
    description = "Run implemented foundation checks; does not claim real Jenkins compatibility."
    dependsOn(tasks.test, tasks.named("codenarcMain"), tasks.named("codenarcTest"))
    dependsOn(tasks.named("codenarcIntegrationTest"), codenarcPipelines)
    dependsOn(compilePipelines, formatCheck, verifyDependencies)
}

tasks.check {
    dependsOn("foundationCheck", integrationTest)
}

tasks.jar {
    // Jenkins consumes source through Git, not a library runtime JAR.
    enabled = false
}
