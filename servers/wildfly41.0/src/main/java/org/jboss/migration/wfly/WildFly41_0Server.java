/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.wfly;

import org.jboss.migration.core.ProductInfo;
import org.jboss.migration.core.Server;
import org.jboss.migration.core.ServerMigrationFailureException;
import org.jboss.migration.core.env.MigrationEnvironment;
import org.jboss.migration.core.jboss.Extensions;
import org.jboss.migration.core.jboss.HostExcludes;
import org.jboss.migration.core.logger.ServerMigrationLogger;
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.wfly10.WildFlyServer10;

import java.nio.file.Path;

/**
 * The WildFly 41.0 {@link org.jboss.migration.core.Server}.
 * @author emmartins
 */
public abstract class WildFly41_0Server extends WildFlyServer10 implements WildFly40_0Server {

    private Extensions extensions;
    private HostExcludes hostExcludes;

    public WildFly41_0Server(String migrationName, ProductInfo productInfo, Path baseDir, MigrationEnvironment migrationEnvironment) {
        super(migrationName, productInfo, baseDir, migrationEnvironment);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void beforeMigration(Server source, TaskContext context) throws ServerMigrationFailureException {
        ServerMigrationLogger.ROOT_LOGGER.debugf("Discovering target server's supported extensions and subsystems...");
        extensions = Extensions.builder()
                .extensions(SupportedExtensionsDiscovery.discoverSupportedExtensions(this, getMigrationEnvironment()))
                .build();

        ServerMigrationLogger.ROOT_LOGGER.debugf("Discovering target server's host-excludes configuration...");
        try {
            hostExcludes = HostExcludesDiscovery.discoverHostExcludes(this, getMigrationEnvironment());
            ServerMigrationLogger.ROOT_LOGGER.debugf("Host Excludes discovered: %s", hostExcludes.getHostExcludes().toString());
        } catch (IllegalArgumentException e) {
            ServerMigrationLogger.ROOT_LOGGER.warnf("Failed to discover %s server's domain host-excludes config: %s", getMigrationName(), e.getMessage());
        }
    }

    @Override
    public Extensions getExtensions() {
        if (extensions == null) {
            throw new IllegalStateException("Extensions not available");
        }
        return extensions;
    }

    public HostExcludes getHostExcludes() {
        if (hostExcludes == null) {
            throw new IllegalStateException("HostExcludes not available");
        }
        return hostExcludes;
    }
}
