#!/bin/bash

# Script to add a new WildFly server version to the migration tool
# Usage:
#   ./add-server-wfly.sh                    (auto-detects next major.0 and latest as source)
#   ./add-server-wfly.sh 42.0 41.0          (explicit target and source versions)

set -e  # Exit on error

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
SERVERS_DIR="${PROJECT_ROOT}/servers"
MIGRATIONS_DIR="${PROJECT_ROOT}/migrations"
ROOT_POM="${PROJECT_ROOT}/pom.xml"
DIST_POM="${PROJECT_ROOT}/dist/standalone/pom.xml"

# Function to extract version components from major.minor format
# Args: version (e.g., "40.0")
# Sets: MAJOR, MINOR, MAJOR_MINOR (underscore format)
parse_version() {
    local version=$1
    MAJOR=$(echo "$version" | cut -d. -f1)
    MINOR=$(echo "$version" | cut -d. -f2)
    MAJOR_MINOR="${MAJOR}_${MINOR}"
}

# Function to apply version replacements in a directory
# Args:
#   $1 - directory path
#   $2 - from_version (e.g., "40.0")
#   $3 - to_version (e.g., "41.0")
apply_version_replacements() {
    local dir=$1
    local from_version=$2
    local to_version=$3

    # Parse versions
    parse_version "$from_version"
    local from_major=$MAJOR
    local from_minor=$MINOR
    local from_major_minor=$MAJOR_MINOR

    parse_version "$to_version"
    local to_major=$MAJOR
    local to_minor=$MINOR
    local to_major_minor=$MAJOR_MINOR

    # Replace content in files (excluding target directories)
    find "$dir" -type d -name target -prune -o -type f -print | while read -r file; do
        # Skip binary files
        if file "$file" | grep -q text; then
            sed -i '' "s/${from_major_minor}/${to_major_minor}/g" "$file"
            sed -i '' "s/${from_major}\.x/${to_major}.x/g" "$file"
            sed -i '' "s/${from_version}/${to_version}/g" "$file"
        fi
    done

    # Rename files (excluding target directories)
    find "$dir" -type d -name target -prune -o -depth -type f -print | while read -r file; do
        local file_dir=$(dirname "$file")
        local base=$(basename "$file")
        local new_base="$base"
        new_base=$(echo "$new_base" | sed "s/${from_major_minor}/${to_major_minor}/g")
        new_base=$(echo "$new_base" | sed "s/${from_version}/${to_version}/g")

        if [ "$base" != "$new_base" ]; then
            mv "$file" "$file_dir/$new_base"
        fi
    done
}

# Function to get all existing wildfly versions sorted numerically
# Returns: sorted list of versions in major.minor format
get_existing_versions() {
    find "$SERVERS_DIR" -maxdepth 1 -type d -name "wildfly*" | \
        sed 's|.*/wildfly||' | \
        grep -E '^[0-9]+\.[0-9]+$' | \
        sort -t. -k1,1n -k2,2n
}

# Function to get the highest existing version before a given version
# Args: target_version (e.g., "42.0")
# Returns: highest version before target, or empty if none
get_version_before() {
    local target=$1
    parse_version "$target"
    local target_major=$MAJOR
    local target_minor=$MINOR

    local versions=$(get_existing_versions)
    local result=""

    while IFS= read -r ver; do
        if [ -z "$ver" ]; then
            continue
        fi

        parse_version "$ver"
        local ver_major=$MAJOR
        local ver_minor=$MINOR

        # Check if this version is before target
        if [ "$ver_major" -lt "$target_major" ] || \
           { [ "$ver_major" -eq "$target_major" ] && [ "$ver_minor" -lt "$target_minor" ]; }; then
            result="$ver"
        fi
    done <<< "$versions"

    echo "$result"
}

# Function to get the latest existing version
# Returns: highest version in major.minor format
get_latest_version() {
    get_existing_versions | tail -1
}

# Parse arguments
if [ $# -eq 0 ]; then
    # No arguments: auto-detect
    LATEST_VERSION=$(get_latest_version)
    if [ -z "$LATEST_VERSION" ]; then
        echo "Error: No existing WildFly versions found in $SERVERS_DIR"
        exit 1
    fi

    parse_version "$LATEST_VERSION"
    NEXT_MAJOR=$((MAJOR + 1))
    TARGET_VERSION="${NEXT_MAJOR}.0"
    SOURCE_VERSION="$LATEST_VERSION"

    echo "Auto-detected target version: $TARGET_VERSION"
    echo "Auto-detected source version: $SOURCE_VERSION"
elif [ $# -eq 2 ]; then
    # Two arguments: explicit target and source
    TARGET_VERSION=$1
    SOURCE_VERSION=$2

    # Validate format
    if ! [[ "$TARGET_VERSION" =~ ^[0-9]+\.[0-9]+$ ]] || ! [[ "$SOURCE_VERSION" =~ ^[0-9]+\.[0-9]+$ ]]; then
        echo "Error: Versions must be in major.minor format (e.g., 42.0)"
        exit 1
    fi
else
    echo "Error: Script takes either 0 or 2 arguments"
    echo "Usage: $0 [target_version source_version]"
    echo "  No arguments: auto-detect next major.0 version and use latest as source"
    echo "  Two arguments: specify target and source versions (e.g., 42.0 41.0)"
    exit 1
fi

# Validate source version exists
if [ ! -d "$SERVERS_DIR/wildfly${SOURCE_VERSION}" ]; then
    echo "Error: Source version wildfly${SOURCE_VERSION} does not exist in $SERVERS_DIR"
    exit 1
fi

# Validate target version doesn't already exist
if [ -d "$SERVERS_DIR/wildfly${TARGET_VERSION}" ]; then
    echo "Error: Target version wildfly${TARGET_VERSION} already exists in $SERVERS_DIR"
    exit 1
fi

# Find the version before the source version
VERSION_BEFORE_SOURCE=$(get_version_before "$SOURCE_VERSION")
if [ -z "$VERSION_BEFORE_SOURCE" ]; then
    echo "Error: No version found before source version $SOURCE_VERSION"
    exit 1
fi

echo "========================================"
echo "Adding new WildFly server version"
echo "========================================"
echo "Target version: $TARGET_VERSION"
echo "Source version: $SOURCE_VERSION"
echo "Version before source: $VERSION_BEFORE_SOURCE"
echo "========================================"

# Parse all version components
parse_version "$TARGET_VERSION"
TARGET_MAJOR=$MAJOR
TARGET_MINOR=$MINOR
TARGET_MAJOR_MINOR=$MAJOR_MINOR

parse_version "$SOURCE_VERSION"
SOURCE_MAJOR=$MAJOR
SOURCE_MINOR=$MINOR
SOURCE_MAJOR_MINOR=$MAJOR_MINOR

parse_version "$VERSION_BEFORE_SOURCE"
BEFORE_MAJOR=$MAJOR
BEFORE_MINOR=$MINOR
BEFORE_MAJOR_MINOR=$MAJOR_MINOR

# Step 1: Copy server module
echo "Step 1: Copying server module..."
SRC_SERVER_DIR="$SERVERS_DIR/wildfly${SOURCE_VERSION}"
TGT_SERVER_DIR="$SERVERS_DIR/wildfly${TARGET_VERSION}"

cp -r "$SRC_SERVER_DIR" "$TGT_SERVER_DIR"
echo "  Copied $SRC_SERVER_DIR to $TGT_SERVER_DIR"

# Step 2: Replace version references in the new server module
echo "Step 2: Replacing version references in new server module..."
apply_version_replacements "$TGT_SERVER_DIR" "$SOURCE_VERSION" "$TARGET_VERSION"
echo "  Version replacements complete"

# Step 3: Add module to root pom.xml
echo "Step 3: Adding module to root pom.xml..."

# Find the line with the last wildfly server module
LAST_WILDFLY_LINE=$(grep -n "<module>servers/wildfly${SOURCE_VERSION}</module>" "$ROOT_POM" | tail -1 | cut -d: -f1)

if [ -z "$LAST_WILDFLY_LINE" ]; then
    echo "  Error: Could not find source version module in root pom.xml"
    exit 1
fi

# Insert new module after the last wildfly module
NEW_MODULE_LINE="        <module>servers/wildfly${TARGET_VERSION}</module>"
sed -i '' "${LAST_WILDFLY_LINE}a\\
${NEW_MODULE_LINE}
" "$ROOT_POM"

echo "  Added <module>servers/wildfly${TARGET_VERSION}</module> to root pom.xml"

# Step 4: Creating migration and docs modules will be done next,
# then dependencies will be added grouped by version in later steps

# Step 5: Clone migrations directory structure
echo "Step 5: Creating migration modules..."

SRC_MIGRATIONS_DIR="${MIGRATIONS_DIR}/wildfly${SOURCE_VERSION}"
TGT_MIGRATIONS_DIR="${MIGRATIONS_DIR}/wildfly${TARGET_VERSION}"

if [ ! -d "$SRC_MIGRATIONS_DIR" ]; then
    echo "  Warning: Source migrations directory not found: $SRC_MIGRATIONS_DIR"
    echo "  Skipping migration creation"
else
    # Copy entire migrations directory from source to target
    cp -r "$SRC_MIGRATIONS_DIR" "$TGT_MIGRATIONS_DIR"
    echo "  Copied migrations from wildfly${SOURCE_VERSION} to wildfly${TARGET_VERSION}"

    # Find the oldest migration (smallest version number) and remove it
    OLDEST_MIGRATION=$(ls "$TGT_MIGRATIONS_DIR" | grep "^wildfly" | sort -t. -k1,1n -k2,2n | head -1)

    if [ -n "$OLDEST_MIGRATION" ]; then
        rm -rf "${TGT_MIGRATIONS_DIR}/${OLDEST_MIGRATION}"
        echo "  Removed oldest migration: ${OLDEST_MIGRATION}"
    fi

    # Rename the original source version directory to target version
    if [ -d "${TGT_MIGRATIONS_DIR}/wildfly${SOURCE_VERSION}" ]; then
        mv "${TGT_MIGRATIONS_DIR}/wildfly${SOURCE_VERSION}" "${TGT_MIGRATIONS_DIR}/wildfly${TARGET_VERSION}"
        echo "  Renamed migration wildfly${SOURCE_VERSION} to wildfly${TARGET_VERSION}"
    fi

    # Copy wildfly<before-source> to wildfly<source>
    if [ -d "${TGT_MIGRATIONS_DIR}/wildfly${VERSION_BEFORE_SOURCE}" ]; then
        cp -r "${TGT_MIGRATIONS_DIR}/wildfly${VERSION_BEFORE_SOURCE}" "${TGT_MIGRATIONS_DIR}/wildfly${SOURCE_VERSION}"
        echo "  Copied migration wildfly${VERSION_BEFORE_SOURCE} to wildfly${SOURCE_VERSION}"
    fi


    # Apply version replacements to ALL migration files (excluding target directories)
    echo "  Applying source -> target version replacements to all migration files..."
    apply_version_replacements "${TGT_MIGRATIONS_DIR}" "$SOURCE_VERSION" "$TARGET_VERSION"

    # Apply version replacements in the wildfly<source> directory (before-source → source)
    echo "  Applying before source -> source version replacements in wildfly${SOURCE_VERSION}..."
    if [ -d "${TGT_MIGRATIONS_DIR}/wildfly${SOURCE_VERSION}" ]; then
        apply_version_replacements "${TGT_MIGRATIONS_DIR}/wildfly${SOURCE_VERSION}" "$VERSION_BEFORE_SOURCE" "$SOURCE_VERSION"
    fi

    echo "  Version replacements complete for migrations"
fi

# Step 6: Clone docs/user-guides/migrations directory structure
echo "Step 6: Creating migration user guide docs..."

DOCS_DIR="${PROJECT_ROOT}/docs/user-guides/migrations"
SRC_DOCS_MIGRATIONS_DIR="${DOCS_DIR}/wildfly${SOURCE_VERSION}"
TGT_DOCS_MIGRATIONS_DIR="${DOCS_DIR}/wildfly${TARGET_VERSION}"

if [ ! -d "$SRC_DOCS_MIGRATIONS_DIR" ]; then
    echo "  Warning: Source docs migrations directory not found: $SRC_DOCS_MIGRATIONS_DIR"
    echo "  Skipping docs creation"
else
    # Copy entire docs migrations directory from source to target
    cp -r "$SRC_DOCS_MIGRATIONS_DIR" "$TGT_DOCS_MIGRATIONS_DIR"
    echo "  Copied docs from wildfly${SOURCE_VERSION} to wildfly${TARGET_VERSION}"

    # Find the oldest migration doc and remove it
    OLDEST_DOC=$(ls "$TGT_DOCS_MIGRATIONS_DIR" | grep "^wildfly" | sort -t. -k1,1n -k2,2n | head -1)

    if [ -n "$OLDEST_DOC" ]; then
        rm -rf "${TGT_DOCS_MIGRATIONS_DIR}/${OLDEST_DOC}"
        echo "  Removed oldest doc: ${OLDEST_DOC}"
    fi

    # Rename the original source version doc directory to target version
    if [ -d "${TGT_DOCS_MIGRATIONS_DIR}/wildfly${SOURCE_VERSION}" ]; then
        mv "${TGT_DOCS_MIGRATIONS_DIR}/wildfly${SOURCE_VERSION}" "${TGT_DOCS_MIGRATIONS_DIR}/wildfly${TARGET_VERSION}"
        echo "  Renamed wildfly${SOURCE_VERSION} doc to wildfly${TARGET_VERSION}"
    fi

    # Copy wildfly<before-source> to wildfly<source>
    if [ -d "${TGT_DOCS_MIGRATIONS_DIR}/wildfly${VERSION_BEFORE_SOURCE}" ]; then
        cp -r "${TGT_DOCS_MIGRATIONS_DIR}/wildfly${VERSION_BEFORE_SOURCE}" "${TGT_DOCS_MIGRATIONS_DIR}/wildfly${SOURCE_VERSION}"
        echo "  Copied wildfly${VERSION_BEFORE_SOURCE} doc to wildfly${SOURCE_VERSION}"
    fi


    # Apply version replacements to ALL docs files, excluding target directories
    echo "  Applying source -> target version replacements to all docs files..."
    apply_version_replacements "${TGT_DOCS_MIGRATIONS_DIR}" "$SOURCE_VERSION" "$TARGET_VERSION"

    # Apply version replacements in the wildfly<source> directory (before-source → source), excluding target directories
    echo "  Applying before source -> source version replacements in wildfly${SOURCE_VERSION}..."
    if [ -d "${TGT_DOCS_MIGRATIONS_DIR}/wildfly${SOURCE_VERSION}" ]; then
        apply_version_replacements "${TGT_DOCS_MIGRATIONS_DIR}/wildfly${SOURCE_VERSION}" "$VERSION_BEFORE_SOURCE" "$SOURCE_VERSION"
    fi

    echo "  Version replacements complete for docs"
fi

# Step 7: Add migration modules to root pom.xml
echo "Step 7: Adding migration modules to root pom.xml..."

if [ -d "$TGT_DOCS_MIGRATIONS_DIR" ]; then
    # Find the last wildfly docs migration module line
    LAST_DOCS_MODULE_LINE=$(grep -n "<module>docs/user-guides/migrations/wildfly${SOURCE_VERSION}/wildfly${SOURCE_VERSION}</module>" "$ROOT_POM" | tail -1 | cut -d: -f1)

    if [ -n "$LAST_DOCS_MODULE_LINE" ]; then
        # Add all doc modules for the new target version
        for migration_dir in $(ls "$TGT_DOCS_MIGRATIONS_DIR" | grep "^wildfly" | sort -t. -k1,1n -k2,2n); do
            NEW_DOC_MODULE="        <module>docs/user-guides/migrations/wildfly${TARGET_VERSION}/${migration_dir}</module>"
            sed -i '' "${LAST_DOCS_MODULE_LINE}a\\
${NEW_DOC_MODULE}
" "$ROOT_POM"
            LAST_DOCS_MODULE_LINE=$((LAST_DOCS_MODULE_LINE + 1))
        done
        echo "  Added doc modules to root pom.xml"
    fi
fi

if [ -d "$TGT_MIGRATIONS_DIR" ]; then
    # Find the last wildfly migration module line
    LAST_MIGRATION_MODULE_LINE=$(grep -n "<module>migrations/wildfly${SOURCE_VERSION}/wildfly${SOURCE_VERSION}</module>" "$ROOT_POM" | tail -1 | cut -d: -f1)

    if [ -n "$LAST_MIGRATION_MODULE_LINE" ]; then
        # Add all migration modules for the new target version
        for migration_dir in $(ls "$TGT_MIGRATIONS_DIR" | grep "^wildfly" | sort -t. -k1,1n -k2,2n); do
            NEW_MIGRATION_MODULE="        <module>migrations/wildfly${TARGET_VERSION}/${migration_dir}</module>"
            sed -i '' "${LAST_MIGRATION_MODULE_LINE}a\\
${NEW_MIGRATION_MODULE}
" "$ROOT_POM"
            LAST_MIGRATION_MODULE_LINE=$((LAST_MIGRATION_MODULE_LINE + 1))
        done
        echo "  Added migration modules to root pom.xml"
    fi
fi

# Step 8: Add dependencies to dependencyManagement in root pom.xml (grouped by version)
echo "Step 8: Adding dependencies to dependencyManagement..."

# Find the LAST userguide dependency for the source version (end of the wfly source.0 section)
LAST_USERGUIDE_DEP_ARTIFACT=$(grep -n "<artifactId>jboss-server-migration-wildfly${SOURCE_VERSION}-to-wildfly${SOURCE_VERSION}-userguide</artifactId>" "$ROOT_POM" | tail -1 | cut -d: -f1)

if [ -n "$LAST_USERGUIDE_DEP_ARTIFACT" ]; then
    LAST_USERGUIDE_DEP_CLOSING=$(tail -n +$((LAST_USERGUIDE_DEP_ARTIFACT + 1)) "$ROOT_POM" | grep -n "</dependency>" | head -1 | cut -d: -f1)
    INSERT_LINE=$((LAST_USERGUIDE_DEP_ARTIFACT + LAST_USERGUIDE_DEP_CLOSING))

    # Insert comment and server dependency
    sed -i '' "${INSERT_LINE}a\\
            <!-- wfly ${TARGET_VERSION} -->\\
            <dependency>\\
                <groupId>\${project.groupId}</groupId>\\
                <artifactId>jboss-server-migration-wildfly${TARGET_VERSION}-server</artifactId>\\
                <version>\${project.version}</version>\\
            </dependency>
" "$ROOT_POM"
    INSERT_LINE=$((INSERT_LINE + 6))

    # Add all migration dependencies for this version
    if [ -d "$TGT_MIGRATIONS_DIR" ]; then
        for migration_dir in $(ls "$TGT_MIGRATIONS_DIR" | grep "^wildfly" | sort -t. -k1,1n -k2,2n); do
            migration_version=$(echo "$migration_dir" | sed 's/wildfly//')
            sed -i '' "${INSERT_LINE}a\\
            <dependency>\\
                <groupId>\${project.groupId}</groupId>\\
                <artifactId>jboss-server-migration-wildfly${migration_version}-to-wildfly${TARGET_VERSION}</artifactId>\\
                <version>\${project.version}</version>\\
            </dependency>
" "$ROOT_POM"
            INSERT_LINE=$((INSERT_LINE + 5))
        done
    fi

    # Add all userguide dependencies for this version
    if [ -d "$TGT_DOCS_MIGRATIONS_DIR" ]; then
        for migration_dir in $(ls "$TGT_DOCS_MIGRATIONS_DIR" | grep "^wildfly" | sort -t. -k1,1n -k2,2n); do
            migration_version=$(echo "$migration_dir" | sed 's/wildfly//')
            sed -i '' "${INSERT_LINE}a\\
            <dependency>\\
                <groupId>\${project.groupId}</groupId>\\
                <artifactId>jboss-server-migration-wildfly${migration_version}-to-wildfly${TARGET_VERSION}-userguide</artifactId>\\
                <version>\${project.version}</version>\\
            </dependency>
" "$ROOT_POM"
            INSERT_LINE=$((INSERT_LINE + 5))
        done
    fi

    echo "  Added dependencies to dependencyManagement (grouped by version)"
fi

# Step 9: Add dependencies to dist/standalone/pom.xml (grouped by version)
echo "Step 9: Adding dependencies to dist/standalone/pom.xml..."

if [ -d "$TGT_MIGRATIONS_DIR" ]; then
    # Find the last wildfly migration dependency for the source version
    LAST_DIST_MIGRATION=$(grep -n "<artifactId>jboss-server-migration-wildfly${SOURCE_VERSION}-to-wildfly${SOURCE_VERSION}</artifactId>" "$DIST_POM" | tail -1 | cut -d: -f1)

    if [ -n "$LAST_DIST_MIGRATION" ]; then
        LAST_DIST_MIGRATION_CLOSING=$(tail -n +$((LAST_DIST_MIGRATION + 1)) "$DIST_POM" | grep -n "</dependency>" | head -1 | cut -d: -f1)
        INSERT_DIST_LINE=$((LAST_DIST_MIGRATION + LAST_DIST_MIGRATION_CLOSING))

        # Add comment and server dependency
        sed -i '' "${INSERT_DIST_LINE}a\\
        <!-- wfly ${TARGET_VERSION} -->\\
        <dependency>\\
            <groupId>\${project.groupId}</groupId>\\
            <artifactId>jboss-server-migration-wildfly${TARGET_VERSION}-server</artifactId>\\
        </dependency>
" "$DIST_POM"
        INSERT_DIST_LINE=$((INSERT_DIST_LINE + 5))

        # Add all migration dependencies for this version
        for migration_dir in $(ls "$TGT_MIGRATIONS_DIR" | grep "^wildfly" | sort -t. -k1,1n -k2,2n); do
            migration_version=$(echo "$migration_dir" | sed 's/wildfly//')
            sed -i '' "${INSERT_DIST_LINE}a\\
        <dependency>\\
            <groupId>\${project.groupId}</groupId>\\
            <artifactId>jboss-server-migration-wildfly${migration_version}-to-wildfly${TARGET_VERSION}</artifactId>\\
        </dependency>
" "$DIST_POM"
            INSERT_DIST_LINE=$((INSERT_DIST_LINE + 4))
        done
        echo "  Added dependencies to dist/standalone/pom.xml (grouped by version)"
    fi
fi

# Step 10: Add userguide dependencies and executions to docs/user-guides/tool/standalone/pom.xml
echo "Step 10: Adding userguides to tool documentation..."

TOOL_DOC_POM="${PROJECT_ROOT}/docs/user-guides/tool/standalone/pom.xml"
if [ -f "$TOOL_DOC_POM" ] && [ -d "$TGT_DOCS_MIGRATIONS_DIR" ]; then
    # Find the last WFLY userguide dependency for the source version
    LAST_TOOL_DEP_ARTIFACT=$(grep -n "<artifactId>jboss-server-migration-wildfly${SOURCE_VERSION}-to-wildfly${SOURCE_VERSION}-userguide</artifactId>" "$TOOL_DOC_POM" | tail -1 | cut -d: -f1)

    if [ -n "$LAST_TOOL_DEP_ARTIFACT" ]; then
        LAST_TOOL_DEP_CLOSING=$(tail -n +$((LAST_TOOL_DEP_ARTIFACT + 1)) "$TOOL_DOC_POM" | grep -n "</dependency>" | head -1 | cut -d: -f1)
        TOOL_DEP_INSERT_LINE=$((LAST_TOOL_DEP_ARTIFACT + LAST_TOOL_DEP_CLOSING))

        # Add comment and all userguide dependencies
        sed -i '' "${TOOL_DEP_INSERT_LINE}a\\
        <!-- WFLY ${TARGET_VERSION} -->
" "$TOOL_DOC_POM"
        TOOL_DEP_INSERT_LINE=$((TOOL_DEP_INSERT_LINE + 1))

        for migration_dir in $(ls "$TGT_DOCS_MIGRATIONS_DIR" | grep "^wildfly" | sort -t. -k1,1n -k2,2n); do
            migration_version=$(echo "$migration_dir" | sed 's/wildfly//')
            sed -i '' "${TOOL_DEP_INSERT_LINE}a\\
        <dependency>\\
            <groupId>\${project.groupId}</groupId>\\
            <artifactId>jboss-server-migration-wildfly${migration_version}-to-wildfly${TARGET_VERSION}-userguide</artifactId>\\
        </dependency>
" "$TOOL_DOC_POM"
            TOOL_DEP_INSERT_LINE=$((TOOL_DEP_INSERT_LINE + 4))
        done
        echo "  Added userguide dependencies"
    fi

    # Find the last WFLY execution for the source version
    LAST_TOOL_EXEC=$(grep -n "<id>WFLY${SOURCE_VERSION}toWFLY${SOURCE_VERSION}</id>" "$TOOL_DOC_POM" | tail -1 | cut -d: -f1)

    if [ -n "$LAST_TOOL_EXEC" ]; then
        LAST_TOOL_EXEC_CLOSING=$(tail -n +$((LAST_TOOL_EXEC + 1)) "$TOOL_DOC_POM" | grep -n "</execution>" | head -1 | cut -d: -f1)
        TOOL_EXEC_INSERT_LINE=$((LAST_TOOL_EXEC + LAST_TOOL_EXEC_CLOSING))

        # Add comment and all executions
        sed -i '' "${TOOL_EXEC_INSERT_LINE}a\\
                    <!-- WFLY ${TARGET_VERSION} -->
" "$TOOL_DOC_POM"
        TOOL_EXEC_INSERT_LINE=$((TOOL_EXEC_INSERT_LINE + 1))

        for migration_dir in $(ls "$TGT_DOCS_MIGRATIONS_DIR" | grep "^wildfly" | sort -t. -k1,1n -k2,2n); do
            migration_version=$(echo "$migration_dir" | sed 's/wildfly//')
            # Convert version format for execution ID (e.g., 30.0 -> 30_0 -> 30.0 for ID)
            MIGRATION_MAJOR=$(echo "$migration_version" | cut -d. -f1)
            MIGRATION_MINOR=$(echo "$migration_version" | cut -d. -f2)
            sed -i '' "${TOOL_EXEC_INSERT_LINE}a\\
                    <execution>\\
                        <id>WFLY${MIGRATION_MAJOR}.${MIGRATION_MINOR}toWFLY${TARGET_MAJOR}.${TARGET_MINOR}</id>\\
                        <phase>generate-resources</phase>\\
                        <goals>\\
                            <goal>unpack-dependencies</goal>\\
                        </goals>\\
                        <configuration>\\
                            <includeArtifactIds>jboss-server-migration-wildfly${migration_version}-to-wildfly${TARGET_VERSION}-userguide</includeArtifactIds>\\
                            <outputDirectory>\${project.build.outputDirectory}/migrations/wildfly${migration_version}-to-wildfly${TARGET_VERSION}</outputDirectory>\\
                        </configuration>\\
                    </execution>
" "$TOOL_DOC_POM"
            TOOL_EXEC_INSERT_LINE=$((TOOL_EXEC_INSERT_LINE + 11))
        done
        echo "  Added userguide executions"
    fi
fi

# Step 11: Add links to master.adoc
echo "Step 11: Adding migration links to master.adoc..."

MASTER_ADOC="${PROJECT_ROOT}/docs/user-guides/tool/standalone/src/main/asciidoc/master.adoc"
if [ -f "$MASTER_ADOC" ] && [ -d "$TGT_DOCS_MIGRATIONS_DIR" ]; then
    # Find the last link mentioning the source version (as target)
    LAST_LINK_LINE=$(grep -n "to-wildfly${SOURCE_VERSION}/index.html" "$MASTER_ADOC" | tail -1 | cut -d: -f1)

    if [ -n "$LAST_LINK_LINE" ]; then
        INSERT_LINE=$LAST_LINK_LINE

        for migration_dir in $(ls "$TGT_DOCS_MIGRATIONS_DIR" | grep "^wildfly" | sort -t. -k1,1n -k2,2n); do
            migration_version=$(echo "$migration_dir" | sed 's/wildfly//')
            sed -i '' "${INSERT_LINE}a\\
* link:migrations/wildfly${migration_version}-to-wildfly${TARGET_VERSION}/index.html[WildFly ${migration_version} to WildFly ${TARGET_VERSION}]
" "$MASTER_ADOC"
            INSERT_LINE=$((INSERT_LINE + 1))
        done
        echo "  Added migration links to master.adoc"
    fi
fi

echo ""
echo "========================================"
echo "SUCCESS!"
echo "========================================"
echo "New WildFly ${TARGET_VERSION} server module created at:"
echo "  $TGT_SERVER_DIR"
if [ -d "$TGT_MIGRATIONS_DIR" ]; then
    echo "New migration modules created at:"
    echo "  $TGT_MIGRATIONS_DIR"
fi
if [ -d "$TGT_DOCS_MIGRATIONS_DIR" ]; then
    echo "New user guide docs created at:"
    echo "  $TGT_DOCS_MIGRATIONS_DIR"
fi
echo ""
echo "Next steps:"
echo "  1. Review the generated modules"
echo "  2. Update migration logic as needed for WildFly ${TARGET_VERSION}"
echo "  3. Build the project: mvn clean package"
echo "========================================"
