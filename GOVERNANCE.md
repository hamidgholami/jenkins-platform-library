# Governance

Hamid Gholami is the initial maintainer and release owner. The project uses a
maintainer-led model suitable for a small shared library.

Propose behavior changes through issues or pull requests. Record significant
architecture choices as numbered decisions under `docs/decisions/`. The
maintainer resolves design disagreements with documented technical reasoning.

Contributions require DCO sign-offs. Maintainers review compatibility, tests,
licensing and security before accepting changes. Future additional maintainers
will be named here and in CODEOWNERS.

Use semantic versioning for release tags, starting with `v0.1.0`. Document
breaking changes and migration instructions, including during the 0.x series.
Consumers should pin reviewed tags or commit hashes. Releases are manual and
must pass the complete verification suite once implemented.

At publication, require pull requests and all CI status checks on `main`, and
disable force pushes and branch deletion. A sole maintainer cannot approve their
own pull request, so do not require an impossible self-approval. Revisit review
counts when another maintainer joins. Local files do not enforce remote rules.
