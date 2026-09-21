# Agent instructions

This repository is one Jenkins shared library and one Gradle project. Read
[coding](docs/coding.md), [testing](docs/testing.md) and
[review guidance](docs/review.md) before changing code.

## Current milestone

Implement consumer pipelines, JCasC fixtures, real Jenkins integration/restart
tests and GitHub Actions (plan step 5, authorized 2026-09-21). Then measure and
tune Gradle performance without weakening verification. Steps 1–4 are complete.
Do not claim runtime support from compilation and mock tests alone.
Read [the session checkpoint](docs/session-checkpoint.md) before resuming.

## Working rules

- Write original code, documentation and neutral fixtures. Do not import code,
  identifiers, endpoints or business logic from private or reference repositories.
- Use explicit Groovy types and `final` wherever a value is not reassigned.
  Never use `def`, untyped method parameters or JSON/Slurper round trips for
  object conversion.
- Use the multiline block format in [coding conventions](docs/coding.md) for all
  code comments, including explanatory comments and license headers.
- Use explicit named methods; avoid `call()` APIs under the coding guide rule.
- Keep `vars` stateless and classes focused. Avoid speculative abstractions.
- Keep versions in the catalog and use the Gradle Wrapper. Preserve the Jenkins
  Groovy baseline independently of Gradle's own runtime.
- Run `./gradlew foundationCheck` during development and `./gradlew clean build`
  for full validation once the integration suite is implemented. Never hide
  failures with task exclusions in CI.
- Prepare meaningful commit messages and commands with `--signoff` at milestones.
  Never stage, commit, remove files from the Git index or change tracking state.
  The maintainer performs all index and commit operations.
- Do not publish, create releases or change remote settings without authorization.

`AGENTS.md` is the authoritative agent entry point. Keep detailed rules in the
linked documents instead of duplicating them across editor-specific files.
