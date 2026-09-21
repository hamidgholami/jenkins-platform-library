# Testing strategy

## Purpose

The repository uses multiple test layers because no single Jenkins testing tool
provides both fast feedback and complete runtime confidence.

The current unit suite is intentionally the only required test layer. A small
real-Jenkins integration suite is planned to cover behavior that mocks cannot
establish.

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

The next testing milestone is an optional `integrationTest` suite running
representative consumers with Jenkins Test Harness.

Start with no more than two scenarios:

1. Load the working-tree library and exercise logging from a sandboxed Pipeline.
2. Exercise `gitUtils.checkout` with the real Git plugin and a temporary local
   bare repository.

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

Evaluate the mkobit Jenkins Shared Library Gradle plugin before wiring Jenkins
Test Harness directly. The plugin currently provides the source-set, Jenkins
WAR, plugin dependency, BOM, and local-library registration conventions that are
otherwise expensive to maintain by hand.

Adopt it only if a short compatibility spike proves that:

- it works with the repository's current Gradle and JDK versions;
- Jenkins and plugin versions remain explicit and reviewable;
- production source code remains independent of the plugin;
- the integration tests are understandable without plugin internals;
- the suite has acceptable runtime and memory use;
- removing or replacing the plugin later would not require rewriting tests from
  first principles.

If those conditions are not met, use Jenkins Test Harness directly with the
smallest explicit dependency and task configuration possible. Jenkinsfile Runner
may be evaluated later for complete Jenkinsfile smoke tests, but it is not the
first choice for library-level integration assertions.

## CI adoption

Introduce `integrationTest` as a manually triggered or non-blocking CI job.
Promote it into the required `check` lifecycle only after it is reliable, has a
measured maintenance cost, and demonstrates that it catches failures the unit
suite cannot.

Add expensive scenarios only in response to concrete requirements:

- controller restart for retained state across restart;
- authenticated SCM for credential behavior owned by this library;
- separate agents for controller/agent boundary behavior;
- broader plugin combinations for an explicit compatibility promise.

The integration suite must remain a test of this library rather than a test of
Jenkins itself.
