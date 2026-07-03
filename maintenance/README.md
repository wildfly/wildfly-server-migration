# Maintenance Scripts

This directory contains maintenance scripts for managing the JBoss Server Migration Tool project.

## Scripts

### add-server-wfly.sh

Adds a new WildFly server version to the migration tool.

**Usage:**
```bash
# Auto-detect next version and use latest as source
./maintenance/add-server-wfly.sh

# Explicit target and source versions
./maintenance/add-server-wfly.sh 42.0 41.0
```

**What it does:**
- Creates server module in `./servers/wildfly<version>/`
- Creates migration modules in `./migrations/wildfly<version>/`
- Creates user guide docs in `./docs/user-guides/migrations/wildfly<version>/`
- Updates all POM files with modules and dependencies
- Updates documentation with migration links

### remove-server-wfly.sh

Removes a WildFly server version from the migration tool.

**Usage:**
```bash
# Remove everything (server, migrations, docs)
./maintenance/remove-server-wfly.sh 41.0

# Remove only migrations and docs (keep server)
./maintenance/remove-server-wfly.sh 41.0 --migrations-only
```

**What it does:**
- Removes server/migration/doc modules and directories
- Removes all references from POM files
- Removes documentation links
- `--migrations-only` flag: keeps server module but removes migrations

### remove-server-eap.sh

Removes a JBoss EAP server version from the migration tool.

**Usage:**
```bash
# Remove everything (server, migrations, docs)
./maintenance/remove-server-eap.sh 8.2

# Remove only migrations and docs (keep server)
./maintenance/remove-server-eap.sh 8.2 --migrations-only
```

**What it does:**
- Removes server/migration/doc modules and directories
- Removes all references from POM files
- Removes documentation links
- `--migrations-only` flag: keeps server module but removes migrations

### add-server-eap.sh

Adds a new JBoss EAP server version to the migration tool.

**Usage:**
```bash
# Auto-detect next version and use latest as source
./maintenance/add-server-eap.sh

# Explicit target and source versions
./maintenance/add-server-eap.sh 9.0 8.2
```

**What it does:**
- Creates server module in `./servers/eap<version>/`
- Creates migration modules in `./migrations/eap<version>/`
- Creates user guide docs in `./docs/user-guides/migrations/eap<version>/`
- Updates all POM files with modules and dependencies
- Updates documentation with migration links

### set-project-version.sh

Sets the project version across all POM files and documentation.

**Usage:**
```bash
./maintenance/set-project-version.sh 42.0.0.Final
./maintenance/set-project-version.sh 42.0.0.Final-SNAPSHOT
```

**What it does:**
- Updates project version in root pom.xml
- Updates parent version in all child module pom.xml files (125+)
- Updates version references in README.md
- Updates version references in documentation files

## Examples

### Adding a new WildFly version
```bash
# Add WildFly 42.0 using 41.0 as source
./maintenance/add-server-wfly.sh 42.0 41.0

# Review changes
git status
git diff

# Build and test
mvn clean package
```

### Preparing a release
```bash
# Set version to release
./maintenance/set-project-version.sh 41.0.0.Final

# Build and verify
mvn clean package

# After release, bump to next SNAPSHOT
./maintenance/set-project-version.sh 42.0.0.Final-SNAPSHOT
```

### Removing old migrations
```bash
# Remove migrations for WildFly 30.0 but keep the server
./maintenance/remove-server-wfly.sh 30.0 --migrations-only

# Or remove everything including server
./maintenance/remove-server-wfly.sh 30.0
```
