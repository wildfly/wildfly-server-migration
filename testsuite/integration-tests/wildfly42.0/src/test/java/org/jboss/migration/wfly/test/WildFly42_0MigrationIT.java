/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.wfly.test;

import org.jboss.migration.test.AbstractWildFlyMigrationIT;

import java.util.Collection;

/**
 * Integration tests for WildFly-to-WildFly 42.0 migrations.
 *
 * <p>For each supported source version (discovered at runtime from the migrations tree)
 * two scenarios are tested:</p>
 * <ol>
 *   <li><b>Clean</b> – migrate the stock distribution (smoke test).</li>
 *   <li><b>Patched</b> – apply the cmtool "before" fixtures, then migrate.</li>
 * </ol>
 */
public class WildFly42_0MigrationIT extends AbstractWildFlyMigrationIT {

    private static final String TARGET_VERSION = "42.0";

    static Collection<String> sourceVersions() {
        return discoverSourceVersions(SERVER_NAME_PREFIX + TARGET_VERSION);
    }

    @Override
    protected String targetVersion() {
        return TARGET_VERSION;
    }
}
