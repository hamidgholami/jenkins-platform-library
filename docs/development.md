# Local development

## Prerequisites

- JDK 21; Gradle is provided by the Wrapper.
- Git for source control; agents do not modify the index or create commits.
- Docker CLI and a running Docker engine; Colima supplies it on macOS.
- `curl` for verified integration-plugin downloads. Network access is needed on
  first use for Gradle dependencies, the pinned image and Jenkins plugins.

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

Colima is only needed for the Jenkins integration suite. Keep it stopped
while working on compilation and unit tests.

The repository owns the Apple Silicon macOS configuration in
[`config/colima/colima.yaml`](../config/colima/colima.yaml). It targets Colima
0.10.3 and macOS 13 or later, using the native virtualization framework.
Colima reads a named profile's configuration from its own directory and does not
accept a `--config` file argument. See the
[Colima configuration documentation](https://colima.run/docs/configuration/).

From the repository root, install the configuration once, with the profile
stopped. Repeat this copy after intentional repository configuration updates;
it replaces any local edits to that profile's configuration:

```sh
mkdir -p "${COLIMA_HOME:-$HOME/.colima}/jenkins-platform-library"
cp config/colima/colima.yaml "${COLIMA_HOME:-$HOME/.colima}/jenkins-platform-library/colima.yaml"
```

Start it when container testing is needed:

```sh
colima start --profile jenkins-platform-library
docker --context colima-jenkins-platform-library info
```

The profile uses 2 CPUs, 4 GiB of memory and a 30 GiB virtual disk. This initial
allocation runs one controller, one agent and one small Git fixture server.
`autoActivate: false` preserves the user's default context. Use the named context
explicitly. Keep VM state outside the repository; copy the configuration rather
than symlinking it because Colima rewrites its configuration on startup.

```sh
colima stop --profile jenkins-platform-library
```

No Docker socket is mounted into Jenkins containers. The host-side test runner
orchestrates disposable resources and removes containers, volumes and networks
after the suite. Cached images and verified plugin downloads remain for reuse.

## Useful tasks

| Command | Purpose |
|---|---|
| `./gradlew foundationCheck` | Fast compilation, unit and static checks; no Docker |
| `./gradlew test` | JenkinsPipelineUnit/JUnit Jupiter tests |
| `./gradlew compilePipelines` | Compile each Jenkinsfile in isolation |
| `./gradlew formatCheck` | Check formatting without rewriting files |
| `./gradlew verifyDependencies` | Validate library classpath invariants |
| `./gradlew integrationTest` | Real Jenkins suite using the active Docker context |
| `./gradlew build -PdockerContext=colima-jenkins-platform-library` | All checks using the isolated macOS context |
| `./gradlew clean build` | Full verification from cleaned build outputs |

The `dockerContext` property takes precedence over `DOCKER_CONTEXT`; otherwise
Docker uses its current context. Do not change the default context for this project.
Use normal incremental builds while developing; reserve `clean` for release/CI
verification or investigating stale outputs. See [build performance](performance.md).

Do not add Gradle subprojects for new consumer Jenkinsfiles. Put them under
`pipelines/<category>/<job>/Jenkinsfile.groovy`; nested directories are supported.
