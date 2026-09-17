# Testing

## Current foundation

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

## Deferred integration suite

`integrationTest` is registered as a separate JUnit Jupiter suite and required by
`check`. Until the real suite is implemented, it fails with a clear explanation.
Consequently `check` and `build` are not release gates that can pass yet. Do not
replace that failure with an empty passing test or silently skip it.

The future suite will start pinned Jenkins/JCasC containers and a separate agent,
load the candidate library through Git, and verify checkout, logging and restart
behavior. Docker availability is a prerequisite for that suite, not unit tests.

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
