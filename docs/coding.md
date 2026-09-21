# Coding conventions

## Code comments and license headers

Use multiline documentation-style blocks for all project-owned code comments,
including explanatory comments, inline explanations moved above the relevant
statement, and license headers. Do not use `//` comments or single-line block
comments. Apply this convention to new and modified Groovy, Kotlin and Java
code, including build scripts, configuration scripts, tests and Jenkinsfiles:

```groovy
/**
 * Explain the reason for the code here.
 * */
```

Use this copyright and SPDX block at the start of project-owned Groovy and
Kotlin source files, including build scripts and test fixtures:

```groovy
/**
 * Copyright 2026 Hamid Gholami
 * SPDX-License-Identifier: Apache-2.0
 * */
```

Use syntax-appropriate comments in other file formats. Preserve upstream
license notices in generated Gradle Wrapper files.

## Groovy and Pipeline

- Declare types for fields, locals, return values, parameters and closures.
  Never use `def`. Use `final` when a value is not reassigned, including parameters.
  A mutable test field assigned in setup is a legitimate exception to finality.
- Use four spaces, explicit returns, parentheses around calls, and single-quoted
  strings unless interpolation is needed. Keep names short and descriptive.
- Use explicit constructors or mapping for typed models. Do not use Slurper or
  JSON serialization/deserialization as a map-to-object conversion mechanism.
- Prefer composition and small focused classes. Avoid generic frameworks,
  placeholder interfaces and constants classes without meaningful constants.
- Prefer explicit named methods on `vars` scripts and classes. Do not implement
  `call()` as a convenience API. A rare exception requires no practical alternative
  and a substantial, documented benefit.
- `vars` exposes stateless entry points. Packaged classes belong below
  `src/io/github/hamidgholami/jenkins/platform/`. No `@Field` script state.
- Put `@Library` on a typed import when using library classes. Avoid the untyped
  underscore declaration. Keep classes packaged so their generated filenames do
  not collide with `vars` scripts on case-insensitive filesystems.
- Helpers that survive suspension must implement `Serializable`; use an explicit
  `serialVersionUID` and keep all reachable state serializable. No mutable static
  state, controller objects, iterators or parser instances across suspension.
- Constructors and `@NonCPS` methods must not call Pipeline steps. Use `@NonCPS`
  only for a justified pure computation; never to silence a serialization error.
- Do not apply `@CompileStatic` to CPS orchestration. Explicit types improve
  clarity but do not turn dynamic Jenkins steps into statically checked calls.
- Use Jenkins steps for agent I/O. No direct controller filesystem/process APIs
  and no runtime `@Grab`. Build and test tooling may use local filesystem APIs.
- Pass environment-derived values explicitly from consumers to helpers.
- Preserve exceptions and cancellation. Do not convert aborts into ordinary
  failures, swallow failures or log entire configuration/environment objects.

## Logging contract

Use five levels: ERROR, WARN, INFO, DEBUG and TRACE, with INFO by default.
`logger.error(message)` will log only. `logger.fail(message)` will log and call
the native Jenkins `error` step. Existing exceptions are logged and rethrown
unchanged. Do not directly assign build status from logging methods.

Only the logging implementation may call production `echo`. Test fixtures may
call `echo` to validate Pipeline mocking without implementing a logger early.
Timestamps are the consumer's responsibility through the Timestamper plugin.

## Build scripts

Use Kotlin DSL with immutable `val` declarations wherever possible. Dependency
versions belong in `gradle/libs.versions.toml`, never duplicated in build scripts.
The Gradle distribution pin lives in Wrapper properties. Runtime compatibility
and tooling classpaths are separate concerns.

CodeNarc checks typing, implicit closure parameters, parameter reassignment and selected
correctness rules. Formatting checks enforce whitespace and final newlines.
Reviewers must also check semantic rules that static tools cannot fully prove.
