#!/bin/sh
set -e
set -o pipefail

DIRNAME=`dirname "$0"`
TEST_DIR=`cd "$DIRNAME"; pwd`
TEST_BEFORE_DIR=$TEST_DIR/before/dist
TEST_AFTER_DIR=$TEST_DIR/after
TOOL_DIR="$TEST_DIR/jboss-server-migration"
SOURCE_DIST_DIR="$1"
TARGET_DIST_DIR="$2"

if [ "x$SOURCE_DIST_DIR" != "x" ]; then
    if [[ $SOURCE_DIST_DIR != /* ]]; then
        SOURCE_DIST_DIR="$TEST_DIR/$SOURCE_DIST_DIR"
    fi
else
    echo "### Usage: ./server-migration-simple-test.sh SOURCE_DIST_DIR TARGET_DIST_DIR"
    exit
fi

if [ ! -d $SOURCE_DIST_DIR ]; then
    echo "### Source Server base directory $SOURCE_DIST_DIR does not exists!"
    exit 1;
fi
echo "### Source Server base directory: $SOURCE_DIST_DIR"

if [ ! -d $TARGET_DIST_DIR ]; then
    echo "### Target Server dist directory $TARGET_DIST_DIR does not exists!"
    exit 1;
fi
echo "### Target Server dist directory: $TARGET_DIST_DIR"

echo "### Preparing JBoss Server Migration Tool binary..."
rm -Rf $TOOL_DIR
unzip $TEST_DIR/../dist/standalone/target/jboss-server-migration-*.zip -d $TEST_DIR

echo "### Executing the migration..."
$TOOL_DIR/jboss-server-migration.sh -n -s $SOURCE_DIST_DIR -t $TARGET_DIST_DIR  -Djboss.server.migration.modules.excludes=""