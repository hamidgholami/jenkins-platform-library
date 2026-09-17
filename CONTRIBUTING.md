# Contributing

Discuss substantial behavior or dependency changes before implementation. Keep
changes small, independently reviewable and supported by behavior-focused tests.

Read [coding](docs/coding.md), [testing](docs/testing.md) and
[review guidance](docs/review.md). Run `./gradlew foundationCheck` for the current
milestone. The real Jenkins suite and GitHub merge gates will be added later.

Use descriptive commit messages that explain the resulting behavior. Human
contributors must sign off contributions using `git commit --signoff` in
accordance with the [Developer Certificate of Origin](https://developercertificate.org/).
A DCO sign-off attests the right to contribute; it is not a cryptographic
signature. Contributors may also use `--gpg-sign` with their configured key.
Do not introduce a custom CLA.

Agents prepare commit messages but must not stage, commit or change tracking
state. The maintainer reviews and performs those operations.

Use neutral examples and disposable test data. Never contribute confidential
code, internal identifiers, endpoints, credentials or copied reference material.
License contributions under Apache-2.0 and add SPDX headers to substantial
original source files. Preserve upstream notices in generated tooling.
