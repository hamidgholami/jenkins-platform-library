# Security policy

This project is unreleased. There are currently no supported production releases.
The compatibility document records targets rather than a support guarantee.

Do not disclose vulnerabilities or secrets in public issues. Before publication,
coordinate privately with the maintainer through an existing private channel.
The publication checklist requires enabling GitHub private vulnerability
reporting; once enabled, use the repository's Security tab to report privately.

Provide reproduction steps, affected revision and expected impact using neutral,
sanitized fixtures. Never attach production credentials, Pipeline environment
dumps or private repository contents.

Shared-library changes can affect every consumer. Prefer sandbox-compatible
steps, least-privilege Jenkins credentials and pinned library revisions. Review
dependency changes and checksum additions before accepting them. Tests must use
disposable credentials and isolated services.
