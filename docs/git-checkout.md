# Git checkout

Use `gitUtils.checkout(options, logger)` inside an allocated Jenkins workspace:

```groovy
import io.github.hamidgholami.jenkins.platform.git.GitCheckoutOptions
import io.github.hamidgholami.jenkins.platform.git.GitCheckoutResult

final GitCheckoutOptions options = GitCheckoutOptions
        .builder('https://github.com/example/service.git')
        .branch('main')
        .credentialsId('source-reader')
        .build()

node('linux') {
    dir('source') {
        final GitCheckoutResult result = gitUtils.checkout(options)
        echo('Checked out ' + result.commit)
    }
}
```

Choose exactly one revision with `.branch(name)`, `.tag(name)`, or
`.commit(fullSha)`. The default behavior fetches complete history and tags,
uses the `origin` remote, disables polling and changelog generation, and leaves
existing workspace content in place.

Useful optional controls include:

| Method | Purpose |
|---|---|
| `credentialsId(id)` | Jenkins credential used by the Git plugin |
| `shallowDepth(depth)` | Request a shallow clone |
| `fetchTags(enabled)` | Enable or disable tag fetching |
| `cloneTimeoutMinutes(minutes)` | Clone and fetch timeout |
| `checkoutTimeoutMinutes(minutes)` | Checkout timeout |
| `cleanBeforeCheckout(enabled)` | Clean the workspace before checkout |
| `pruneStaleBranches(enabled)` | Remove stale remote-tracking branches |
| `refspec(value)` | Override the default branch refspec |
| `referenceRepository(path)` | Use an agent-local reference repository |

The helper delegates cloning and checkout to Jenkins's `scmGit` and `checkout`
steps. It adds no retries and never deletes the workspace. Checkout failures and
Pipeline interruptions propagate to the caller unchanged.

The returned result contains the checked-out commit and available branch
metadata. The commit is read from the workspace's actual `HEAD`, avoiding stale
metadata after repeated checkouts.

Repository credentials belong in Jenkins. Do not embed passwords or tokens in
repository URLs or log complete checkout configurations.
