# Testing strategy

## Purpose

The repository uses multiple test layers because no single Jenkins testing tool
provides both fast feedback and complete runtime confidence.

The unit suite remains the fast required test layer. A separate, focused
real-Jenkins integration suite covers behavior that mocks cannot establish.

## Test layers

### Unit tests

JenkinsPipelineUnit and JUnit cover:

- library logic and validation;
- arguments passed to Jenkins steps;
- logging output;
- failure and interruption propagation.

These tests run through `./gradlew check` and remain the fast pull-request gate.
They do not prove CPS execution, sandbox compatibility, Jenkins class loading, or
real plugin behavior.

### Embedded Jenkins integration tests

The optional `integrationTest` suite runs representative consumers with Jenkins
Test Harness.

Run it with `./gradlew integrationTest`. Add
`-PshowIntegrationLogs=true --console=plain` to print the successful Pipeline
console logs without enabling Gradle's verbose `--info` output. The logs are
also retained in `build/reports/jenkins-console/` for inspection.

It contains two scenarios:

1. Load the working-tree library and exercise logging from a sandboxed Pipeline.
2. Exercise `gitUtils.checkout` with the real Git plugin and a temporary bare
   repository exposed on loopback by the standard `git daemon` command.

This layer should verify:

- CPS execution;
- sandbox compatibility and required approvals;
- global-variable and typed-class loading;
- actual Pipeline and Git plugin parameter binding;
- checkout behavior against a real repository.

It must not initially include Docker, Colima, JCasC, a custom Git server,
authenticated SCM, a separate agent, controller restart, or a plugin matrix.

### Test Jenkins environment

A dedicated non-production Jenkins controller may later run candidate library
revisions against representative pipelines, agents, credentials, JCasC, and the
organization's real plugin set. This provides the highest fidelity and belongs
outside the repository's local test infrastructure.

## Tooling decision

Use Jenkins Test Harness directly. A compatibility spike with mkobit plugin
0.12.1, Gradle 9.7.1, Java 21, and Jenkins 2.568.3 failed during Groovy
compilation because the plugin changed a finalized Gradle task property. Keeping
the current Gradle version is more valuable than delegating this small test layer
to a third-party build plugin.

The direct configuration keeps Jenkins core, BOM, Test Harness, WAR, and plugin
dependencies explicit. Integration tests use standard `JenkinsRule` APIs and a
small test-only retriever for the working-tree library. Reconsider the mkobit
plugin only after a release explicitly supports the repository's current Gradle
version and passes the same compatibility spike.

Jenkinsfile Runner may be evaluated later for complete Jenkinsfile smoke tests,
but it is not the first choice for library-level integration assertions.

## CI adoption

Keep `integrationTest` manually triggered or non-blocking in CI initially.
Promote it into the required `check` lifecycle only after it remains reliable
and its measured maintenance cost is acceptable.

Add expensive scenarios only in response to concrete requirements:

- controller restart for retained state across restart;
- authenticated SCM for credential behavior owned by this library;
- separate agents for controller/agent boundary behavior;
- broader plugin combinations for an explicit compatibility promise.

The integration suite must remain a test of this library rather than a test of
Jenkins itself.
