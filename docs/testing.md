# Testing

## Current foundation and utilities

Run `./gradlew foundationCheck`. It compiles library source, tests and each nested
consumer Jenkinsfile independently; runs unit tests, CodeNarc and formatting;
and verifies the resolved compilation classpath.

Unit tests use JUnit Jupiter. Pipeline-facing tests extend
`com.lesfurets.jenkins.unit.BasePipelineTest` and use `*UTest.groovy` names.
Pure model tests do not need a Pipeline base class. Test-only fixtures live under
`test/fixtures/`, outside the library resources and production source sets.

The foundation tests exercise script loading, resource mocking and duplicate
Jenkinsfile basenames in separate directories. An annotation fixture also checks
Jenkins's implicit `Library` import during compilation. They do not claim Jenkins CPS,
sandbox, plugin or restart compatibility.

Utility tests cover logger thresholds, context and color isolation, multiline
formatting, native error delegation, checkout defaults/tuning, revision selection,
validation before Pipeline operations, metadata mapping and unchanged exception
propagation. Serialization round trips check immutable data objects without
claiming real Jenkins restart coverage.

`UtilitiesPipelineUTest` loads the local library through JenkinsPipelineUnit's
project retriever and runs a CPS-transformed consumer with typed imports and
named operations. Its `vars` scripts are compiled through the test loader;
packaged library classes use Gradle's compiled output. Duplicate class preloading
is disabled so enum and options arguments keep one class identity. Pipeline
steps, color and timestamp wrappers remain mocks.

## Real Jenkins integration suite

`integrationTest` is a separate JUnit Jupiter suite required by `check` and `build`.
It requires Docker, Git, curl and JDK 21. Missing prerequisites fail the build.
Use `-PdockerContext=colima-jenkins-platform-library` for the isolated macOS profile.
Every invocation runs the suite; integration results are never restored from the
build cache or considered up to date.

One disposable environment serves the suite: a digest-pinned Jenkins controller
with zero executors, a separate inbound agent and an authenticated, read-only Git
fixture server. JCasC registers an **untrusted** library retrieved using `@Library`;
consumer scripts run in the sandbox without added script approvals. The runner
verifies every active plugin version against the complete SHA-256 runtime lock.

The candidate contains the current working-tree `src`, `vars` and `resources`,
including unstaged changes. Synthetic bare histories are built in temporary
directories with Git fast-import; the user's repository and index are untouched.

Coverage includes:

- Full history, workspace reuse, opt-in cleaning/pruning, narrow shallow fetch,
  custom remote names, tags, exact commits, branch names and reference repositories.
- Credential success/failure, logger filtering and multiline prefixes, rendered
  ANSI output, timestamps, log-only errors, native failure and `catchError` handling.
- Helper, logger, options and result objects retained across a controller restart.
- Native cancellation and rejection of a forbidden controller API in the sandbox.

Reports live in `build/reports/tests/integrationTest` and `build/reports/jenkins`.
The latter records the candidate revision, plugin manifest and redacted container
and job logs. Generated credentials are temporary. Cleanup removes only this run's
containers, network and Jenkins home volume; verified downloads and images remain.
An abruptly killed local runner may leave labelled resources. Inspect resources
labelled `jenkins-platform-library.test=true` before removing them, and do not
remove fixtures while another test run is active.

The fixture is intentionally small: it proves checkout semantics, not throughput
or timeout suitability for a multi-gigabyte production repository. SSH transport,
other plugin combinations and provider-specific behavior are not covered.

## Continuous integration

The `Verify` workflow runs `clean build` and a separate history/working-tree secret
scan for pull requests and pushes to `main`. Actions are pinned, permissions are
read-only, and reports are retained for seven days. PRs cannot write the Gradle
cache. The workflow also validates the Wrapper and cleans interrupted fixtures.

After the first successful GitHub run, configure a `main` ruleset requiring PRs
and the checks **Build and Jenkins integration** and **Secret scan**, with no
force pushes or branch deletion. This repository supplies the workflow; remote
ruleset configuration and enforcement require a separate maintainer action.

## Dependency maintenance

Use `./gradlew resolveDependencies --write-locks` after intentional catalog
changes. Generate verification updates with
`./gradlew resolveDependencies --write-verification-metadata sha256`, then review
every new coordinate and checksum against trusted publisher metadata. Generated
checksums record downloaded bytes; generation itself is not a trust decision.

Normal verification uses strict locking and checksum verification. Do not use
dynamic versions, snapshots, disabled verification or broad dependency exclusions
as fixes. `verifyDependencies` rejects archive artifacts and mixed Groovy
versions on the library compilation classpath and verifies plugin transitives.
