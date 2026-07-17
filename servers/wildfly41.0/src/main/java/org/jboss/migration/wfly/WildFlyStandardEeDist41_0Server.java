/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.wfly;

import org.jboss.migration.core.ProductInfo;
import org.jboss.migration.core.env.MigrationEnvironment;
import org.jboss.migration.wfly10.ServiceLoaderWildFlyServerMigrations10;
import org.jboss.migration.wfly10.WildFlyServerMigrations10;

import java.nio.file.Path;
import java.util.ServiceLoader;

/**
 * The WildFly 41.0 Standard EE Dist {@link org.jboss.migration.core.Server}.
 * @author emmartins
 */
public class WildFlyStandardEeDist41_0Server extends WildFly41_0Server {

    private static final WildFlyServerMigrations10 SERVER_MIGRATIONS = new ServiceLoaderWildFlyServerMigrations10<>(ServiceLoader.load(WildFly41_0ServerMigrationProvider.class));

    public WildFlyStandardEeDist41_0Server(String migrationName, ProductInfo productInfo, Path baseDir, MigrationEnvironment migrationEnvironment) {
        super(migrationName, productInfo, baseDir, migrationEnvironment);
    }

    @Override
    protected WildFlyServerMigrations10 getMigrations() {
        return SERVER_MIGRATIONS;
    }

    @Override
    public WildFly41_0Server.DistributionType getDistributionType() {
        return DistributionType.EE_DIST;
    }

    @Override
    public WildFly41_0Server.TechnologyType getTechnologyType() {
        return TechnologyType.STANDARD;
    }
}
