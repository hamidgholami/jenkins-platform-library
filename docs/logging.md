# Logging

Create a logger with an explicit context. Instances carry their own threshold
and color setting; no initialization or shared mutable configuration is needed.

```groovy
import io.github.hamidgholami.jenkins.platform.logging.LogLevel
import io.github.hamidgholami.jenkins.platform.logging.PipelineLogger

final PipelineLogger logger = log.forContext('Build', LogLevel.INFO)
logger.info('Starting verification')
logger.warn('Optional report is unavailable')
```

Plain output:

```text
[INFO] [Build] Starting verification
[WARN] [Build] Optional report is unavailable
```

The five levels, from least to most severe, are TRACE, DEBUG, INFO, WARN and
ERROR. The default threshold is INFO. Methods accept strings. Every line of a
multiline message receives the same prefix, including empty lines. CRLF and CR
are normalized to LF. A null message is invalid; an empty message is allowed.
Contexts must be nonblank, without surrounding whitespace, control characters
or brackets.

## Color and timestamps

Color is off by default. To enable it, use the AnsiColor wrapper and set the
logger's color argument to true. Only the severity label is colored; it resets
before the context and message. Timestamper supplies timestamps:

```groovy
final PipelineLogger logger = log.forContext('Build', LogLevel.DEBUG, true)
timestamps() {
    ansiColor('xterm') {
        logger.debug('Preparing workspace')
        logger.info('Ready')
    }
}
```

See [AnsiColor](https://plugins.jenkins.io/ansicolor/) and
[Timestamper](https://plugins.jenkins.io/timestamper/). Plain logging needs neither
wrapper. A colored logger must be used inside an AnsiColor scope; the library
intentionally does not inspect environment variables or start wrappers itself.

## Logging and failing

`logger.error(message)` emits an ERROR message. It does not throw or change
build status. `logger.fail(message)` first logs at ERROR, then calls the native
Jenkins `error(message)` step with the original message. An unhandled failure
fails the build; callers can use `try/catch`, `retry` or `catchError` normally.
Neither method assigns `currentBuild.result`.

For an existing exception, keep its identity and stack trace:

```groovy
try {
    sh('./verify.sh')
} catch (final Exception failure) {
    logger.error('Verification did not complete')
    throw failure
}
```

Do not replace cancellation or an existing failure with `logger.fail(...)`.
Do not log credentials, configuration objects or raw exception messages that
might contain secrets. The logger formats caller-supplied messages; it is not a
secret redaction service.

## Use from other helpers

Pass a `PipelineLogger` to a helper, or construct one with the Pipeline script
and an explicit context. Its constructor performs no Pipeline steps. The logger
is serializable and retains the script so calls still go through Jenkins.
Only pure formatting uses `@NonCPS`; `echo` and `error` stay in CPS code.
Real Jenkins restart and sandbox validation remain part of the integration
milestone. Unit tests do not establish that compatibility.
