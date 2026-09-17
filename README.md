# Jenkins Platform Library

An original Jenkins shared library with typed Groovy code, reproducible Gradle
checks and explicit runtime compatibility targets.

**Status: foundation only.** Git checkout and logging helpers are not implemented
yet. No released version or production runtime compatibility is claimed.

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
`check` and `build` intentionally require `integrationTest`, which currently
fails with an explicit deferred-milestone message. `foundationCheck` validates
the work available now without presenting an empty integration suite as passing.

## Layout

| Directory | Responsibility |
|---|---|
| `src/` | Packaged Groovy classes loaded by Jenkins |
| `vars/` | Stateless public Pipeline entry points |
| `resources/` | Runtime library resources |
| `test/unit/` | JenkinsPipelineUnit and JUnit Jupiter tests |
| `test/fixtures/` | Test-only scripts and resources |
| `test/integration/` | Future real Jenkins consumer suite |
| `pipelines/` | Nested Scripted consumer Jenkinsfiles, each compiled separately |
| `config/` | CodeNarc rules |
| `docs/` | Decisions, contribution guidance and roadmap |

There is one Gradle project and one shared library. Distribution will use Git
release tags, not a runtime JAR. Gradle compilation does not install dependencies
on Jenkins; see the [build decision](docs/decisions/0001-build-foundation.md).

## Project information

- [Compatibility targets](docs/compatibility.md)
- [Roadmap](docs/roadmap.md)
- [Contributing](CONTRIBUTING.md), [governance](GOVERNANCE.md), [security](SECURITY.md)
- [Authors](AUTHORS.md), [Apache-2.0 license](LICENSE), [notice](NOTICE)
