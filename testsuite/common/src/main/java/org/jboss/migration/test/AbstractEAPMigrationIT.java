/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Integration tests for migrations to EAP.
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
public abstract class AbstractEAPMigrationIT extends AbstractMigrationIT {

    public static final String SERVER_NAME_PREFIX = "eap";

    @ParameterizedTest(name = SERVER_NAME_PREFIX + "{0}")
    @MethodSource("sourceVersions")
    public void cleanMigration(String sourceVersion) throws Exception {
        runCleanMigration(sourceVersion);
    }

    @ParameterizedTest(name = SERVER_NAME_PREFIX + "{0}")
    @MethodSource("sourceVersions")
    public void patchedMigration(String sourceVersion) throws Exception {
        runPatchedMigration(sourceVersion);
    }

    @Override
    protected String serverNamePrefix() {
        return SERVER_NAME_PREFIX;
    }

    @Override
    protected PatchFamily patchFamily(String sourceVersion) {
        int major = Integer.parseInt(sourceVersion.split("\\.")[0]);
        return major < 8 ? PatchFamily.EAP7 : PatchFamily.EAP8;
    }
}
