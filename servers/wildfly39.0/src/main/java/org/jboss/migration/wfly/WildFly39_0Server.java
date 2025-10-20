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
 * The WildFly 39.0 {@link org.jboss.migration.core.Server}.
 * @author emmartins
 */
public class WildFly39_0Server extends WildFlyServer10 {

    public static final JBossServer.Extensions EXTENSIONS = WildFly38_0Server.EXTENSIONS;

    private static final WildFlyServerMigrations10 SERVER_MIGRATIONS = new ServiceLoaderWildFlyServerMigrations10<>(ServiceLoader.load(WildFly39_0ServerMigrationProvider.class));

    public WildFly39_0Server(String migrationName, ProductInfo productInfo, Path baseDir, MigrationEnvironment migrationEnvironment) {
        super(migrationName, productInfo, baseDir, migrationEnvironment, EXTENSIONS);
    }

    @Override
    protected WildFlyServerMigrations10 getMigrations() {
        return SERVER_MIGRATIONS;
    }
}
