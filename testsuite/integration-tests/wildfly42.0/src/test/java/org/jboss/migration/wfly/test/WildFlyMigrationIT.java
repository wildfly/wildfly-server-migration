/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.wfly.test;

import org.jboss.migration.test.AbstractMigrationIT;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collection;

/**
 * Integration tests for WildFly-to-WildFly 42.0 migrations.
 *
 * <p>For each supported source version (discovered at runtime from the migrations tree)
 * two scenarios are tested:</p>
 * <ol>
 *   <li><b>Clean</b> – migrate the unmodified default distribution (smoke test).</li>
 *   <li><b>Patched</b> – apply the cmtool "before" test fixtures then run the migration.</li>
 * </ol>
 *
 * <p>Both scenarios assert only that the migration tool exits with code 0.</p>
 */
public class WildFlyMigrationIT extends AbstractMigrationIT {

    static Collection<String> sourceVersions() {
        return discoverSourceVersions("wildfly42.0", "wildfly");
    }

    @ParameterizedTest(name = "wildfly{0}")
    @MethodSource("sourceVersions")
    public void cleanMigration(String sourceVersion) throws Exception {
        runCleanMigration(sourceVersion);
    }

    @ParameterizedTest(name = "wildfly{0}")
    @MethodSource("sourceVersions")
    public void patchedMigration(String sourceVersion) throws Exception {
        runPatchedMigration(sourceVersion);
    }

    @Override
    protected String targetVersion() {
        return "42.0";
    }

    @Override
    protected String targetCachePrefix() {
        return "wildfly";
    }

    @Override
    protected String sourceCachePrefix(String sourceVersion) {
        return "wildfly";
    }

    @Override
    protected PatchFamily patchFamily(String sourceVersion) {
        return PatchFamily.WILDFLY;
    }
}
