# Roadmap

## Completed through the utilities milestone

Repository foundations, logging and configurable Git checkout with unit tests.
The utilities use explicit named methods, immutable configuration/results and
instance-local logging. Real Jenkins compatibility remains unverified.

## Next planned stages, pending implementation authorization

1. Scripted consumers, JCasC fixtures, real Jenkins integration/restart tests and
   GitHub Actions on PRs and `main`.
2. Final compatibility and release review. Configure private security reporting
   and required checks on the remote repository.

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
