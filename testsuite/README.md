# WildFly Server Migration Tool — Integration Testsuite

This directory contains the Java-based integration testsuite for the migration tool.
Tests are JUnit 5 `@ParameterizedTest` suites executed by the Maven Failsafe plugin.
Each supported target server has its own Maven module under `testsuite/<target>/`.

---

## Directory layout

```
testsuite/
├── server-cache/                Persistent server distribution cache (shared, survives mvn clean)
│   └── servers/                 Cache root — created at build time, delete manually to re-download
├── common/                      Shared base class and XML patch resources
├── integration-tests/           Parent pom for all per-target IT modules
│   ├── wildfly<version>/        Integration tests: *-to-WildFly-<version> migrations
│   ├── eap<version>/            Integration tests: *-to-EAP-<version> migrations
│   ├── eap8.0/                  Integration tests: *-to-EAP-8.0 migrations
│   ├── eap8.1/                  Integration tests: *-to-EAP-8.1 migrations
│   └── eap8.2/                  Integration tests: *-to-EAP-8.2 migrations
└── before/dist/                 Static test fixtures applied to source servers before migration
```

> **Note:** `testsuite/server-cache/servers/` is listed in `.gitignore`.
> It is never deleted by `mvn clean`. To force a re-download or rebuild
> of cached server distributions, delete the directory manually.

---

## How the testsuite works

### 1 — Cache population (`testsuite/server-cache`)

Before any integration tests run, the `server-cache` module provisions all
required server distributions into a persistent local cache at
`testsuite/server-cache/servers/`.

**WildFly distributions** are provisioned automatically:

| Condition | Action |
|---|---|
| A GitHub `Final` release exists for the major.minor | Download and extract it |
| Cached copy already equals the latest `Final` | Skip (up-to-date) |
| No `Final` release exists | Clone `wildfly/wildfly:main`, build, copy `dist/target/wildfly-*/` |
| A `SNAPSHOT` from `main` is cached but new commits exist upstream | Rebuild and replace |

Required versions are discovered by walking the `migrations/` directory tree —
every `wildfly<M>.<N>` directory name (at either the target or source level)
results in a cache entry named `wildfly-<M>.<N>.<qualifier>/`.

**EAP distributions** cannot be downloaded automatically. They must be supplied
manually via the `testsuite.eapServersDir` system property (see [Properties](#properties)).
Only the versions referenced in the `migrations/` tree are copied; any extras in
the local directory are ignored. When multiple qualifiers for the same major.minor
exist, the one whose name contains `GA` is preferred; otherwise the most recently
modified entry wins. If the property is not set, EAP tests skip silently.

Cache entries follow the naming convention:

```
wildfly-42.0.0.Final/
wildfly-42.0.0.Beta1-SNAPSHOT/
eap-8.2.0.GA/
eap-7.4.0.GA/
```

### 2 — Test execution (`testsuite/integration-tests/<target server>`)

Each per-target module uses `maven-failsafe-plugin` to run one parameterized
`*IT` class. Source versions are **discovered at runtime** from the
`migrations/<target server>/` directory tree, so they automatically track additions or
removals of migration modules — no hardcoded list to maintain.

For each source version, two test scenarios run:

| Scenario | What it does |
|---|---|
| `cleanMigration` | Migrates the source server's stock distribution (smoke test) |
| `patchedMigration` | Applies `before/dist/` fixtures to the source first, then migrates |

Both scenarios assert only that the migration tool exits with code `0`.

#### Per-test working directories

Each module integration tests run gets a fresh directory under `target/migrations/`:

```
target/migrations/
  wildfly41.0-to-wildfly42.0-clean/
    source/   copy of source distribution
    target/   copy of target distribution
    tool/     unpacked migration tool
  wildfly41.0-to-wildfly42.0-patched/
    source/   source with before-fixtures applied
    target/   copy of target distribution
    tool/     unpacked migration tool
```

Directories are deleted before each run and **kept afterwards** for inspection
(logs, migrated configs, reports).

#### Skip behaviour

- If the **target** server is absent from the cache, the **entire test class**
  is skipped via `Assumptions.assumeTrue` in `@BeforeAll` — no red tests.
- If a specific **source** server is absent, only that parameterized pair
  is skipped.

This means the EAP modules can be included in a full reactor build without
breaking CI when EAP distributions have not been provided.

### 3 — Before-fixtures (`testsuite/before/dist/`)

The patched scenario copies the following assets into the source server before
running the migration:

| Asset | Source path | Destination in source server |
|---|---|---|
| Custom WARs | `before/dist/cmtool/` | `<source>/cmtool/` |
| System modules | `before/dist/modules-system/cmtool/` | `<source>/modules/system/layers/base/cmtool/` |
| Custom modules | `before/dist/modules-custom/cmtool/` | `<source>/modules/cmtool/` |
| Deployment content | `before/dist/content/` | `<source>/standalone/data/content/` and `domain/data/content/` |
| Standalone deployments | `before/dist/standalone-deployments/` | `<source>/standalone/deployments/` |
| `cmtool-standalone.xml` | copy of `standalone.xml` + patch applied | `<source>/standalone/configuration/cmtool-standalone.xml` |
| `cmtool-domain.xml` | copy of `domain.xml` + patch applied | `<source>/domain/configuration/cmtool-domain.xml` |

XML patches use a simple sed-like format (`s|pattern|replacement|`) and are
stored in `testsuite/common/src/main/resources/` per server family:

| Patch file | Used for |
|---|---|
| `cmtool-standalone-wildfly.xml.patch` | WildFly (all versions) standalone config |
| `cmtool-domain-wildfly.xml.patch` | WildFly (all versions) domain config |
| `cmtool-standalone-eap7.xml.patch` | EAP 6.4 / 7.x standalone config |
| `cmtool-domain-eap7.xml.patch` | EAP 6.4 / 7.x domain config |
| `cmtool-standalone-eap8.xml.patch` | EAP 8.x standalone config |
| `cmtool-domain-eap8.xml.patch` | EAP 8.x domain config |

---

## Properties

### Maven / Failsafe system properties (set by pom.xml)

| Property | Description | Default (relative to module) |
|---|---|---|
| `testsuite.serverCacheDir` | Persistent server cache directory | `testsuite/server-cache/servers` |

### Optional user properties (pass on the command line)

| Property                  | Description |
|---------------------------|---|
| `testsuite.eapServersDir` | Absolute path to a local directory containing EAP distributions (directories `jboss-eap-<version>/` or zip files `jboss-eap-<version>.zip`). Required for EAP integration tests. |

---

## Running the tests

### Full reactor (WildFly IT only, EAP skipped)

```bash
mvn install
```

The `testsuite/server-cache` module runs first and downloads/builds all required
WildFly distributions. The `testsuite/integration-tests/wildfly42.0` integration tests then execute.
EAP modules are present in the reactor but all their tests skip if required servers are not in the cache, or   
not in the path specified by`testsuite.eapServersDir`.

### Full reactor with EAP

```bash
mvn install -Dtestsuite.eapServersDir=/path/to/eap-distributions
```

The directory at `testsuite.eapServersDir` should contain one entry per required
major.minor version:

```
/path/to/eap-distributions/
  jboss-eap-7.4.0.GA/    (or jboss-eap-7.4.0.GA.zip)
  jboss-eap-8.0.0.GA/
  jboss-eap-8.1.0.GA/
  jboss-eap-8.2.0.GA/
  ...
```

Multiple qualifiers for the same major.minor are allowed — the cache updater
picks the `GA` one (or the most recently modified if none is named `GA`).

### Re-running only the integration tests (after a full build)

```bash
# WildFly 42.0 only
mvn verify -pl testsuite/wildfly42.0

# EAP 8.2 only
mvn verify -pl testsuite/eap8.2 -Dtestsuite.eapServersDir=/path/to/eap-distributions
```

### Refreshing the server cache

The cache survives `mvn clean`. To force a re-download or rebuild:

```bash
rm -rf testsuite/server-cache/servers
mvn install -pl testsuite/server-cache
```

---

## Shared modules

### `testsuite/server-cache`

Maven artifact: `jboss-server-migration-server-cache`

Produces a regular jar (consumed by IT modules for `ServerCacheLookup`) and runs
`WildFlyServerCache.main()` during the `generate-test-resources` phase.

Key classes:

| Class | Role |
|---|---|
| `WildFlyServerCache` | Entry point; discovers WildFly versions from `migrations/`, provisions each via GitHub or main-branch build |
| `EapServerCache` | Discovers required EAP versions from `migrations/`, copies the best candidate from `testsuite.eapServersDir` |
| `GitHubReleases` | Queries GitHub API for the latest `Final` release tag and downloads the distribution zip |
| `MainBranchBuilder` | Clones or updates `wildfly/wildfly:main`, runs `mvn -DskipTests install`, copies `dist/target/wildfly-*/` |
| `ServerCacheLookup` | Resolves `<prefix>-<MAJOR>.<MINOR>.*` cache entries; used by both the cache updater and IT tests |

### `testsuite/common`

Maven artifact: `jboss-server-migration-testsuite-common`

Produces a regular jar (not a test-jar) that all IT modules depend on at test scope.

Key class: `AbstractMigrationIT` — the base for every `*MigrationIT` subclass.
It provides:
- `@BeforeAll resolveSharedResources()` — resolves `cacheDir`, `toolZip`,
  `migrationsDir`, `migrationsRoot` from system properties; skips the entire class
  if the target server is absent from the cache.
- `runCleanMigration(String)` and `runPatchedMigration(String)` — the two test
  scenario implementations, called by each subclass's `@ParameterizedTest` methods.
- `discoverSourceVersions(targetDirName, sourcePrefix)` — static factory for
  `@MethodSource`; walks `migrations/<targetDirName>/<sourcePrefix>*` at runtime
  and returns a `Collection<String>` of major.minor version strings.
- All file-system utilities: `copyDirectory`, `deleteDirectory`, `unzipTool`,
  `applyPatch`, `applyBeforeFixtures`.

---

## Adding a new WildFly target version

Suppose WildFly 43.0 is released and a new target is needed.

1. **Use maintenance/add-server-wfly.sh to add the server, migrations, docs and integration-tests. The cache updater will automatically
   discover the new versions from the directory tree.

---

## Adding a new EAP target version

Suppose EAP 8.3 is released.

1. **Use maintenance/add-server-eap.sh to add the server, migrations, docs and integration-tests. The cache updater will automatically
   discover the new versions from the directory tree.
2. **Supply the EAP distributions** at build time:

   ```bash
   mvn install -Dtestsuite.eapServersDir=/path/to/eap-distributions
   ```

   The directory must contain `jboss-eap-8.3.0.GA/` (or a zip of the same name)
   along with all required source version distributions.

---

## CI

The GitHub Actions workflow (`.github/workflows/ci.yml`) runs `mvn install -Dtestsuite.serverCacheDir=./cache`
across a matrix of JDK × OSes. This covers:

- Unit tests for all production modules
- WildFly IT modules integration tests (server cache populated automatically from GitHub)
- EAP IT modules — **self-skip** because `testsuite.eapServersDir` is not set in CI. EAP integration tests must be run locally with access to EAP distributions.

The server cache is relocated due to Windows issues with too long filenames  