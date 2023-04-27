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

SOURCE_DIST_CMTOOL_DIR=$SOURCE_DIST_DIR/cmtool
SOURCE_DIST_CMTOOL_MODULES_SYSTEM_DIR=$SOURCE_DIST_DIR/modules/system/layers/base/cmtool
SOURCE_DIST_CMTOOL_MODULES_CUSTOM_DIR=$SOURCE_DIST_DIR/modules/cmtool
SOURCE_DIST_STANDALONE_CONFIG_DIR=$SOURCE_DIST_DIR/standalone/configuration
SOURCE_DIST_STANDALONE_CONTENT_DIR=$SOURCE_DIST_DIR/standalone/data/content
SOURCE_DIST_STANDALONE_DEPLOYMENTS_DIR=$SOURCE_DIST_DIR/standalone/deployments
SOURCE_DIST_DOMAIN_CONFIG_DIR=$SOURCE_DIST_DIR/domain/configuration
SOURCE_DIST_DOMAIN_CONTENT_DIR=$SOURCE_DIST_DIR/domain/data/content

TARGET_DIST_CMTOOL_DIR=$TARGET_DIST_DIR/cmtool
TARGET_DIST_CMTOOL_MODULES_SYSTEM_DIR=$TARGET_DIST_DIR/modules/system/layers/base/cmtool
TARGET_DIST_CMTOOL_MODULES_CUSTOM_DIR=$TARGET_DIST_DIR/modules/cmtool
TARGET_DIST_STANDALONE_CONFIG_DIR=$TARGET_DIST_DIR/standalone/configuration
TARGET_DIST_STANDALONE_CONTENT_DIR=$TARGET_DIST_DIR/standalone/data/content
TARGET_DIST_STANDALONE_DEPLOYMENTS_DIR=$TARGET_DIST_DIR/standalone/deployments
TARGET_DIST_DOMAIN_CONFIG_DIR=$TARGET_DIST_DIR/domain/configuration
TARGET_DIST_DOMAIN_CONTENT_DIR=$TARGET_DIST_DIR/domain/data/content

echo "### Ensuring servers are in clean state..."
rm -Rf $SOURCE_DIST_CMTOOL_DIR
rm -Rf $TARGET_DIST_CMTOOL_DIR
rm -Rf $SOURCE_DIST_CMTOOL_MODULES_SYSTEM_DIR
rm -Rf $SOURCE_DIST_CMTOOL_MODULES_CUSTOM_DIR
rm -Rf $TARGET_DIST_CMTOOL_MODULES_SYSTEM_DIR
rm -Rf $TARGET_DIST_CMTOOL_MODULES_CUSTOM_DIR

for file in "$TEST_BEFORE_DIR"/content/*
do
  rm -Rf $SOURCE_DIST_STANDALONE_CONTENT_DIR/${file##*/}
  rm -Rf $SOURCE_DIST_DOMAIN_CONTENT_DIR/${file##*/}
  rm -Rf $TARGET_DIST_STANDALONE_CONTENT_DIR/${file##*/}
  rm -Rf $TARGET_DIST_DOMAIN_CONTENT_DIR/${file##*/}
done
for file in "$TEST_BEFORE_DIR"/standalone-deployments/*
do
  rm -Rf $SOURCE_DIST_STANDALONE_DEPLOYMENTS_DIR/${file##*/}
  rm -Rf $TARGET_DIST_STANDALONE_DEPLOYMENTS_DIR/${file##*/}
done
rm -Rf $SOURCE_DIST_STANDALONE_CONFIG_DIR/cmtool*
rm -Rf $SOURCE_DIST_DOMAIN_CONFIG_DIR/cmtool*
rm -Rf $TARGET_DIST_STANDALONE_CONFIG_DIR/cmtool*
rm -Rf $TARGET_DIST_DOMAIN_CONFIG_DIR/cmtool*

echo "### Installing test modules & deployments in source server..."
cp -Rf $TEST_BEFORE_DIR/cmtool $SOURCE_DIST_CMTOOL_DIR
cp -Rf $TEST_BEFORE_DIR/modules-system/cmtool $SOURCE_DIST_CMTOOL_MODULES_SYSTEM_DIR
cp -Rf $TEST_BEFORE_DIR/modules-custom/cmtool $SOURCE_DIST_CMTOOL_MODULES_CUSTOM_DIR
mkdir -p $SOURCE_DIST_STANDALONE_CONTENT_DIR
mkdir -p $SOURCE_DIST_DOMAIN_CONTENT_DIR
cp -Rf "$TEST_BEFORE_DIR"/content/ $SOURCE_DIST_STANDALONE_CONTENT_DIR/
cp -Rf "$TEST_BEFORE_DIR"/content/ $SOURCE_DIST_DOMAIN_CONTENT_DIR/
cp -Rf "$TEST_BEFORE_DIR"/standalone-deployments/ $SOURCE_DIST_STANDALONE_DEPLOYMENTS_DIR/

echo "### Setting up cmtool-standalone.xml and cmtool-domain.xml"
cp $SOURCE_DIST_STANDALONE_CONFIG_DIR/standalone.xml $SOURCE_DIST_STANDALONE_CONFIG_DIR/cmtool-standalone.xml
sed -f $TEST_BEFORE_DIR/cmtool-standalone.xml-wf25.patch -i '' $SOURCE_DIST_STANDALONE_CONFIG_DIR/cmtool-standalone.xml
cp $SOURCE_DIST_DOMAIN_CONFIG_DIR/domain.xml $SOURCE_DIST_DOMAIN_CONFIG_DIR/cmtool-domain.xml
sed -f $TEST_BEFORE_DIR/cmtool-domain.xml-wf25.patch -i '' $SOURCE_DIST_DOMAIN_CONFIG_DIR/cmtool-domain.xml

echo "### Executing the migration..."
$TOOL_DIR/jboss-server-migration.sh -n -s $SOURCE_DIST_DIR --target $TARGET_DIST_DIR -Djboss.server.migration.deployments.migrate-deployments.skip="false" -Djboss.server.migration.modules.includes="cmtool.module1" -Djboss.server.migration.modules.excludes="cmtool.module2,cmtool.module3"