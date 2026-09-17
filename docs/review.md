# Review checklist

- Does the change stay within the approved milestone and solve a concrete need?
- Are Groovy types explicit and values final wherever appropriate?
- Are models mapped explicitly without JSON/Slurper conversion round trips?
- Are `vars` stateless, CPS boundaries correct and retained objects serializable?
- Are original exceptions and interruptions preserved?
- Are credentials represented by IDs and excluded from logs and examples?
- Is the library still a single Gradle project with standard Jenkins roots?
- Are dependency versions catalog-backed and classpath changes verified?
- Are tests behavioral, with mocks clearly distinguished from real Jenkins?
- Are examples, metadata and documentation original and neutral?
- Are task failures and deferred checks reported honestly?

Agents provide a proposed `git commit --signoff` command with a useful message
at a milestone, but never execute it or stage/change tracking of files.
