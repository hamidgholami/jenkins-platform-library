# Build performance

Measurement and tuning are authorized but deferred to the next session at the
maintainer's request. No performance improvement has been measured or claimed.

Start with the [session checkpoint](session-checkpoint.md). Compare warm incremental
`foundationCheck` with and without a persistent daemon, forced unit-test execution
with one and two isolated test JVMs, and configuration-cache compatibility.
Measure before changing defaults; keep integration results uncached and preserve
all required checks. Avoid in-process JUnit parallelism because Pipeline mocks
modify shared Groovy metaclasses.

Also measure the integration fixture's default job quiet period. Setting it to
zero may remove unnecessary queue delays without reducing test coverage. Do not
reduce checkout timeouts or remove restart tests to make the build look faster.
