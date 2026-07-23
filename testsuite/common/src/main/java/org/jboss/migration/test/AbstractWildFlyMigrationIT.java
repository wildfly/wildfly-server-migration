/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Integration tests for migrations to WildFly.
 *
 * <p>For each supported source version (discovered at runtime from the migrations tree)
 * two scenarios are tested:</p>
 * <ol>
 *   <li><b>Clean</b> – migrate the stock distribution (smoke test).</li>
 *   <li><b>Patched</b> – apply the cmtool "before" fixtures, then migrate.</li>
 * </ol>
 */
public abstract class AbstractWildFlyMigrationIT extends AbstractMigrationIT {

    public static final String SERVER_NAME_PREFIX = "wildfly";

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
        return PatchFamily.WILDFLY;
    }
}
