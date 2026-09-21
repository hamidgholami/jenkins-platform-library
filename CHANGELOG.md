# Changelog

## Unreleased

- Establish a single-library repository, governance and contributor guidance.
- Add a Kotlin DSL Gradle foundation with a version catalog, dependency checks,
  isolated Jenkinsfile compilation, CodeNarc and JUnit Jupiter test wiring.

- Add context-based logging with five levels, optional ANSI severity colors,
  multiline prefixes and native Jenkins failure handling through `fail`.
- Add `gitUtils.checkout` with immutable typed options/results, explicit revision
  selection, timeout and fetch tuning, and opt-in cleanup/pruning.
- Cover utility behavior, validation, failure propagation and serialization with
  JUnit Jupiter and JenkinsPipelineUnit tests.

Real Jenkins integration testing and CI are pending.
