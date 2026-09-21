# Jenkins Platform Library

A Jenkins Shared Library for practical, reusable CI/CD building blocks. It
currently provides:

- contextual Pipeline logging with levels and optional ANSI colors;
- configurable Git checkout through Jenkins's Git plugin.

The current scope is deliberately focused, while the structure is intended to
support additional utilities and example pipelines when they solve concrete
problems. The repository demonstrates a conventional shared-library layout,
packaged Groovy classes, thin global variables, JenkinsPipelineUnit tests, and a
consumer Jenkinsfile.

## Usage

Configure this repository as a Jenkins Shared Library named
`jenkins-platform-library`, then load it from a Pipeline:

```groovy
@Library('jenkins-platform-library')
import io.github.hamidgholami.jenkins.platform.git.GitCheckoutOptions
import io.github.hamidgholami.jenkins.platform.git.GitCheckoutResult
import io.github.hamidgholami.jenkins.platform.logging.PipelineLogger

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

See [logging](docs/logging.md), [Git checkout](docs/git-checkout.md), and the
[complete example](pipelines/examples/checkout/Jenkinsfile.groovy).

## Project structure

| Path | Purpose |
|---|---|
| `src/` | Packaged Groovy implementation |
| `vars/` | Public Jenkins Pipeline entry points |
| `test/unit/` | Unit tests |
| `pipelines/` | Example consumers |

## Development

Use JDK 21 and the checked-in Gradle Wrapper:

```sh
./gradlew check
```

The build compiles the library, runs CodeNarc and formatting checks, and executes
the unit suite. It does not require Docker or a local Jenkins controller. The
[testing strategy](docs/testing-strategy.md) records the planned path toward
focused Jenkins Test Harness coverage and later validation on a test controller.

This project targets Jenkins environments compatible with Groovy 2.4 and the
Pipeline and Git plugins. Consumers should validate the library against their
own Jenkins and plugin versions before production use.

## License

Apache License 2.0. See [LICENSE](LICENSE).
