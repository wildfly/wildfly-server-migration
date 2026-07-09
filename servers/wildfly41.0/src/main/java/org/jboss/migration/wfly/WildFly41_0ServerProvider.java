/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.wfly;

import org.jboss.migration.core.ProductInfo;
import org.jboss.migration.core.Server;
import org.jboss.migration.core.env.MigrationEnvironment;

import java.nio.file.Path;

/**
 * The WildFly 41.0 {@link org.jboss.migration.core.ServerProvider}.
 *  @author emmartins
 */
public class WildFly41_0ServerProvider extends WildFly40_0ServerProvider {

    protected boolean isValidVersion(String version) {
        return version != null && version.startsWith("41.0");
    }

    protected Server constructStandardDistServer(String migrationName, String version, Path baseDir, MigrationEnvironment migrationEnvironment) {
        return new WildFlyStandardDist41_0Server(migrationName, new ProductInfo("WildFly Dist", version), baseDir, migrationEnvironment);
    }

    protected Server constructStandardEeDistServer(String migrationName, String version, Path baseDir, MigrationEnvironment migrationEnvironment) {
        return new WildFlyStandardEeDist41_0Server(migrationName, new ProductInfo("WildFly EE Dist", version), baseDir, migrationEnvironment);
    }

    protected Server constructLegacyDistServer(String migrationName, String version, Path baseDir, MigrationEnvironment migrationEnvironment) {
        return new WildFlyLegacyDist41_0Server(migrationName, new ProductInfo("WildFly Legacy Dist", version), baseDir, migrationEnvironment);
    }

    protected Server constructLegacyEeDistServer(String migrationName, String version, Path baseDir, MigrationEnvironment migrationEnvironment) {
        return new WildFlyLegacyEeDist41_0Server(migrationName, new ProductInfo("WildFly Legacy EE Dist", version), baseDir, migrationEnvironment);
    }

    public String getName() {
        return "WildFly 41.0";
    }
}
