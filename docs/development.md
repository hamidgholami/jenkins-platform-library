# Local development

## Prerequisites

- JDK 21; Gradle is provided by the Wrapper.
- Git for source control; agents do not modify the index or create commits.
- Docker CLI and Colima for later container integration tests on macOS.

On Apple Silicon macOS:

```sh
brew install openjdk@21 docker colima
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
./gradlew foundationCheck
```

These environment settings apply to the current shell. No system Java symlink
or shell-profile modification is required. Linux users can supply any compatible
JDK 21 installation through `JAVA_HOME`.

## Isolated container runtime

```sh
colima start --profile jenkins-platform-library --cpu 2 --memory 4 --disk 30 --runtime docker --activate=false
docker --context colima-jenkins-platform-library info
```

The profile uses 2 CPUs, 4 GiB of memory and a 30 GiB virtual disk. This initial
allocation verifies tooling; revisit capacity when implementing Jenkins tests.
Use the named context explicitly rather than changing the user's default.

```sh
colima stop --profile jenkins-platform-library
```

No Docker socket will be mounted into Jenkins containers. The host-side test
runner will orchestrate disposable test resources.

## Useful tasks

| Command | Purpose |
|---|---|
| `./gradlew foundationCheck` | All implemented foundation checks |
| `./gradlew test` | JenkinsPipelineUnit/JUnit Jupiter tests |
| `./gradlew compilePipelines` | Compile each Jenkinsfile in isolation |
| `./gradlew formatCheck` | Check formatting without rewriting files |
| `./gradlew verifyDependencies` | Validate library classpath invariants |
| `./gradlew integrationTest` | Explicit deferred-suite failure for now |

Do not add Gradle subprojects for new consumer Jenkinsfiles. Put them under
`pipelines/<category>/<job>/Jenkinsfile.groovy`; nested directories are supported.
