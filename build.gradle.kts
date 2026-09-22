plugins {
    groovy
    codenarc
    idea
}

group = "io.github.hamidgholami.jenkins"
version = "0.1.0-SNAPSHOT"

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
    }
}

dependencies {
    implementation(libs.groovy)
    compileOnly(libs.groovy.cps)
    compileOnly("io.jenkins.plugins:pipeline-groovy-lib:${libs.versions.pipeline.groovy.lib.get()}@jar") {
        isTransitive = false
    }
    testImplementation(libs.pipeline.unit)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
    codenarc(libs.codenarc)
}

tasks.withType<GroovyCompile>().configureEach {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
    groovyOptions.encoding = "UTF-8"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    maxParallelForks = 1
    testLogging {
        events("passed", "failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

val pipelineSourceDirectories = fileTree("pipelines") {
    include("**/Jenkinsfile.groovy")
}.files.map { it.parentFile }.toSet()

idea {
    module {
        sourceDirs = sourceDirs + pipelineSourceDirectories + setOf(file("config"), file("ide"))
    }
}

codenarc {
    toolVersion = libs.versions.codenarc.get()
    configFile = file("config/codenarc.groovy")
    isIgnoreFailures = false
}

val formatCheck = tasks.register("formatCheck") {
    group = "verification"
    description = "Check whitespace and final newlines."
    val textFiles = fileTree(projectDir) {
        include("**/*.md", "**/*.groovy", "**/*.gdsl", "**/*.kts", "**/*.toml",
                "**/*.yml", "**/*.yaml", "**/*.properties")
        exclude(".git/**", ".gradle/**", "build/**", "gradle/wrapper/**", "ide/controller.gdsl")
    }
    inputs.files(textFiles)
    doLast {
        val violations = mutableListOf<String>()
        textFiles.forEach { source ->
            val content = source.readText()
            val path = source.relativeTo(projectDir).path
            if (content.isNotEmpty() && !content.endsWith("\n")) {
                violations.add("$path: missing final newline")
            }
            content.lineSequence().forEachIndexed { index, line ->
                if (line.contains('\t') || line.endsWith(' ') || line.endsWith('\r')) {
                    violations.add("$path:${index + 1}: tab, trailing whitespace or CRLF")
                }
            }
        }
        check(violations.isEmpty()) { violations.joinToString("\n") }
    }
}

tasks.check {
    dependsOn(formatCheck)
}

tasks.jar {
    enabled = false
}
