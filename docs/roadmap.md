# Roadmap

## Current milestone: foundation

Repository skeleton, governance, coding rules, local tooling and verified Gradle
dependency/test wiring. There are no Git or logging production APIs yet.

## Next approved-plan stages, pending implementation authorization

1. Logging: five levels, explicit context and color, log-only `error`, and `fail`
   delegating to Jenkins's native failure step; unit tests.
2. Git checkout: typed configuration/results, complete history by default,
   configurable clone/fetch and checkout timeouts, refspecs, tags, shallow depth,
   reference repositories and explicit cleanup; unit tests.
3. Scripted consumers, JCasC fixtures, real Jenkins integration/restart tests and
   GitHub Actions on PRs and `main`.
4. Final compatibility, release and publication review. Enable private security
   reporting and required checks when the remote repository is created.

## Required future Git capabilities

| Capability | Acceptance criteria |
|---|---|
| LFS | Explicit opt-in; agent prerequisite checks; real fixture verifies downloaded content and failure behavior |
| Submodules | Explicit recursion, credentials, timeout and depth controls; nested/private fixture tests |
| Sparse checkout | Explicit path selection; real tests verify included/excluded paths and interaction with shallow clones |
| Provider-specific PR checkout | Preserve intended head/merge revision semantics; provider fixtures verify exact checked-out commit and credentials isolation |

## Later evaluation

Evaluate embedded Jenkins Test Harness when focused plugin/CPS tests justify its
maintenance cost. Broader application delivery helpers require their own scoped
design and acceptance criteria.

YAML-driven workflow design is learning only; it is not an implementation feature
or scheduled milestone. Reconsider it only for a future pipeline that needs it.
