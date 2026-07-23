/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.eap.test;

import org.jboss.migration.test.AbstractEAPMigrationIT;

import java.util.Collection;

/**
 * Integration tests for *-to-EAP-8.2 migrations.
 *
 * <p>For each supported source version (discovered at runtime from the migrations tree)
 * two scenarios are tested:</p>
 * <ol>
 *   <li><b>Clean</b> – migrate the unmodified default distribution (smoke test).</li>
 *   <li><b>Patched</b> – apply the cmtool "before" test fixtures then run the migration.</li>
 * </ol>
 *
 * <p>Tests self-skip when the required EAP distribution is absent from the server cache.
 * Populate the cache by passing {@code -Dtestsuite.eapServersDir=<dir>} to the build.</p>
 */
public class EAP8_2MigrationIT extends AbstractEAPMigrationIT {

    private static final String TARGET_VERSION = "8.2";

    static Collection<String> sourceVersions() {
        return discoverSourceVersions(SERVER_NAME_PREFIX + TARGET_VERSION);
    }

    @Override
    protected String targetVersion() {
        return TARGET_VERSION;
    }
}
