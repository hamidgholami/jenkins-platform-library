import java.nio.file.Files
import java.nio.file.Path

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.jenkinsci.plugins.workflow.libs.GlobalLibraries
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.jvnet.hudson.test.JenkinsRule
import org.jvnet.hudson.test.junit.jupiter.WithJenkins

import static org.junit.jupiter.api.Assertions.assertTrue

@WithJenkins
class JenkinsRuntimeITest {

    private static final String LIBRARY_NAME = 'jenkins-platform-library'

    @TempDir
    Path temporaryDirectory

    private Process gitDaemon

    @AfterEach
    void stopGitDaemon() {
        if (gitDaemon != null) {
            gitDaemon.destroy()
            gitDaemon.waitFor()
        }
    }

    @Test
    void runsLoggingFromASandboxedPipeline(final JenkinsRule jenkins) {
        registerLibrary()
        final WorkflowRun run = runPipeline(jenkins, 'logging', '''
                @Library('jenkins-platform-library') _

                log.forContext('Runtime').info('executed by Jenkins')
        '''.stripIndent())

        jenkins.assertLogContains('[INFO] [Runtime] executed by Jenkins', run)
    }

    @Test
    void checksOutWithTheRealGitPlugin(final JenkinsRule jenkins) {
        registerLibrary()
        final Path repository = createRepository()
        final String repositoryUrl = serveRepository(repository)
        final WorkflowRun run = runPipeline(jenkins, 'git-checkout', """
                import io.github.hamidgholami.jenkins.platform.git.GitCheckoutOptions
                import io.github.hamidgholami.jenkins.platform.git.GitCheckoutResult

                @Library('${LIBRARY_NAME}') _

                node {
                    dir('source') {
                        final GitCheckoutResult result = gitUtils.checkout(GitCheckoutOptions.builder('${repositoryUrl}')
                                .branch('main').build())
                        echo "RESULT|\${result.commit}|\${result.branch}|\${result.authorName}|" +
                                "\${result.authorEmail}|\${result.committerName}|\${result.committerEmail}"
                    }
                }
        """.stripIndent())

        final String log = JenkinsRule.getLog(run)
        assertTrue(log.contains('RESULT|'))
        assertTrue(log.contains('|origin/main|Ada Author|ada@example.org|Casey Committer|casey@example.org'))
    }

    private static WorkflowRun runPipeline(final JenkinsRule jenkins, final String name, final String script) {
        final WorkflowJob job = jenkins.createProject(WorkflowJob, name)
        job.definition = new CpsFlowDefinition(script, true)
        final WorkflowRun run = jenkins.buildAndAssertSuccess(job)
        final File consoleLogDirectory = new File(System.getProperty('jenkins.console.logs'))
        Files.writeString(consoleLogDirectory.toPath().resolve(name + '.log'), JenkinsRule.getLog(run))
        return run
    }

    private static void registerLibrary() {
        final File sourceRoot = new File(System.getProperty('jenkins.library.root'))
        final LibraryConfiguration library = new LibraryConfiguration(
                LIBRARY_NAME, new LocalLibraryRetriever(sourceRoot))
        library.defaultVersion = 'working-tree'
        library.implicit = false
        GlobalLibraries.get().libraries = [library]
    }

    private Path createRepository() {
        final Path working = temporaryDirectory.resolve('working')
        final Path bare = temporaryDirectory.resolve('source.git')
        Files.createDirectories(working)
        runGit(temporaryDirectory, [:], 'init', '--bare', bare.toString())
        runGit(working, [:], 'init', '--initial-branch=main')
        Files.writeString(working.resolve('README.md'), 'integration test\n')
        runGit(working, [:], 'add', 'README.md')
        runGit(working, [
                GIT_AUTHOR_NAME: 'Ada Author',
                GIT_AUTHOR_EMAIL: 'ada@example.org',
                GIT_COMMITTER_NAME: 'Casey Committer',
                GIT_COMMITTER_EMAIL: 'casey@example.org',
        ], 'commit', '-m', 'Create fixture')
        runGit(working, [:], 'remote', 'add', 'origin', bare.toUri().toString())
        runGit(working, [:], 'push', 'origin', 'main')
        return bare
    }

    private String serveRepository(final Path repository) {
        final int port
        new ServerSocket(0).withCloseable { final ServerSocket socket ->
            port = socket.localPort
        }
        gitDaemon = new ProcessBuilder(
                'git', 'daemon', '--reuseaddr', '--export-all',
                '--base-path=' + temporaryDirectory, '--listen=127.0.0.1',
                '--port=' + port, temporaryDirectory.toString(),
        ).redirectErrorStream(true).start()
        waitForPort(port)
        return 'git://127.0.0.1:' + port + '/' + repository.fileName
    }

    private void waitForPort(final int port) {
        for (int attempt = 0; attempt < 50; attempt++) {
            if (!gitDaemon.alive) {
                throw new IllegalStateException('git daemon stopped: ' + gitDaemon.inputStream.getText('UTF-8'))
            }
            try {
                new Socket('127.0.0.1', port).withCloseable { }
                return
            } catch (final IOException ignored) {
                Thread.sleep(100)
            }
        }
        throw new IllegalStateException('git daemon did not start')
    }

    private static void runGit(final Path directory, final Map<String, String> environment,
                               final String... arguments) {
        final List<String> command = ['git']
        command.addAll(arguments)
        final ProcessBuilder builder = new ProcessBuilder(command).directory(directory.toFile())
        builder.environment().putAll(environment)
        builder.redirectErrorStream(true)
        final Process process = builder.start()
        final String output = process.inputStream.getText('UTF-8')
        final int exitCode = process.waitFor()
        if (exitCode != 0) {
            throw new IllegalStateException(command.join(' ') + ' failed: ' + output)
        }
    }
}
