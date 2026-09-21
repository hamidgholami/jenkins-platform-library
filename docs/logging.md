# Logging

Create a logger with an explicit context:

```groovy
import io.github.hamidgholami.jenkins.platform.logging.LogLevel
import io.github.hamidgholami.jenkins.platform.logging.PipelineLogger

final PipelineLogger logger = log.forContext('Build', LogLevel.INFO)
logger.info('Starting verification')
logger.warn('Optional report is unavailable')
```

Output is consistent and easy to search:

```text
[INFO] [Build] Starting verification
[WARN] [Build] Optional report is unavailable
```

The available levels are `TRACE`, `DEBUG`, `INFO`, `WARN`, and `ERROR`.
The default threshold is `INFO`. Every line of a multiline message receives
the same prefix.

Color is disabled by default. Enable it only inside Jenkins's AnsiColor wrapper:

```groovy
final PipelineLogger logger = log.forContext('Build', LogLevel.DEBUG, true)
ansiColor('xterm') {
    logger.debug('Preparing workspace')
}
```

`logger.error(message)` only logs. `logger.fail(message)` logs at ERROR and
then calls Jenkins's native `error(message)` step. Existing exceptions should be
logged and rethrown so their type and stack trace are preserved.
