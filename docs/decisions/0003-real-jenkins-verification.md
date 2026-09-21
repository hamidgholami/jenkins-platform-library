# 0003 — Verify consumers in disposable Jenkins containers

Status: implemented for the stage 5 validation baseline.

## Decision

Use a JUnit Jupiter suite to orchestrate the official Jenkins image through the
Docker CLI. Keep JenkinsPipelineUnit for fast behavior tests. A separate inbound
agent runs Git operations; the controller has no executors and neither container
receives the Docker socket. JCasC configures authentication, generated test
credentials and the untrusted shared library.

Load the candidate working-tree source through authenticated Git and `@Library`,
with sandbox enforcement intact. Test actual plugin behavior, retained objects
across controller restart, failure handling and cancellation. One environment is
shared within a suite to avoid repeated Jenkins boot and plugin installation.
Jobs and workspaces are separate, except the deliberate workspace-reuse scenario.

Git and logging require no additional script approvals. Keep that property in
the integration gate. Approvals apply beyond a single job, so a future exception
must be narrowly reviewed and recorded in reproducible controller configuration;
never approve arbitrary signatures just to make a failing consumer pass. This
suite validates exercised paths on the pinned baseline, not every future API or
controller upgrade. See [Jenkins script approval guidance](https://www.jenkins.io/doc/book/managing/script-approval/).

Use a digest-pinned image and a complete plugin lock containing versions and
publisher SHA-256 checksums. Verify downloads before image creation and verify the
installed active plugin set before running consumers. The lock describes runtime
installation; the Gradle catalog describes compilation/test dependencies. Keep
their Jenkins core and shared-library plugin baselines aligned when upgrading.

The small Java fixture server wraps Git's own smart-HTTP backend with generated
Basic credentials and read-only routes. It runs only on the disposable Docker
network. This avoids external repositories, embedded credentials in URLs and a
separate third-party Git-server image. Synthetic histories do not touch the user's
Git index or create commits in the user's repository.

## Consequences

The suite needs Docker and takes longer than unit tests. Verified plugin downloads
and image layers are reusable; integration outcomes are deliberately not cached.
Reports identify the exact candidate and runtime and redact generated credentials.
Cleanup removes each run's named resources, including the persisted controller
home used for restart verification.

An embedded Jenkins Test Harness remains a future option for focused plugin tests.
It would add a second runtime/dependency model without replacing the separate-agent,
Git retrieval and controller-restart scenarios needed here.

The tested fixture uses HTTP credentials and small repositories. It does not
establish SSH behavior, large-repository performance or compatibility with arbitrary
plugin versions. GitHub's Linux runner still needs its own successful workflow run.

Sources: [Jenkins shared libraries](https://www.jenkins.io/doc/book/pipeline/shared-libraries/),
[official Jenkins image](https://github.com/jenkinsci/docker),
[Configuration as Code](https://plugins.jenkins.io/configuration-as-code/).
