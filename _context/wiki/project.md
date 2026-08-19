# Project Overview

## What is this project?

See [`README.md`](../README.md) for the user-facing description, build instructions, and a sample migration run.

**GitHub:** https://github.com/wildfly/wildfly-server-migration  
**Issue tracker:** https://issues.redhat.com/projects/CMTOOL/issues

---

## Main goals and objectives

1. **Correctness** — every migrated configuration must boot successfully in the target server (verified by the integration testsuite via embedded WildFly/EAP boot checks).
2. **Extensibility** — adding support for a new WildFly or EAP target version should require minimal code and effort. The maintenance scripts (`maintenance/add-server-wfly.sh`, `maintenance/add-server-eap.sh`) scaffold new server, migrations, and integration-test modules automatically.
3. **Low maintenance overhead** — the integration testsuite discovers source/target version pairs dynamically from the `migrations/` directory tree, so no hardcoded lists need updating when versions are added or removed.

---

## Key stakeholders / users

- **External users** — WildFly and JBoss EAP operators who need to upgrade their server installations.
- **Red Hat / WildFly contributors** — maintain the tool and add support for new server releases.
- **CI** — GitHub Actions runs the full WildFly IT suite on every push (JDK 17 & 25 × Ubuntu & Windows).

---

## Repository layout

```
core/                    Core API/SPI (server, task, env, XML utilities)
servers/                 Server modules
  wildflyX.Y/            WildFly version X.Y server module
  eapX.Y/                JBoss EAP version X.Y server module
migrations/              Migration modules
  wildflyX2.Y2/          Migrations to WildFly X2.Y2
    wildflyX1.Y1/        Migration from WildFly X1.Y1 → WildFly X2.Y2
  eapX2.Y2/              Migrations to EAP X2.Y2
    eapX1.Y1/            Migration from EAP X1.Y1 → EAP X2.Y2
testsuite/               Integration testsuite (see testsuite/README.md)
cli/                     CLI entry point
dist/                    Distribution packaging
maintenance/             Shell scripts to add/remove server and migration modules
docs/                    User-facing documentation
```

---

## Important modules

### `core`

The foundation. Defines the main API/SPI interfaces every server module must implement:

| Package / class | Role |
|---|---|
| `core.Server` / `AbstractServer` | Server identity and resource discovery |
| `core.ServerProvider` / `AbstractServerProvider` | Detects a server from a filesystem path |
| `core.ServerMigration` | Orchestrates source → target migration |
| `core.task.*` | Task model: `ServerMigrationTask`, `TaskContext`, composite/leaf task builders |
| `core.jboss.*` | JBoss-specific abstractions: `JBossServer`, `JBossServerConfiguration`, `ModulesMigrationTask`, `XmlConfigurationMigration`, `Subsystem`, `Extension` |
| `core.env.*` | Environment / property system for task configuration and skipping |
| `core.util.xml.*` | Low-level XML DOM utilities used by configuration migration tasks |
| `core.report.*` | HTML, XML, and summary report writers |

### `servers/wildfly10.0`

The WildFly 10.0 server module introduced most of the SPI used by current migration and server modules.

### `servers/wildfly41.0`

The WildFly 41.0 server module introduced higher-level SPI elements that require less maintenance, such as auto-discovery of supported extensions/subsystems and domain host-excludes configuration. Use this as the **primary reference** when implementing a new server or migration module.

| Class | Role |
|---|---|
| `WildFly41_0Server` | Server implementation |
| `WildFly41_0ServerProvider` | Detects WildFly 41.0 from path |
| `WildFly41_0ServerMigrationProvider` | Wires together all migration tasks |
| `WildFly41_0AddHostExcludes` | Host-excludes migration task |
| `WildFly41_0MigrateReferencedPaths` | Resolvable-paths migration task |
| `WildFly41_0UpdateElytronSubsystem` | Elytron subsystem update task |
| `SupportedExtensionsDiscovery` / `HostExcludesDiscovery` | Discovery helpers shared across recent WildFly versions |

### `migrations/<target>/`

Each subdirectory represents a supported source-version migration for that target. The directory names drive both the testsuite (source version discovery) and the server cache population.

---

## Key workflows

### Adding or removing a WildFly or EAP server version

See [`maintenance/README.md`](../maintenance/README.md) — the maintenance scripts handle add/remove for both WildFly and EAP, and scaffold server modules, migration modules, docs, integration-tests, and all POM updates automatically.

### Running integration tests

See the [testsuite README](../testsuite/README.md#running-the-tests) for the full command reference, JDK compatibility matrix, and CI setup.

---

## Architecture notes

- Server identification uses the Java `ServiceLoader` mechanism: each server module registers its `ServerProvider` implementation via `META-INF/services/org.jboss.migration.core.ServerProvider`.
- Migration tasks form a tree; `CompositeTask` / `LeafTask` builders in `core.task.component` are the primary composition primitives.
- Environment properties (`core.env`) allow tasks to be skipped or configured without code changes — useful for non-interactive / automated migrations.
- `XmlConfigurationMigration` in `core.jboss` is the base for all XML-level configuration migrations; per-subsystem tasks extend it.

---

## Migrating paths referenced in configurations

### Overview

The tool copies files referenced by path attributes in XML configurations (keystores, certificate files, etc.) from the source server to the target server. The entry point is `WildFly41_0MigrateReferencedPaths` (in `servers/wildfly41.0`), which is registered as a subtask of every migration's server-configuration migration step.

### Class hierarchy

```
ConfigurationPathsMigrationTaskFactory        (servers/wildfly10.0 — task factory wired into the migration pipeline)
  └── WildFly41_0MigrateReferencedPaths       (servers/wildfly41.0 — composites the component factories below)

XmlConfigurationMigration.Component           (core.jboss — SPI; one implementation per path-source type)
  ├── VaultPathsMigration                     (servers/wildfly10.0 — legacy vault path)
  ├── WebSubsystemPathsMigration              (servers/wildfly10.0 — urn:jboss:domain:web ssl/@certificate-key-file, ssl/@ca-certificate-file)
  ├── ModclusterSubsystemPathsMigration       (servers/wildfly41.0 — urn:jboss:domain:modcluster ssl/@certificate-key-file, @ca-certificate-file, @ca-revocation-url)
  ├── ElytronOidcClientSubsystemPathsMigration(servers/wildfly41.0 — urn:wildfly:elytron-oidc-client: client-keystore/@file, truststore/@file, credential/@client-keystore-file, request-object-signing-keystore-file/@file)
  └── AttributesResolvablePathsMigration      (servers/wildfly41.0 — generic: any element with path+relative-to attributes)
```

### How `XmlConfigurationMigration.Component` works

Each component is a two-phase SAX-style visitor registered against a set of XML element local names:

1. **`getElementLocalNames()`** — returns the set of element names the component wants to visit. Use `XmlConfigurationMigration.ANY_ELEMENT_NAME` to visit every element (as `AttributesResolvablePathsMigration` does).
2. **`processElement(reader, source, target, context)`** — called once per matching element while the XML is being parsed. Collect attribute values into instance fields (e.g. `Set<String>`); do not execute tasks here.
3. **`afterProcessingElements(source, target, taskContext)`** — called once after the full XML has been parsed. Execute `MigrateResolvablePathTaskBuilder` subtasks for every collected path.

The `Factory` inner class (implements `XmlConfigurationMigration.ComponentFactory`) simply calls `new YourComponent()`, giving each XML file its own fresh component instance.

### Adding a new paths migration component

1. **Create** a new class in `servers/wildfly41.0/src/main/java/org/jboss/migration/wfly/task/paths/` implementing `XmlConfigurationMigration.Component`. Use `ModclusterSubsystemPathsMigration` or `ElytronOidcClientSubsystemPathsMigration` (subsystem-specific, non-standard attribute names) or `AttributesResolvablePathsMigration` (generic `path`/`relative-to`) as a template depending on how the paths are encoded in the XML.

2. **Key implementation choices:**
   - Filter by namespace URI prefix in `processElement` to avoid false matches from other subsystems that happen to use the same element local name (e.g. `ssl`).
   - Use `ResolvablePath.fromPathExpression(value)` when the attribute value is a standalone path expression; use `new ResolvablePath(path, relativeTo)` when both `path` and `relative-to` attributes are present.
   - Set `skipIfSourcePathDoesNotExists(true)` on `MigrateResolvablePathTaskBuilder` when the path might legitimately be absent in the source.

3. **Register** the new `Factory` in `WildFly41_0MigrateReferencedPaths` by adding `.componentFactory(new YourComponent.Factory())` to the `XmlConfigurationMigration.Builder` chain — before `AttributesResolvablePathsMigration.Factory` (which is the generic catch-all and should remain last). **Do not create a new `WildFlyNN_0MigrateReferencedPaths` subclass** for new components — adding to `WildFly41_0MigrateReferencedPaths` directly means the component is picked up by all existing and future migration providers at no extra cost.

4. **Add a unit test** by subclassing `AbstractXmlConfigurationMigrationComponentTest` (in `servers/wildfly10.0/src/test/`) — see the unit testing section below.

5. **Build** with `mvn clean install -pl servers/wildfly41.0 -am` to verify. The `subtask 3(always=fails)` test error in that module is intentional and pre-existing — a `BUILD SUCCESS` result means the code is correct.

### Unit testing a XML Configuration component

`AbstractXmlConfigurationMigrationComponentTest` (in `servers/wildfly10.0/src/test/`) is the base class for all component unit tests. It handles the full test lifecycle; a subclass only needs to implement three methods:

| Method | What to return |
|---|---|
| `getXmlConfig()` | XML string to parse (placed in the target server's config dir) |
| `getComponentFactory()` | `new YourComponent.Factory()` |
| `getExpectedCopiedPaths()` | Paths relative to the server base dir that must be copied (e.g. `"standalone/data/server.keystore"`) |

The base test builds real `JBossServerConfiguration` objects backed by `@TempDir` server trees, runs `XmlConfigurationMigration` through `TaskExecutionImpl`, and asserts every expected file was copied.

**Important:** Paths in the XML must be expressed as `${jboss.home.dir}/...` expressions so that `MigrateResolvablePathTaskRunnable` resolves them relative to the server base dir and performs the copy. Bare absolute paths are skipped with a warning.

**Module placement:**
- If the component lives in `servers/wildfly10.0` → add the test there directly.
- If the component lives in a later server module (e.g. `servers/wildfly41.0`) → depend on the `wildfly10.0` test-jar (`<type>test-jar</type>`, `<scope>test</scope>`) and place the test in that module's `src/test/`.
