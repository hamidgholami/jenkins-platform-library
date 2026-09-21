# Compatibility

These are development targets, not broad production support claims. The real
Jenkins suite is implemented and undergoing validation; see the
[session checkpoint](session-checkpoint.md) for the latest result. No release exists.

| Component | Baseline | Purpose |
|---|---|---|
| JDK | 21 LTS | Gradle execution and test toolchain |
| Gradle | 9.7.1 | Pinned stable Wrapper; Gradle has no free conventional LTS line |
| Jenkins | 2.568.3 LTS | Compilation and container-test baseline |
| Library Groovy | 2.4.21 | Matches the Jenkins core dependency baseline |
| JenkinsPipelineUnit | 1.29 | Mock-based Pipeline tests |
| JUnit Jupiter | 6.0.1 | Unit test execution |

Groovy 2.4 compilation targets Java 8 bytecode, even though compilation and tests
run on JDK 21. This is a compiler compatibility setting, not a claim that Jenkins
can run on Java 8. Gradle's embedded Groovy and CodeNarc's Groovy are isolated
from the library compilation/test runtime.

Keep versions in the catalog and Wrapper properties authoritative. Upgrade the
Jenkins baseline, its compilation APIs and the integration plugin lock
together, with real consumer and restart tests before declaring runtime support.

Sources: [Jenkins core metadata](https://repo.jenkins-ci.org/public/org/jenkins-ci/main/jenkins-core/2.568.3/jenkins-core-2.568.3.pom),
[Gradle releases](https://gradle.org/releases/),
[JenkinsPipelineUnit](https://github.com/jenkinsci/JenkinsPipelineUnit).
