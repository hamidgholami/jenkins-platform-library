# Git checkout

Use `gitUtils.checkout(options, logger)` inside an allocated agent workspace.
It delegates to Jenkins's `checkout(scm: scmGit(...))` and returns a
`GitCheckoutResult`. There is no callable global-variable API.

```groovy
import io.github.hamidgholami.jenkins.platform.git.GitCheckoutOptions
import io.github.hamidgholami.jenkins.platform.git.GitCheckoutResult

final GitCheckoutOptions source = GitCheckoutOptions
        .builder('https://git.example.org/team/service.git')
        .branch('main')
        .credentialsId('source-reader')
        .build()

node('linux') {
    dir('source') {
        final GitCheckoutResult result = gitUtils.checkout(source)
        final String sourceCommit = result.commit
    }
}
```

The snippet assumes this library has already been loaded. With `@Library`, place
its annotation on a typed import as described in [coding conventions](coding.md).
Use the default library version configured by the administrator until release
tags are available. The helper cannot configure retrieval of its own library;
credentials, version and clone behavior for that retrieval belong in Jenkins
library configuration/JCasC. A runnable consumer and JCasC fixture are deferred
to the integration milestone.

## Configuration

Build immutable options through `GitCheckoutOptions.builder(repositoryUrl)`.
Select `.branch(name)`, `.tag(name)` or `.commit(sha)` before `.build()`; a later
selector replaces the earlier one. Branch and tag names are short literal names
such as `feature/cache` or `release/1.0`, without `refs/` prefixes or wildcards.
Commit selection requires a full 40-character hexadecimal SHA. Returned commit
metadata must match an explicitly requested SHA.

| Builder method | Default / behavior |
|---|---|
| `remoteName(name)` | `origin`; letters, digits, underscores and hyphens, starting with a letter or digit |
| `credentialsId(id)` | Omitted; public repository or agent authentication configuration |
| `cloneTimeoutMinutes(minutes)` | 30; applies to clone and fetch |
| `checkoutTimeoutMinutes(minutes)` | 15; applies to checkout |
| `shallowDepth(depth)` | Omitted, requesting full history; positive depth opts into shallow fetch |
| `fetchTags(enabled)` | `true`; must remain enabled for tag selection |
| `refspec(value)` | All branch heads mapped to the configured remote |
| `honorRefspec(enabled)` | `true`, including the initial clone |
| `referenceRepository(path)` | Omitted; optional agent-local reference repository |
| `cleanBeforeCheckout(enabled)` | `false` |
| `pruneStaleBranches(enabled)` | `false` |
| `poll(enabled)` | `false` |
| `changelog(enabled)` | `false` |

Timeouts must be positive integers in minutes. Outer Pipeline timeouts still
apply. There is no helper-added retry; Jenkins SCM configuration can have its
own retry behavior. Callers may wrap checkout with a deliberate retry policy.

URLs support HTTP(S), SSH (including SCP notation) and Git transport. Local
paths and remote helper protocols are outside this API. Use credential IDs;
embedded HTTP credentials, SSH passwords, URL queries and fragments are rejected.
SSH usernames such as `git@host` are allowed. Errors do not repeat input URLs.

Custom refspecs accept space-separated `refs/source:refs/destination` fetch
mappings, optionally prefixed by `+`. Wildcard mappings need one `*` on each
side. Negative and source-only refspecs are outside this API. Branch checkout
selects `refs/remotes/<remoteName>/<branch>`, so custom mappings must populate
that destination. Environment values must be resolved by the caller, rather
than supplied as `$...` expressions in selectors or refspecs.

## Large repositories

Start by increasing timeouts and keeping history intact. If the build needs only
one branch, a narrow refspec can reduce transfer without truncating its history:

```groovy
final GitCheckoutOptions source = GitCheckoutOptions
        .builder('ssh://git@git.example.org/team/service.git')
        .branch('main')
        .credentialsId('source-reader')
        .cloneTimeoutMinutes(60)
        .checkoutTimeoutMinutes(20)
        .refspec('+refs/heads/main:refs/remotes/origin/main')
        .fetchTags(false)
        .build()
```

Use `.shallowDepth(50)` only when the build can work with incomplete history.
Tags, version calculation, merge-base and historical commit selection may need
more history. An explicit commit must be reachable through fetched refs and
available within the shallow depth, if any. The helper does not fetch an
arbitrary unreachable object by SHA.

Reference repositories can reduce network traffic, but their agent-local object
store must remain available while workspaces borrow its objects. Jenkins may
ignore a missing reference path. Capacity and reference lifecycle belong to agent
management.

The helper reuses the caller's workspace through the plugin. It never deletes
that workspace, forces a reclone or runs repair commands. A workspace previously
cloned shallowly is not automatically unshallowed when shallow depth is omitted.
Choose a new directory or deliberately repair that workspace outside the helper.
Use separate directories for concurrent checkouts and different repositories.

`cleanBeforeCheckout(true)` resets tracked files and removes untracked/ignored
files through the plugin. Nested untracked Git repositories are not forcibly
removed. It is opt-in because it can discard workspace content.

## Result and logging

The result exposes `commit`, `branch` and `localBranch` as immutable strings.
Branch values are the plugin's metadata, not inferred from the requested selector;
either can be null. No environment variables are assigned by this helper.

Without a logger, checkout emits plain INFO messages under `GitHelper.checkout`.
To choose a threshold, context or color, supply a logger from `log.forContext`:

```groovy
final PipelineLogger logger = log.forContext('Source', LogLevel.DEBUG, true)
ansiColor('xterm') {
    final GitCheckoutResult result = gitUtils.checkout(source, logger)
}
```

Import `PipelineLogger` and `LogLevel` as shown in [logging](logging.md).
Helper logs contain status, timeouts at DEBUG, and the resolved commit. They do
not dump URLs, credential IDs, option objects or exception text. The Git plugin
still emits its own diagnostics. Checkout errors and cancellation pass through
unchanged, with no success log on failure.

The agent needs Git; Jenkins needs the Git, Git Client, Pipeline SCM Step and
Pipeline Groovy Libraries plugins. Configure SSH host verification in Jenkins.
LFS, submodule controls, sparse checkout and provider-specific PR semantics remain
on the [roadmap](roadmap.md); this helper does not implement their policies.
For multibranch PR jobs that rely on the provider's merge strategy, keep using
`checkout(scm)` until that support is implemented.

See the upstream [Git plugin](https://plugins.jenkins.io/git/) and
[`scmGit` reference](https://www.jenkins.io/doc/pipeline/steps/params/scmgit/).
Plugin behavior, sandbox execution and restart recovery still require the
planned real Jenkins integration suite.
