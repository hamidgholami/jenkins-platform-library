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
