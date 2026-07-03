#!/bin/bash

# Script to remove a JBoss EAP server version from the migration tool
# Usage:
#   ./remove-server-eap.sh 8.2          (removes JBoss EAP 8.2)

set -e  # Exit on error

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
SERVERS_DIR="${PROJECT_ROOT}/servers"
MIGRATIONS_DIR="${PROJECT_ROOT}/migrations"
DOCS_DIR="${PROJECT_ROOT}/docs/user-guides/migrations"
ROOT_POM="${PROJECT_ROOT}/pom.xml"
DIST_POM="${PROJECT_ROOT}/dist/standalone/pom.xml"
TOOL_POM="${PROJECT_ROOT}/docs/user-guides/tool/standalone/pom.xml"
MASTER_ADOC="${PROJECT_ROOT}/docs/user-guides/tool/standalone/src/main/asciidoc/master.adoc"

# Function to extract version components from major.minor format
# Args: version (e.g., "8.2")
# Sets: MAJOR, MINOR, MAJOR_MINOR (underscore format)
parse_version() {
    local version=$1
    MAJOR=$(echo "$version" | cut -d. -f1)
    MINOR=$(echo "$version" | cut -d. -f2)
    MAJOR_MINOR="${MAJOR}_${MINOR}"
}

# Parse arguments
MIGRATIONS_ONLY=false

if [ $# -lt 1 ] || [ $# -gt 2 ]; then
    echo "Error: Script requires 1 or 2 arguments"
    echo "Usage: $0 <version> [--migrations-only]"
    echo "Example: $0 8.2"
    echo "Example: $0 8.2 --migrations-only  (removes only migrations and docs)"
    exit 1
fi

VERSION=$1

if [ $# -eq 2 ]; then
    if [ "$2" = "--migrations-only" ]; then
        MIGRATIONS_ONLY=true
    else
        echo "Error: Unknown flag '$2'"
        echo "Usage: $0 <version> [--migrations-only]"
        exit 1
    fi
fi

# Validate format
if ! [[ "$VERSION" =~ ^[0-9]+\.[0-9]+$ ]]; then
    echo "Error: Version must be in major.minor format (e.g., 8.2)"
    exit 1
fi

# Parse version components
parse_version "$VERSION"

echo "========================================"
echo "Removing JBoss EAP $VERSION"
echo "========================================"

# Define directories
SERVER_DIR="${SERVERS_DIR}/eap${VERSION}"
MIGRATIONS_VERSION_DIR="${MIGRATIONS_DIR}/eap${VERSION}"
DOCS_VERSION_DIR="${DOCS_DIR}/eap${VERSION}"

# Check if at least one directory exists
if $MIGRATIONS_ONLY; then
    # When skipping server, only check migrations and docs
    if [ ! -d "$MIGRATIONS_VERSION_DIR" ] && [ ! -d "$DOCS_VERSION_DIR" ]; then
        echo "Error: No migration or doc directories found for JBoss EAP ${VERSION}"
        echo "  Checked: $MIGRATIONS_VERSION_DIR"
        echo "  Checked: $DOCS_VERSION_DIR"
        exit 1
    fi
else
    # When not skipping server, check all three
    if [ ! -d "$SERVER_DIR" ] && [ ! -d "$MIGRATIONS_VERSION_DIR" ] && [ ! -d "$DOCS_VERSION_DIR" ]; then
        echo "Error: No directories found for JBoss EAP ${VERSION}"
        echo "  Checked: $SERVER_DIR"
        echo "  Checked: $MIGRATIONS_VERSION_DIR"
        echo "  Checked: $DOCS_VERSION_DIR"
        exit 1
    fi
fi

# Step 1: Remove server module from root pom.xml
if $MIGRATIONS_ONLY; then
    echo "Step 1: Skipping server module removal (--migrations-only flag)"
else
    echo "Step 1: Removing server module from root pom.xml..."
    if grep -q "<module>servers/eap${VERSION}</module>" "$ROOT_POM"; then
        sed -i '' "/<module>servers\/eap${VERSION}<\/module>/d" "$ROOT_POM"
        echo "  Removed server module"
    else
        echo "  Server module not found (skipping)"
    fi
fi

# Step 2: Remove migration modules from root pom.xml
echo "Step 2: Removing migration modules from root pom.xml..."
if [ -d "$MIGRATIONS_VERSION_DIR" ]; then
    for migration_dir in $(ls "$MIGRATIONS_VERSION_DIR" 2>/dev/null | grep "^eap" || true); do
        if grep -q "<module>migrations/eap${VERSION}/${migration_dir}</module>" "$ROOT_POM"; then
            sed -i '' "/<module>migrations\/eap${VERSION}\/${migration_dir}<\/module>/d" "$ROOT_POM"
        fi
    done
    echo "  Removed migration modules"
else
    echo "  No migration modules to remove"
fi

# Step 3: Remove doc modules from root pom.xml
echo "Step 3: Removing doc modules from root pom.xml..."
if [ -d "$DOCS_VERSION_DIR" ]; then
    for doc_dir in $(ls "$DOCS_VERSION_DIR" 2>/dev/null | grep "^eap" || true); do
        migration_version=$(echo "$doc_dir" | sed 's/eap//')
        if grep -q "<module>docs/user-guides/migrations/eap${VERSION}/eap${migration_version}</module>" "$ROOT_POM"; then
            sed -i '' "/<module>docs\/user-guides\/migrations\/eap${VERSION}\/eap${migration_version}<\/module>/d" "$ROOT_POM"
        fi
    done
    echo "  Removed doc modules"
else
    echo "  No doc modules to remove"
fi

# Step 4: Remove dependencies from root pom.xml (dependencyManagement)
echo "Step 4: Removing dependencies from dependencyManagement..."

# Remove comment line (only if not skipping server, as comment belongs to version block)
if ! $MIGRATIONS_ONLY; then
    sed -i '' "/<!-- eap ${VERSION} -->/d" "$ROOT_POM"
fi

# Helper function to remove a dependency from a POM file
# Args: $1 = pom file, $2 = artifact pattern to match
remove_dependency() {
    local pom_file=$1
    local artifact_pattern=$2

    # Find the line with the artifactId
    local artifact_line=$(grep -n "${artifact_pattern}<\/artifactId>" "$pom_file" | cut -d: -f1)

    if [ -n "$artifact_line" ]; then
        # Find the opening <dependency> tag before this line
        local dep_start=$(tail -n +1 "$pom_file" | head -n $((artifact_line - 1)) | grep -n "<dependency>" | tail -1 | cut -d: -f1)

        if [ -n "$dep_start" ]; then
            # Find the closing </dependency> tag after the artifactId line
            local dep_end_offset=$(tail -n +$((artifact_line + 1)) "$pom_file" | grep -n "</dependency>" | head -1 | cut -d: -f1)

            if [ -n "$dep_end_offset" ]; then
                local dep_end=$((artifact_line + dep_end_offset))
                # Delete from dep_start to dep_end
                sed -i '' "${dep_start},${dep_end}d" "$pom_file"
            fi
        fi
    fi
}

# Remove server dependency (unless skipping server)
if ! $MIGRATIONS_ONLY; then
    if grep -q "jboss-server-migration-eap${VERSION}-server" "$ROOT_POM"; then
        remove_dependency "$ROOT_POM" "jboss-server-migration-eap${VERSION}-server"
    fi
fi

# Remove migration dependencies (to this version)
if [ -d "$MIGRATIONS_VERSION_DIR" ]; then
    for migration_dir in $(ls "$MIGRATIONS_VERSION_DIR" 2>/dev/null | grep "^eap" || true); do
        migration_version=$(echo "$migration_dir" | sed 's/eap//')
        if grep -q "jboss-server-migration-eap${migration_version}-to-eap${VERSION}" "$ROOT_POM"; then
            remove_dependency "$ROOT_POM" "jboss-server-migration-eap${migration_version}-to-eap${VERSION}"
        fi
    done
fi

# Remove userguide dependencies (to this version)
if [ -d "$DOCS_VERSION_DIR" ]; then
    for doc_dir in $(ls "$DOCS_VERSION_DIR" 2>/dev/null | grep "^eap" || true); do
        migration_version=$(echo "$doc_dir" | sed 's/eap//')
        if grep -q "jboss-server-migration-eap${migration_version}-to-eap${VERSION}-userguide" "$ROOT_POM"; then
            remove_dependency "$ROOT_POM" "jboss-server-migration-eap${migration_version}-to-eap${VERSION}-userguide"
        fi
    done
fi

echo "  Removed dependencies from dependencyManagement"

# Step 5: Remove dependencies from dist/standalone/pom.xml
echo "Step 5: Removing dependencies from dist/standalone/pom.xml..."

# Remove comment (only if not skipping server)
if ! $MIGRATIONS_ONLY; then
    sed -i '' "/<!-- eap ${VERSION} -->/d" "$DIST_POM"
fi

# Remove server dependency (unless skipping server)
if ! $MIGRATIONS_ONLY; then
    if grep -q "jboss-server-migration-eap${VERSION}-server" "$DIST_POM"; then
        remove_dependency "$DIST_POM" "jboss-server-migration-eap${VERSION}-server"
    fi
fi

# Remove migration dependencies
if [ -d "$MIGRATIONS_VERSION_DIR" ]; then
    for migration_dir in $(ls "$MIGRATIONS_VERSION_DIR" 2>/dev/null | grep "^eap" || true); do
        migration_version=$(echo "$migration_dir" | sed 's/eap//')
        if grep -q "jboss-server-migration-eap${migration_version}-to-eap${VERSION}" "$DIST_POM"; then
            remove_dependency "$DIST_POM" "jboss-server-migration-eap${migration_version}-to-eap${VERSION}"
        fi
    done
fi

echo "  Removed dependencies from dist/standalone/pom.xml"

# Step 6: Remove userguide dependencies and executions from tool pom.xml
echo "Step 6: Removing userguides from tool documentation..."

if [ -f "$TOOL_POM" ]; then
    # Remove comment
    sed -i '' "/<!-- EAP ${VERSION} -->/d" "$TOOL_POM"

    # Remove userguide dependencies
    if [ -d "$DOCS_VERSION_DIR" ]; then
        for doc_dir in $(ls "$DOCS_VERSION_DIR" 2>/dev/null | grep "^eap" || true); do
            migration_version=$(echo "$doc_dir" | sed 's/eap//')
            if grep -q "jboss-server-migration-eap${migration_version}-to-eap${VERSION}-userguide" "$TOOL_POM"; then
                remove_dependency "$TOOL_POM" "jboss-server-migration-eap${migration_version}-to-eap${VERSION}-userguide"
            fi
        done
    fi

    # Remove executions
    if [ -d "$DOCS_VERSION_DIR" ]; then
        for doc_dir in $(ls "$DOCS_VERSION_DIR" 2>/dev/null | grep "^eap" || true); do
            migration_version=$(echo "$doc_dir" | sed 's/eap//')
            # Parse migration version for ID format
            parse_version "$migration_version"
            MIGRATION_MAJOR=$MAJOR
            MIGRATION_MINOR=$MINOR

            # Parse target version for ID format
            parse_version "$VERSION"
            TARGET_MAJOR=$MAJOR
            TARGET_MINOR=$MINOR

            # Remove execution block by ID
            if grep -q "<id>EAP${MIGRATION_MAJOR}.${MIGRATION_MINOR}toEAP${TARGET_MAJOR}.${TARGET_MINOR}</id>" "$TOOL_POM"; then
                # Find line with the execution id
                EXEC_LINE=$(grep -n "<id>EAP${MIGRATION_MAJOR}.${MIGRATION_MINOR}toEAP${TARGET_MAJOR}.${TARGET_MINOR}</id>" "$TOOL_POM" | cut -d: -f1)
                if [ -n "$EXEC_LINE" ]; then
                    # Find the <execution> opening tag before this line
                    EXEC_START=$(tail -n +1 "$TOOL_POM" | head -n $((EXEC_LINE - 1)) | grep -n "<execution>" | tail -1 | cut -d: -f1)
                    if [ -n "$EXEC_START" ]; then
                        # Find the </execution> closing tag after the id line
                        EXEC_END=$(tail -n +$((EXEC_LINE + 1)) "$TOOL_POM" | grep -n "</execution>" | head -1 | cut -d: -f1)
                        if [ -n "$EXEC_END" ]; then
                            EXEC_END=$((EXEC_LINE + EXEC_END))
                            # Delete from EXEC_START to EXEC_END
                            sed -i '' "${EXEC_START},${EXEC_END}d" "$TOOL_POM"
                        fi
                    fi
                fi
            fi
        done
    fi

    echo "  Removed userguides from tool documentation"
else
    echo "  Tool pom.xml not found (skipping)"
fi

# Step 7: Remove migration links from master.adoc
echo "Step 7: Removing migration links from master.adoc..."

if [ -f "$MASTER_ADOC" ] && [ -d "$DOCS_VERSION_DIR" ]; then
    for doc_dir in $(ls "$DOCS_VERSION_DIR" 2>/dev/null | grep "^eap" || true); do
        migration_version=$(echo "$doc_dir" | sed 's/eap//')
        # Remove the link line
        sed -i '' "/link:migrations\/eap${migration_version}-to-eap${VERSION}\/index.html\[JBoss EAP ${migration_version} to JBoss EAP ${VERSION}\]/d" "$MASTER_ADOC"
    done
    echo "  Removed migration links from master.adoc"
else
    echo "  master.adoc not found or no docs to remove (skipping)"
fi

# Step 8: Remove directories
echo "Step 8: Removing directories..."

if ! $MIGRATIONS_ONLY; then
    if [ -d "$SERVER_DIR" ]; then
        rm -rf "$SERVER_DIR"
        echo "  Removed $SERVER_DIR"
    fi
fi

if [ -d "$MIGRATIONS_VERSION_DIR" ]; then
    rm -rf "$MIGRATIONS_VERSION_DIR"
    echo "  Removed $MIGRATIONS_VERSION_DIR"
fi

if [ -d "$DOCS_VERSION_DIR" ]; then
    rm -rf "$DOCS_VERSION_DIR"
    echo "  Removed $DOCS_VERSION_DIR"
fi

echo ""
echo "========================================"
echo "SUCCESS!"
echo "========================================"
if $MIGRATIONS_ONLY; then
    echo "JBoss EAP ${VERSION} migrations and docs have been removed from:"
    echo "  - Migration modules"
    echo "  - Documentation modules"
    echo "  - POM files (migrations and docs only)"
    echo "  - Master documentation"
else
    echo "JBoss EAP ${VERSION} has been removed from:"
    echo "  - Server modules"
    echo "  - Migration modules"
    echo "  - Documentation modules"
    echo "  - All POM files"
    echo "  - Master documentation"
fi
echo ""
echo "Next steps:"
echo "  1. Review the changes with: git status"
echo "  2. Build the project: mvn clean package"
echo "========================================"
