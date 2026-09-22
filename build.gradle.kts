plugins {
    groovy
    codenarc
    idea
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
    }
    create("integrationTest") {
        java.setSrcDirs(emptyList<String>())
        groovy.setSrcDirs(listOf("test/integration"))
        resources.setSrcDirs(listOf("test/integration-resources"))
    }
}

val integrationTestImplementation = configurations.getByName("integrationTestImplementation")
val integrationTestRuntimeOnly = configurations.getByName("integrationTestRuntimeOnly")
val jenkinsPlugins = configurations.create("jenkinsPlugins")
val jenkinsWar = configurations.create("jenkinsWar") {
    isTransitive = false
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

    integrationTestImplementation(libs.groovy)
    integrationTestImplementation(platform(libs.junit.bom))
    integrationTestImplementation(libs.junit.jupiter)
    integrationTestImplementation(platform(libs.jenkins.bom))
    integrationTestImplementation(libs.jenkins.core)
    integrationTestImplementation(libs.jenkins.test.harness)
    integrationTestImplementation("jakarta.servlet:jakarta.servlet-api:5.0.0")
    integrationTestRuntimeOnly(libs.junit.launcher)

    jenkinsPlugins(platform(libs.jenkins.bom))
    jenkinsPlugins("io.jenkins.plugins:pipeline-groovy-lib")
    jenkinsPlugins("org.jenkins-ci.plugins.workflow:workflow-basic-steps")
    jenkinsPlugins("org.jenkins-ci.plugins.workflow:workflow-durable-task-step")
    jenkinsPlugins("org.jenkins-ci.plugins.workflow:workflow-job")
    jenkinsPlugins("org.jenkins-ci.plugins:git")
    jenkinsWar(libs.jenkins.war) {
        artifact { type = "war" }
    }
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

tasks.named<CodeNarc>("codenarcIntegrationTest") {
    compilationClasspath = files()
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

val jenkinsTestDependencies = layout.buildDirectory.dir("jenkins-test-classpath")
val prepareJenkinsTestDependencies = tasks.register<Sync>("prepareJenkinsTestDependencies") {
    group = "verification"
    description = "Prepare Jenkins plugins for the embedded controller."
    from(jenkinsPlugins)
    into(jenkinsTestDependencies.map { it.dir("test-dependencies") })
    include("*.hpi", "*.jpi")
    doLast {
        val directory = destinationDir
        val plugins = directory.listFiles().orEmpty()
                .filter { it.extension == "hpi" || it.extension == "jpi" }
                .sortedBy { it.name }
        directory.resolve("index").writeText(
                plugins.joinToString("\n", postfix = if (plugins.isEmpty()) "" else "\n") {
                    it.nameWithoutExtension
                },
        )
    }
}

val jenkinsPluginJars = layout.buildDirectory.dir("jenkins-test-plugin-jars")
val extractJenkinsPluginJars = tasks.register<Sync>("extractJenkinsPluginJars") {
    group = "verification"
    description = "Extract plugin JARs needed by the Jenkins Test Harness classloader."
    from({
        jenkinsPlugins.filter { it.extension == "hpi" || it.extension == "jpi" }
                .map { zipTree(it) }
    }) {
        include("WEB-INF/lib/*.jar")
        eachFile { path = name }
        includeEmptyDirs = false
    }
    into(jenkinsPluginJars)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

sourceSets["integrationTest"].apply {
    val pluginJars = files(fileTree(jenkinsPluginJars) { include("*.jar") })
            .builtBy(extractJenkinsPluginJars)
    compileClasspath += pluginJars
    runtimeClasspath += pluginJars
}

val integrationTest = tasks.register<Test>("integrationTest") {
    group = "verification"
    description = "Run focused tests in an embedded Jenkins controller."
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath + files(jenkinsTestDependencies)
    dependsOn(prepareJenkinsTestDependencies, extractJenkinsPluginJars)
    shouldRunAfter(tasks.test)
    maxHeapSize = "2g"
    doFirst {
        systemProperty("buildDirectory", layout.buildDirectory.get().asFile.absolutePath)
        systemProperty("jth.jenkins-war.path", jenkinsWar.singleFile.absolutePath)
        systemProperty("jenkins.library.root", projectDir.absolutePath)
    }
    jvmArgs(
            "-XX:+ExitOnOutOfMemoryError",
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
            "--add-opens=java.base/java.text=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED",
            "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
            "--add-opens=java.desktop/java.awt=ALL-UNNAMED",
    )
}

tasks.jar {
    enabled = false
}
