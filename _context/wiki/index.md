# JBoss Server Migration Tool — Wiki

Concise orientation for AI agents and contributors. Follow links only when they are relevant to the task at hand.

## Pages

| File | Contents |
|---|---|
| [project.md](project.md) | Goals, key modules, architecture notes, and workflow pointers — developer context not covered by the READMEs |
| [preferences.md](preferences.md) | Code style, AI collaboration rules, commit conventions (with links to canonical sources) |
| [../README.md](../README.md) | End-user guide: what the tool does, build instructions, sample migration run |
| [../CONTRIBUTING.md](../CONTRIBUTING.md) | Contribution guide: fork/clone setup, JIRA workflow, PR and commit message format |
| [../maintenance/README.md](../maintenance/README.md) | Maintenance scripts: add/remove WildFly and EAP server versions, set project version |
| [../testsuite/README.md](../testsuite/README.md) | Integration testsuite: architecture, server cache, running tests, CI |


## Quick facts

- **Repo:** `wildfly/wildfly-server-migration` (GitHub)
- **Language:** Java 17+, Maven multi-module
- **Issue tracker:** [JIRA CMTOOL](https://issues.redhat.com/projects/CMTOOL/issues)
- **Default branch:** `main`
- **Build:** `mvn clean install` (unit tests) · `mvn install -DrunITs` (+ integration tests)
