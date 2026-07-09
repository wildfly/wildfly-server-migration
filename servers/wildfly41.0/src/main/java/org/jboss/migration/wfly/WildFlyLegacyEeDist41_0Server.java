/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.wfly;

import org.jboss.migration.core.ProductInfo;
import org.jboss.migration.core.env.MigrationEnvironment;
import org.jboss.migration.core.jboss.JBossExtensionNames;
import org.jboss.migration.wfly10.ServiceLoaderWildFlyServerMigrations10;
import org.jboss.migration.wfly10.WildFlyServer10;
import org.jboss.migration.wfly10.WildFlyServerMigrations10;

import java.nio.file.Path;
import java.util.ServiceLoader;

/**
 * The WildFly 41.x Legacy EE Dist {@link org.jboss.migration.core.Server}.
 * @author emmartins
 */
public class WildFlyLegacyEeDist41_0Server extends WildFlyServer10 implements WildFly41_0Server {

    public static final Extensions EXTENSIONS = Extensions.builder()
            .extensionsExcept(WildFlyLegacyDist41_0Server.EXTENSIONS,
                    JBossExtensionNames.MICROMETER,
                    JBossExtensionNames.MICROPROFILE_CONFIG_SMALLRYE,
                    JBossExtensionNames.MICROPROFILE_FAULT_TOLERANCE_SMALLRYE,
                    JBossExtensionNames.MICROPROFILE_HEALTH_SMALLRYE,
                    JBossExtensionNames.MICROPROFILE_JWT_SMALLRYE,
                    JBossExtensionNames.MICROPROFILE_LRA_COORDINATOR,
                    JBossExtensionNames.MICROPROFILE_LRA_PARTICIPANT,
                    JBossExtensionNames.MICROPROFILE_OPENAPI_SMALLRYE,
                    JBossExtensionNames.MICROPROFILE_REACTIVE_MESSAGING_SMALLRYE,
                    JBossExtensionNames.MICROPROFILE_REACTIVE_STREAMS_OPERATORS_SMALLRYE,
                    JBossExtensionNames.MICROPROFILE_TELEMETRY,
                    JBossExtensionNames.OPENTELEMETRY
            ).build();

    private static final WildFlyServerMigrations10 SERVER_MIGRATIONS = new ServiceLoaderWildFlyServerMigrations10<>(ServiceLoader.load(WildFly41_0ServerMigrationProvider.class));

    public WildFlyLegacyEeDist41_0Server(String migrationName, ProductInfo productInfo, Path baseDir, MigrationEnvironment migrationEnvironment) {
        super(migrationName, productInfo, baseDir, migrationEnvironment, EXTENSIONS);
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
