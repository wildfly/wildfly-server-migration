# Working Preferences

## Code style

- **Consistency first** — match the existing style, naming conventions, and patterns in the file or module being edited. Do not introduce a new style unless the entire module is being updated.
- **Java** — follow the conventions already present (JBoss project style): no trailing whitespace, 4-space indentation, Javadoc on public API.
- **Maven** — keep `pom.xml` files consistent with sibling modules (same plugin versions, dependency scopes, etc.).
- **Documentation** — keep `README.md` files consistent with existing docs in structure and voice.
- **Minimal diffs** — produce the smallest change that solves the problem. Do not refactor, clean up, or reformat code that is unrelated to the task.

---

## Refactoring approach

- **Proactive** — if the AI spots a clear improvement opportunity directly related to the task (e.g., a repeated pattern that the new code should also follow), it should raise it and apply it unless told otherwise.
- **Unrelated code** — do not touch code that is not directly related to the task even if it looks improvable.

---

## Commit messages and pull requests

See [`CONTRIBUTING.md`](../CONTRIBUTING.md) for the canonical rules. Key points for AI context:

- Commit message format: `[CMTOOL-XXX] short imperative description` (multiple issues: `[CMTOOL-XXX][CMTOOL-YYY] …`).
- PR title must match the commit message format.
- PR description must include a link to the JIRA issue.
- Branch naming: use the JIRA issue number, e.g., `CMTOOL-338`.

---

## AI collaboration preferences

- **Proactive** — suggest improvements when they are directly related to the task; don't ask for permission first.
- **No explanations unless asked** — skip preamble and narration; just show the diff or the result.
- **Grounded answers only** — read the relevant file before answering questions about code; never speculate.
- **Investigate before acting** — for non-trivial changes, check the reference modules (`servers/wildfly41.0`, `core`) before writing new code, to ensure the implementation matches existing patterns.
- **Consistent with existing reference modules** — when adding a new server or migration module, use `servers/wildfly41.0` as the primary template.

---

## Testing

- Unit tests live in `core/src/test/` and companion `src/test/` directories within each module.
- Integration tests live under `testsuite/integration-tests/` and require `-DrunITs`. See [`testsuite/README.md`](../testsuite/README.md) for the full command reference.
- EAP integration tests require `testsuite.eapServersDir` to be set; they silently skip when it is not.
- After any change to server or migration modules, run the relevant IT module to verify:
  ```bash
  mvn -f testsuite/integration-tests/wildfly42.0/pom.xml verify
  ```

---

## Branch rename workflow

When renaming a branch that backs an open PR:
1. Push the new branch name first.
2. Retarget the PR with `gh pr edit --head <new-branch>` *before* deleting the old branch — or open a new PR from the new branch first.
3. Only then delete the old branch. Deleting it while it is still the PR head closes the PR automatically.

---

## Adding new server / migration support

Always use the maintenance scripts as the starting point — see [`maintenance/README.md`](../maintenance/README.md) for usage and what each script does.

Reference the most recent WildFly server module (`servers/wildfly41.0` or higher) for how tasks are structured.
