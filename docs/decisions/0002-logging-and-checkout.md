# Decision: explicit logging and checkout APIs

Date: 2026-09-21
Status: implemented, with real Jenkins validation added in stage 5

## Context

Large repository checkouts need visible, independently configurable clone/fetch
and checkout timeouts. Helpers and Jenkinsfiles need the same readable log
format, with ordinary Jenkins failure semantics.

The reference review covered logging implementations, level filtering, color,
checkout adapters, repository models, tests, and coding rules. The implementations
here are original. The useful lessons are typed inputs, named operations,
central formatting and testing the actual Pipeline-step arguments.

## Decisions

- Use `log.forContext(...)` and `gitUtils.checkout(...)` with named methods.
  Do not introduce `call()` APIs for convenience.
- Keep five log levels and instance-local configuration. Explicit context avoids
  reflection; explicit color avoids hidden environment dependencies. No global
  initializer, object dumping, credential discovery or additional log categories.
- Keep options and results immutable, and builders mutable only during setup.
  Copy fields explicitly; no serialization-based object conversion.
- Use `Serializable` for state retained through Pipeline suspension. Keep Pipeline
  steps in CPS methods; `@NonCPS` is limited to pure validation, mapping and
  formatting. Constructors perform no steps and call no CPS helper methods.
  Persist enum names internally while retaining typed getters: the real restart
  test exposed sandbox rejection of Groovy enum initialization during restoration.
- Translate options to plugin maps at one boundary in `GitHelper`. Do not
  reproduce clone/fetch operations, force workspace cleanup or add implicit retries.
  Read `HEAD` through the agent after checkout: real multi-checkout tests exposed
  stale plugin return metadata when revisiting a previously checked-out revision.
  Use a fixed Git command with no user input; preserve checkout and command failures.
- Preserve native exceptions and interruptions. `error` logs only; `fail` logs
  then invokes the native Jenkins failure step.

## Verification boundaries

JUnit Jupiter and JenkinsPipelineUnit verify API behavior and the intended
plugin arguments. Java serialization tests check data objects; they do not prove
Jenkins restart recovery. No new dependency was needed for these utilities.
Actual plugin execution, untrusted-library sandbox access, library retrieval and
controller restart are covered by the [real Jenkins suite](0003-real-jenkins-verification.md).

References:
[Shared libraries](https://www.jenkins.io/doc/book/pipeline/shared-libraries/),
[CPS boundaries](https://www.jenkins.io/doc/book/pipeline/cps-method-mismatches/),
[Git checkout](https://www.jenkins.io/doc/pipeline/steps/params/scmgit/).
