# Contributing

Keep changes small and tied to a concrete Jenkins Pipeline use case. Prefer
standard Jenkins steps and plugin behavior over custom infrastructure or generic
frameworks.

Run the verification suite before opening a pull request:

```sh
./gradlew check
```

Add behavior-focused tests for public changes. Use neutral examples and never
include private endpoints, credentials, company-specific identifiers, or copied
proprietary code.

Contributions are licensed under Apache-2.0 and require a Developer Certificate
of Origin sign-off using `git commit --signoff`.
