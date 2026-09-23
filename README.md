# Jenkins Platform Library

A Jenkins Shared Library that combines practical CI/CD building blocks with
fast unit tests and focused execution inside an embedded Jenkins controller. It
currently provides:

- contextual Pipeline logging with levels and optional ANSI colors;
- configurable Git checkout through Jenkins's Git plugin.

Shared-library code can pass mock-based tests and still fail when Jenkins applies
CPS transformation, sandbox rules, dynamic library loading, or real plugin
binding. This project addresses that gap with two deliberate test layers:

- JenkinsPipelineUnit tests provide fast feedback for behavior and edge cases;
- Jenkins Test Harness runs representative library calls in a real Jenkins
  runtime, including a sandboxed Pipeline and the actual Git plugin.

The current scope is deliberately focused, while the structure can support
additional utilities and example pipelines when they solve concrete problems.

## Usage

Configure this repository as a Jenkins Shared Library named
`jenkins-platform-library`, then load it from a Pipeline:

```groovy
import io.github.hamidgholami.jenkins.platform.git.GitCheckoutOptions
import io.github.hamidgholami.jenkins.platform.git.GitCheckoutResult
import io.github.hamidgholami.jenkins.platform.logging.PipelineLogger
import org.jenkinsci.plugins.workflow.libs.Library

@Library('jenkins-platform-library') _

final PipelineLogger logger = log.forContext('Source')
final GitCheckoutOptions options = GitCheckoutOptions
        .builder('https://github.com/example/service.git')
        .branch('main')
        .credentialsId('source-reader')
        .build()

node('linux') {
    final GitCheckoutResult result = gitUtils.checkout(options, logger)
    logger.info('Checked out ' + result.commit)
}
```

See [logging](docs/logging.md), [Git checkout](docs/git-checkout.md), the
[pipeline conventions](docs/pipeline-conventions.md), and the
[complete example](pipelines/examples/checkout/Jenkinsfile.groovy).

## Project structure

| Path | Purpose |
|---|---|
| `src/` | Packaged Groovy implementation |
| `vars/` | Public Jenkins Pipeline entry points |
| `test/unit/` | Unit tests |
| `test/integration/` | Focused embedded-Jenkins tests |
| `pipelines/` | Example pipeline projects; one `Jenkinsfile.groovy` per leaf directory |
| `ide/` | IntelliJ Jenkins Pipeline DSL support |

## Development

Use JDK 21 and the checked-in Gradle Wrapper:

```sh
./gradlew check
./gradlew integrationTest
./gradlew integrationTest -PshowIntegrationLogs=true --console=plain
```

The build compiles the library, runs CodeNarc and formatting checks, and executes
the unit suite. The separate `integrationTest` task starts an embedded Jenkins
controller and runs focused sandbox and Git-plugin checks. Neither task requires
Docker or an externally managed Jenkins controller. The Git integration test
requires the standard `git` command with `git daemon` support. See the
[testing strategy](docs/testing-strategy.md) for the boundaries of each layer.

The final command prints only the Pipeline console output for each integration
scenario. Jenkins console logs are also saved under
`build/reports/jenkins-console/`. In IntelliJ IDEA, create a Gradle run
configuration for the `integrationTest` task and add
`-PshowIntegrationLogs=true --console=plain` as arguments. Keep **Run tests
using** set to **Gradle** so the required Jenkins Test Harness setup runs.

This project targets Jenkins environments compatible with Groovy 2.4 and the
Pipeline and Git plugins. Consumers should validate the library against their
own Jenkins and plugin versions before production use.

## Versioning

Releases use semantic Git tags such as `v0.1.0`; the `main` branch represents
ongoing development. Jenkins consumers should pin a reviewed release with
`@Library('jenkins-platform-library@v0.1.0') _`. The project is loaded from SCM
and does not publish a Maven artifact.

## License

Apache License 2.0. See [LICENSE](LICENSE).
