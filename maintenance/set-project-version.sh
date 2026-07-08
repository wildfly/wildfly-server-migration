#!/bin/bash

# Script to set the project version across all POM files
# Usage:
#   ./set-version.sh 42.0.0.Final

set -e  # Exit on error

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Parse arguments
if [ $# -ne 1 ]; then
    echo "Error: Script requires exactly 1 argument"
    echo "Usage: $0 <version>"
    echo "Example: $0 42.0.0.Final"
    echo "Example: $0 42.0.0.Final-SNAPSHOT"
    exit 1
fi

NEW_VERSION=$1

# Validate version format (should match Maven version format)
if ! [[ "$NEW_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.(Final|CR[0-9]+|Beta[0-9]+|Alpha[0-9]+)(-SNAPSHOT)?$ ]]; then
    echo "Warning: Version format doesn't match typical pattern (X.Y.Z.Qualifier[-SNAPSHOT])"
    echo "         Proceeding anyway with: $NEW_VERSION"
fi

echo "========================================"
echo "Setting project version to: $NEW_VERSION"
echo "========================================"

# Get current version from root pom.xml (the one after <artifactId>jboss-server-migration-parent</artifactId>)
CURRENT_VERSION=$(sed -n '/<artifactId>jboss-server-migration-parent<\/artifactId>/,/<version>/ {
    /<version>/ {
        s/.*<version>\(.*\)<\/version>.*/\1/
        p
        q
    }
}' "$PROJECT_ROOT/pom.xml" | tr -d '[:space:]')

if [ -z "$CURRENT_VERSION" ]; then
    echo "Error: Could not determine current version from root pom.xml"
    exit 1
fi

echo "Current version: $CURRENT_VERSION"
echo ""

if [ "$CURRENT_VERSION" = "$NEW_VERSION" ]; then
    echo "Version is already set to $NEW_VERSION"
    exit 0
fi

# Step 1: Update root pom.xml project version
echo "Step 1: Updating root pom.xml project version..."
# Update the <version> tag after <artifactId>jboss-server-migration-parent</artifactId>
sed -i '' "/<artifactId>jboss-server-migration-parent<\/artifactId>/,/<version>/ {
    s|<version>${CURRENT_VERSION}</version>|<version>${NEW_VERSION}</version>|
}" "$PROJECT_ROOT/pom.xml"
echo "  Updated root pom.xml"

# Step 2: Update all child pom.xml parent version references
echo "Step 2: Updating parent version in all child modules..."

UPDATED_COUNT=0

# Find all pom.xml files except the root and update them
for pom_file in $(find "$PROJECT_ROOT" -name "pom.xml" -not -path "$PROJECT_ROOT/pom.xml" -not -path "*/target/*"); do
    # Check if this pom has a parent with groupId org.jboss.migration
    if grep -q "<groupId>org.jboss.migration</groupId>" "$pom_file"; then
        # Update the version in the parent section
        sed -i '' "/<parent>/,/<\/parent>/ {
            s|<version>${CURRENT_VERSION}</version>|<version>${NEW_VERSION}</version>|
        }" "$pom_file"

        # Get relative path for display
        REL_PATH="${pom_file#$PROJECT_ROOT/}"
        echo "  Updated $REL_PATH"
        UPDATED_COUNT=$((UPDATED_COUNT + 1))
    fi
done

echo "  Updated $UPDATED_COUNT child module(s)"

# Step 3: Update version references in README.md and master.adoc
echo "Step 3: Updating version references in documentation..."

README_FILE="$PROJECT_ROOT/README.md"
MASTER_ADOC="$PROJECT_ROOT/docs/user-guides/tool/standalone/src/main/asciidoc/master.adoc"
DOCS_UPDATED=0

if [ -f "$README_FILE" ]; then
    # Update version references in README.md (e.g., jboss-server-migration-41.0.0.Final-SNAPSHOT.zip)
    if grep -q "jboss-server-migration-${CURRENT_VERSION}" "$README_FILE"; then
        sed -i '' "s/jboss-server-migration-${CURRENT_VERSION}/jboss-server-migration-${NEW_VERSION}/g" "$README_FILE"
        echo "  Updated README.md"
        DOCS_UPDATED=$((DOCS_UPDATED + 1))
    fi
fi

if [ -f "$MASTER_ADOC" ]; then
    # Update version references in master.adoc if any
    if grep -q "jboss-server-migration-${CURRENT_VERSION}" "$MASTER_ADOC"; then
        sed -i '' "s/jboss-server-migration-${CURRENT_VERSION}/jboss-server-migration-${NEW_VERSION}/g" "$MASTER_ADOC"
        echo "  Updated master.adoc"
        DOCS_UPDATED=$((DOCS_UPDATED + 1))
    fi
fi

if [ $DOCS_UPDATED -eq 0 ]; then
    echo "  No version-specific references found in documentation"
fi

echo ""
echo "========================================"
echo "SUCCESS!"
echo "========================================"
echo "Project version changed from:"
echo "  $CURRENT_VERSION"
echo "to:"
echo "  $NEW_VERSION"
echo ""
echo "Files updated:"
echo "  - Root pom.xml (project version)"
echo "  - $UPDATED_COUNT child module pom.xml files (parent version)"
if [ $DOCS_UPDATED -gt 0 ]; then
    echo "  - $DOCS_UPDATED documentation file(s)"
fi
echo ""
echo "Next steps:"
echo "  1. Review the changes with: git diff"
echo "  2. Build the project: mvn clean package"
echo "========================================"
