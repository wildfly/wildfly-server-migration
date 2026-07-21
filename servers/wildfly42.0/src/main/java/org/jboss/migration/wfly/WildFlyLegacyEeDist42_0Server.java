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
 * The WildFly 42.0 Legacy EE Dist {@link org.jboss.migration.core.Server}.
 * @author emmartins
 */
public class WildFlyLegacyEeDist42_0Server extends WildFly42_0Server {

    private static final WildFlyServerMigrations10 SERVER_MIGRATIONS = new ServiceLoaderWildFlyServerMigrations10<>(ServiceLoader.load(WildFly42_0ServerMigrationProvider.class));

    public WildFlyLegacyEeDist42_0Server(String migrationName, ProductInfo productInfo, Path baseDir, MigrationEnvironment migrationEnvironment) {
        super(migrationName, productInfo, baseDir, migrationEnvironment);
    }

    @Override
    protected WildFlyServerMigrations10 getMigrations() {
        return SERVER_MIGRATIONS;
    }

    @Override
    public DistributionType getDistributionType() {
        return DistributionType.EE_DIST;
    }

    @Override
    public TechnologyType getTechnologyType() {
        return TechnologyType.LEGACY;
    }
}
