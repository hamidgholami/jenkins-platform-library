# Jenkins Platform Library

An original Jenkins shared library with typed Groovy code, reproducible Gradle
checks and explicit runtime compatibility targets.

**Status: stage 5 implementation is in progress.** Git/log utilities, real Jenkins
fixtures and the CI workflow are implemented; final acceptance and performance
tuning remain open. See the [session checkpoint](docs/session-checkpoint.md).
No released version or broad production compatibility is claimed.

- [Logging](docs/logging.md): `log.forContext(...)`, five levels and native failure handling.
- [Git checkout](docs/git-checkout.md): `gitUtils.checkout(...)`, typed options and configurable clone/fetch behavior.

## Development

Use JDK 21 and the checked-in Gradle Wrapper:

```sh
./gradlew foundationCheck
```

On Apple Silicon macOS, after `brew install openjdk@21 docker colima`:

```sh
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew foundationCheck
```

See [local setup](docs/development.md) and [testing](docs/testing.md).
`check` and `build` also require Docker for the real Jenkins suite. Use
`./gradlew build` for full verification, or on the isolated macOS profile:

```sh
./gradlew build -PdockerContext=colima-jenkins-platform-library
```

## Layout

| Directory | Responsibility |
|---|---|
| `src/` | Packaged Groovy classes loaded by Jenkins |
| `vars/` | Stateless public Pipeline entry points |
| `resources/` | Runtime library resources |
| `test/unit/` | JenkinsPipelineUnit and JUnit Jupiter tests |
| `test/fixtures/` | Test-only scripts and resources |
| `test/integration/` | Real Jenkins lifecycle and consumer tests |
| `test/integration-resources/` | Pinned runtime, JCasC and disposable Git/Pipeline fixtures |
| `pipelines/` | Nested Scripted consumer Jenkinsfiles, each compiled separately |
| `config/` | CodeNarc rules, compiler configuration and local Colima settings |
| `docs/` | Decisions, contribution guidance and roadmap |

There is one Gradle project and one shared library. Distribution will use Git
release tags, not a runtime JAR. Gradle compilation does not install dependencies
on Jenkins; see the [build decision](docs/decisions/0001-build-foundation.md).

## Project information

- [Compatibility targets](docs/compatibility.md)
- [Roadmap](docs/roadmap.md)
- [Contributing](CONTRIBUTING.md), [governance](GOVERNANCE.md), [security](SECURITY.md)
- [Authors](AUTHORS.md), [Apache-2.0 license](LICENSE), [notice](NOTICE)
