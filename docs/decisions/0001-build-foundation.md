# 0001 — One library with an independent Gradle build

Status: accepted for the foundation milestone.

## Decision

Use one Gradle project, the standard Groovy and CodeNarc plugins, Kotlin DSL,
catalog-backed dependencies and the official Wrapper. Jenkins loads source from
the root `src`, `vars` and `resources` directories. Nested consumer Jenkinsfiles
are compiled independently to prevent duplicate generated script class names.

Prefer this small explicit build over a dedicated shared-library build plugin.
The latter is viable again, but its conventions and upgrade cadence are not
required for the initial library. Maven would offer convenient Jenkins plugin
test tooling, but this is a source-loaded shared library and Gradle suits the
existing development workflow.

## Dependency handling

Jenkins plugins publish HPI packaging in Maven metadata. A component metadata
rule changes only the artifacts for components whose POM packaging is `hpi` or
`jpi`, selecting their companion JAR and preserving the dependency graph.
It does not alter ordinary JAR dependencies or discard transitive dependencies.
Each direct dependency is declared once through the catalog.

Keep Jenkins APIs compile-only. Unit tests receive the APIs needed to load
Pipeline scripts. Compilation dependencies do not install plugins on a running
controller. The container suite owns a separate complete runtime plugin lock.

Strict dependency locks and SHA-256 verification make resolution reviewable.
`verifyDependencies` checks for unwanted archives, incompatible Groovy modules
and lost plugin transitives.

## Consequences

Jenkins-compatible Groovy compilation is independent of Gradle and CodeNarc's
own Groovy versions. JDK 21 is the toolchain; Groovy 2.4 emits Java 8 bytecode.
Mock tests do not prove CPS correctness. Full `check` requires the real Jenkins
suite; `foundationCheck` remains the explicit, narrower gate without Docker.

Sources: [Gradle metadata rules](https://docs.gradle.org/current/userguide/component_metadata_rules.html),
[Jenkins shared libraries](https://www.jenkins.io/doc/book/pipeline/shared-libraries/).
