/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.wfly;

import org.jboss.migration.core.ProductInfo;
import org.jboss.migration.core.env.MigrationEnvironment;
import org.jboss.migration.core.jboss.JBossServer;
import org.jboss.migration.wfly10.ServiceLoaderWildFlyServerMigrations10;
import org.jboss.migration.wfly10.WildFlyServer10;
import org.jboss.migration.wfly10.WildFlyServerMigrations10;

import java.nio.file.Path;
import java.util.ServiceLoader;

/**
 * The WildFly 41.x Legacy Dist {@link org.jboss.migration.core.Server}.
 * @author emmartins
 */
public class WildFlyLegacyDist41_0Server extends WildFlyServer10 implements WildFly41_0Server {

    public static final JBossServer.Extensions EXTENSIONS = WildFlyLegacyDist40_0Server.EXTENSIONS;

    private static final WildFlyServerMigrations10 SERVER_MIGRATIONS = new ServiceLoaderWildFlyServerMigrations10<>(ServiceLoader.load(WildFly41_0ServerMigrationProvider.class));

    public WildFlyLegacyDist41_0Server(String migrationName, ProductInfo productInfo, Path baseDir, MigrationEnvironment migrationEnvironment) {
        super(migrationName, productInfo, baseDir, migrationEnvironment, EXTENSIONS);
    }

    @Override
    protected WildFlyServerMigrations10 getMigrations() {
        return SERVER_MIGRATIONS;
    }

    @Override
    public DistributionType getDistributionType() {
        return DistributionType.DIST;
    }

    @Override
    public TechnologyType getTechnologyType() {
        return TechnologyType.LEGACY;
    }
}
