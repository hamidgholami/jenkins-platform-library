# Agent instructions

This repository is one Jenkins shared library and one Gradle project. Read
[coding](docs/coding.md), [testing](docs/testing.md) and
[review guidance](docs/review.md) before changing code.

## Current milestone

Implement repository and Gradle foundations only. Logging, Git checkout,
consumer pipelines, real Jenkins integration tests and GitHub Actions are later
milestones. Do not create placeholder production APIs or claim runtime support
from compilation and mock tests alone.

## Working rules

- Write original code, documentation and neutral fixtures. Do not import code,
  identifiers, endpoints or business logic from private or reference repositories.
- Use explicit Groovy types and `final` wherever a value is not reassigned.
  Never use `def`, untyped method parameters or JSON/Slurper round trips for
  object conversion.
- Use the multiline block format in [coding conventions](docs/coding.md) for all
  code comments, including explanatory comments and license headers.
- Keep `vars` stateless and classes focused. Avoid speculative abstractions.
- Keep versions in the catalog and use the Gradle Wrapper. Preserve the Jenkins
  Groovy baseline independently of Gradle's own runtime.
- Run `./gradlew foundationCheck` for this milestone. Report that real Jenkins
  integration testing is deferred; never hide failures with task exclusions in CI.
- Prepare meaningful commit messages and commands with `--signoff` at milestones.
  Never stage, commit, remove files from the Git index or change tracking state.
  The maintainer performs all index and commit operations.
- Do not publish, create releases or change remote settings without authorization.

`AGENTS.md` is the authoritative agent entry point. Keep detailed rules in the
linked documents instead of duplicating them across editor-specific files.
