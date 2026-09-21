/**
 * Copyright 2026 Hamid Gholami
 * SPDX-License-Identifier: Apache-2.0
 * */

package io.github.hamidgholami.jenkins.platform.testing

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.MessageDigest
import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathFactory
import org.w3c.dom.Document

final class JenkinsEnvironment implements AutoCloseable {

    private final File project = new File(System.getProperty('jpl.projectDir'))
    private final File resources = new File(project, 'test/integration-resources')
    private final File temporary = Files.createTempDirectory('jpl-integration-').toFile()
    private final File reports = new File(project, 'build/reports/jenkins')
    private final CommandRunner commands = new CommandRunner(temporary)
    private final String context = System.getProperty('jpl.dockerContext', '')
    private final String name = 'jpl-test-' + UUID.randomUUID().toString().substring(0, 8)
    private final String password = UUID.randomUUID().toString()
    private final String gitPassword = UUID.randomUUID().toString()
    private final String invalidPassword = UUID.randomUUID().toString()
    private final List<String> secrets = [password, gitPassword, invalidPassword]
    private final List<String> containers = []
    private final List<String> jobs = []
    private final HttpClient http = HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .connectTimeout(Duration.ofSeconds(5)).build()
    private final Map<String, String> pluginVersions = [:]
    private String baseUrl
    private String image
    private boolean networkCreated
    private boolean volumeCreated
    GitFixtures fixtures

    void start() {
        reports.deleteDir()
        reports.mkdirs()
        docker(['info', '--format', '{{.ServerVersion}}'])
        final Properties versions = new Properties()
        new File(resources, 'jenkins/images.properties').withInputStream { final InputStream input -> versions.load(input) }
        image = versions.getProperty('jenkins')
        if (image == null || !image.contains('@sha256:')) {
            throw new IllegalStateException('The integration image must be pinned by digest')
        }
        prepareImage()
        fixtures = new GitFixtures(commands, new File(temporary, 'fixtures'))
        fixtures.create(project)
        new File(reports, 'candidate.txt').setText('library=' + fixtures.libraryCommit + '\nsource=' + fixtures.sourceCommit + '\n', 'UTF-8')
        docker(['network', 'create', '--label', 'jenkins-platform-library.test=true', name])
        networkCreated = true
        docker(['volume', 'create', '--label', 'jenkins-platform-library.test=true', name + '-home'])
        volumeCreated = true
        final File environment = new File(temporary, 'secrets.env')
        environment.setText('TEST_ADMIN_PASSWORD=' + password + '\nTEST_GIT_PASSWORD=' + gitPassword +
                '\nTEST_INVALID_PASSWORD=' + invalidPassword + '\n', 'UTF-8')
        environment.setReadable(false, false)
        environment.setReadable(true, true)
        create('git', ['--memory', '256m', '--env-file', environment.absolutePath, '--entrypoint', 'java', image,
                       '-Xmx96m', '/fixture/GitServer.java'])
        docker(['cp', new File(resources, 'git-server').absolutePath + '/.', name + '-git:/fixture'])
        docker(['cp', fixturesDirectory().absolutePath + '/.', name + '-git:/fixtures'])
        docker(['start', name + '-git'])
        create('controller', ['--memory', '1536m', '--env-file', environment.absolutePath,
                              '--publish', '127.0.0.1::8080', '--volume', name + '-home:/var/jenkins_home',
                              'jpl-integration-controller:' + imageKey()])
        docker(['cp', new File(resources, 'jenkins/jenkins.yaml').absolutePath, name + '-controller:/tmp/jenkins.yaml'])
        docker(['start', name + '-controller'])
        baseUrl = 'http://' + docker(['port', name + '-controller', '8080/tcp']).readLines()[0]
        awaitReady()
        final String jnlp = request('GET', '/computer/integration-agent/jenkins-agent.jnlp')
        final String agentSecret = xml(jnlp, '//application-desc/argument[1]')
        if (!agentSecret.matches('[0-9a-f]{64}')) {
            throw new IllegalStateException('Jenkins did not return an agent connection secret')
        }
        secrets.add(agentSecret)
        final File secretFile = new File(temporary, 'agent-secret')
        secretFile.setText(agentSecret, 'UTF-8')
        create('agent', ['--memory', '768m', '--entrypoint', '/bin/sh', image, '-c',
                        'curl -fsS http://controller:8080/jnlpJars/agent.jar -o /tmp/agent.jar && ' +
                        'exec java -Xmx384m -jar /tmp/agent.jar -url http://controller:8080/ ' +
                        '-name integration-agent -secret @/tmp/agent-secret -webSocket -workDir /tmp/agent'])
        docker(['cp', secretFile.absolutePath, name + '-agent:/tmp/agent-secret'])
        docker(['start', name + '-agent'])
        awaitAgent()
        verifyPlugins()
    }

    String run(final String job, final String script) {
        createJob(job, script)
        request('POST', '/job/' + job + '/build')
        return awaitResult(job)
    }

    void createJob(final String job, final String script) {
        final String xml = '<flow-definition><description>Disposable library test</description>' +
                '<definition class="org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition">' +
                '<script>' + escape(script) + '</script><sandbox>true</sandbox></definition>' +
                '<disabled>false</disabled></flow-definition>'
        request('POST', '/createItem?name=' + job, xml, 'application/xml')
        jobs.add(job)
    }

    String pipeline(final String filename) {
        return new File(resources, 'pipelines/' + filename).getText('UTF-8')
                .replace('@@SOURCE_COMMIT@@', fixtures.sourceCommit)
                .replace('@@FIRST_COMMIT@@', fixtures.firstCommit)
    }

    String awaitResult(final String job) {
        String result = ''
        waitUntil('build ' + job, 180) { ->
            try {
                result = xml(request('GET', '/job/' + job + '/1/api/xml'), '/workflowRun/result')
                return !result.isEmpty()
            } catch (final IOException ignored) {
                return false
            }
        }
        final String console = request('GET', '/job/' + job + '/1/consoleText')
        new File(reports, job + '.log').setText(redact(console), 'UTF-8')
        return result
    }

    String console(final String job) {
        return request('GET', '/job/' + job + '/1/consoleText')
    }

    void awaitInput(final String job) {
        waitUntil('input in ' + job, 120) { ->
            try {
                final String result = xml(request('GET', '/job/' + job + '/1/api/xml'), '/workflowRun/result')
                if (!result.isEmpty()) {
                    throw new IllegalStateException(job + ' finished before input: ' + result)
                }
                return console(job).contains('INTEGRATION_WAITING') &&
                        request('GET', '/job/' + job + '/1/input/Resume/').contains('Proceed')
            } catch (final IOException ignored) {
                return false
            }
        }
    }

    void restart() {
        docker(['restart', '--time', '30', name + '-controller'])
        baseUrl = 'http://' + docker(['port', name + '-controller', '8080/tcp']).readLines()[0]
        awaitReady()
        awaitAgent()
    }

    String request(final String method, final String path, final String body = '', final String contentType = 'text/plain') {
        final HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(20))
                .header('Authorization', 'Basic ' + Base64.encoder.encodeToString(('test-admin:' + password).getBytes(StandardCharsets.UTF_8)))
        if (method == 'POST') {
            final String crumb = request('GET', '/crumbIssuer/api/xml')
            builder.header(xml(crumb, '/defaultCrumbIssuer/crumbRequestField'), xml(crumb, '/defaultCrumbIssuer/crumb'))
        }
        final HttpResponse<String> response = http.send(builder.header('Content-Type', contentType)
                .method(method, HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() < 200 || response.statusCode() >= 400) {
            throw new IOException('Jenkins HTTP ' + response.statusCode() + ' for ' + path + '\n' + redact(response.body()).take(1200))
        }
        final String core = response.headers().firstValue('X-Jenkins').orElse('')
        if (!core.isEmpty() && core != System.getProperty('jpl.jenkinsVersion')) {
            throw new IllegalStateException('Jenkins runtime differs from the Gradle catalog: ' + core)
        }
        return response.body()
    }

    static String xml(final String text, final String expression) {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance()
        factory.setFeature('http://apache.org/xml/features/disallow-doctype-decl', true)
        final Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)))
        return XPathFactory.newInstance().newXPath().evaluate(expression, document)
    }

    static void waitUntil(final String description, final int seconds, final Closure<Boolean> condition) {
        final long deadline = System.nanoTime() + Duration.ofSeconds(seconds).toNanos()
        while (System.nanoTime() < deadline) {
            if (condition()) {
                return
            }
            Thread.sleep(1000)
        }
        throw new IllegalStateException('Timed out waiting for ' + description)
    }

    private void awaitReady() {
        waitUntil('Jenkins readiness', 180) { ->
            try {
                request('GET', '/api/xml')
                return true
            } catch (final IOException ignored) {
                return false
            }
        }
    }

    private void awaitAgent() {
        waitUntil('agent connection', 120) { ->
            return xml(request('GET', '/computer/integration-agent/api/xml'), '/slaveComputer/offline') == 'false'
        }
    }

    private void verifyPlugins() {
        final String installed = request('GET', '/pluginManager/api/xml?depth=1')
        new File(reports, 'plugins.xml').setText(installed, 'UTF-8')
        for (final Map.Entry<String, String> plugin : pluginVersions.entrySet()) {
            final String query = '/*/plugin[shortName="' + plugin.key + '"]'
            if (xml(installed, query + '/version') != plugin.value || xml(installed, query + '/active') != 'true') {
                throw new IllegalStateException('Pinned plugin is missing, inactive or changed: ' + plugin.key)
            }
        }
        if (xml(installed, 'count(/*/plugin)') != pluginVersions.size().toString()) {
            throw new IllegalStateException('Installed plugin set differs from the runtime lock')
        }
        if (pluginVersions.get('pipeline-groovy-lib') != System.getProperty('jpl.libraryPluginVersion')) {
            throw new IllegalStateException('Shared-library plugin runtime differs from the Gradle catalog')
        }
    }

    private void prepareImage() {
        final File cache = new File(project, '.gradle/jenkins-integration/plugins')
        cache.mkdirs()
        final File contextDirectory = new File(temporary, 'image')
        final File plugins = new File(contextDirectory, 'plugins')
        plugins.mkdirs()
        final ExecutorService downloads = Executors.newFixedThreadPool(4)
        final List<Future<Void>> pending = []
        try {
            new File(resources, 'jenkins/plugins.lock').eachLine('UTF-8') { final String line ->
                if (!line.trim().isEmpty() && !line.startsWith('#')) {
                    final String[] fields = line.split(' ')
                    final String id = fields[0]
                    final String version = fields[1]
                    final String checksum = fields[2]
                    pluginVersions.put(id, version)
                    pending.add(downloads.submit({ ->
                        final File artifact = new File(cache, id + '-' + version + '.hpi')
                        if (!artifact.exists() || sha256(artifact.bytes) != checksum) {
                            commands.execute(['curl', '-fsSL', '--retry', '2', '--connect-timeout', '15', '--max-time', '180',
                                              'https://updates.jenkins.io/download/plugins/' + id + '/' + version + '/' + id + '.hpi',
                                              '-o', artifact.absolutePath], new byte[0], 600)
                        }
                        if (sha256(artifact.bytes) != checksum) {
                            throw new IllegalStateException('Plugin checksum mismatch: ' + id)
                        }
                        Files.copy(artifact.toPath(), new File(plugins, id + '.jpi').toPath())
                        return null
                    } as Callable<Void>))
                }
            }
            for (final Future<Void> download : pending) {
                download.get()
            }
        } finally {
            downloads.shutdownNow()
        }
        Files.copy(new File(resources, 'jenkins/Dockerfile').toPath(), new File(contextDirectory, 'Dockerfile').toPath())
        docker(['build', '--build-arg', 'JENKINS_IMAGE=' + image, '--tag', 'jpl-integration-controller:' + imageKey(),
                contextDirectory.absolutePath], 600)
    }

    private String imageKey() {
        return sha256((image + new File(resources, 'jenkins/plugins.lock').getText('UTF-8') +
                new File(resources, 'jenkins/Dockerfile').getText('UTF-8')).getBytes(StandardCharsets.UTF_8)).substring(0, 16)
    }

    private static String sha256(final byte[] content) {
        return MessageDigest.getInstance('SHA-256').digest(content).encodeHex().toString()
    }

    private void create(final String role, final List<String> arguments) {
        final String container = name + '-' + role
        docker(['create', '--name', container, '--label', 'jenkins-platform-library.test=true',
                '--network', name, '--network-alias', role] + arguments)
        containers.add(container)
    }

    private String docker(final List<String> arguments, final int timeout = 120) {
        final List<String> command = ['docker']
        if (!context.isEmpty()) {
            command.addAll(['--context', context])
        }
        return commands.execute(command + arguments, new byte[0], timeout)
    }

    private File fixturesDirectory() {
        return new File(temporary, 'fixtures')
    }

    private String redact(final String text) {
        String redacted = text
        for (final String secret : secrets) {
            redacted = redacted.replace(secret, '[REDACTED]')
        }
        return redacted
    }

    private static String escape(final String text) {
        return text.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
    }

    @Override
    void close() {
        final List<String> failures = []
        for (final String job : jobs) {
            try {
                new File(reports, job + '.log').setText(redact(console(job)), 'UTF-8')
            } catch (final Exception failure) {
                failures.add('Collect build log: ' + job)
            }
        }
        for (final String container : containers.reverse()) {
            try {
                new File(reports, container.substring(name.length() + 1) + '.log')
                        .setText(redact(docker(['logs', container])), 'UTF-8')
            } catch (final Exception failure) {
                failures.add('Collect logs: ' + container)
            }
            try {
                docker(['rm', '--force', '--volumes', container])
            } catch (final Exception failure) {
                failures.add('Remove container: ' + container)
            }
        }
        if (volumeCreated) {
            try {
                docker(['volume', 'rm', name + '-home'])
            } catch (final Exception failure) {
                failures.add('Remove volume: ' + name + '-home')
            }
        }
        if (networkCreated) {
            try {
                docker(['network', 'rm', name])
            } catch (final Exception failure) {
                failures.add('Remove network: ' + name)
            }
        }
        temporary.deleteDir()
        if (!failures.isEmpty()) {
            throw new IllegalStateException(failures.join('\n'))
        }
    }
}
