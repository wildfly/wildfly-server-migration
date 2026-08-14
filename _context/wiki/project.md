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
