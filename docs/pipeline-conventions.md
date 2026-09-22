# Pipeline conventions

Pipeline projects live below `pipelines/`. Nested directories may describe the
domain and purpose, but every leaf project contains exactly one
`Jenkinsfile.groovy`. Gradle exposes each leaf directory as a separate IntelliJ
source root, so Jenkinsfiles remain package-free scripts without false package
warnings.

Example Jenkinsfiles use the explicit standalone Shared Library annotation:

```groovy
import org.jenkinsci.plugins.workflow.libs.Library

@Library('jenkins-platform-library') _
```

The `_` is a harmless annotated expression that lets the annotation stand on
its own. It avoids attaching the annotation to an unrelated application import
and gives every pipeline the same recognizable header.

## Job properties

Scripted Pipelines apply job properties before reading `params`. Each pipeline
includes a `REFRESH_JOB_PROPERTIES` boolean parameter and exits successfully
after applying properties on either the first build or an explicit refresh:

```groovy
properties([
        parameters([
                booleanParam(
                        name: 'REFRESH_JOB_PROPERTIES',
                        defaultValue: false,
                        description: 'Apply the current Jenkinsfile properties and exit successfully',
                ),
        ]),
])

if (env.BUILD_NUMBER == '1' || params.get('REFRESH_JOB_PROPERTIES', false) == true) {
    echo(env.BUILD_NUMBER == '1'
            ? 'Job properties initialized; run the pipeline again with parameters'
            : 'Job properties refreshed')
    currentBuild.result = 'SUCCESS'
    return
}
```

This keeps the first build from running with required values that the user had
no opportunity to enter. It also gives administrators a normal successful build
that refreshes parameters, triggers, retention, and other job properties after
the Jenkinsfile changes.

Keep this control flow visible in the Jenkinsfile. A Shared Library method can
apply properties and return a decision, but it cannot return from the calling
Jenkinsfile; wrapping these few lines would therefore add indirection without
removing the pipeline-level guard.

## IntelliJ Pipeline DSL

The checked-in `ide/jenkins-pipeline.gdsl` describes the Jenkins globals and
common steps used by this repository, including `params`, `env`, `properties`,
`node`, `stage`, `sh`, and `withCredentials`. It exists only for navigation and
completion; Jenkins remains the runtime authority.

Pipeline steps contributed by plugins depend on the plugins installed on a
particular Jenkins controller. For completion beyond the repository baseline,
download that controller's generated `pipeline-syntax/gdsl` file as
`ide/controller.gdsl`. This local file is ignored because it describes one
controller rather than the portable project.
