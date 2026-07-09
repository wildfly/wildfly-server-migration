/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.wfly;

import org.jboss.migration.core.ProductInfo;
import org.jboss.migration.core.Server;
import org.jboss.migration.core.env.MigrationEnvironment;
import org.jboss.migration.core.jboss.Extensions;
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.wfly10.WildFlyServer10;

import java.nio.file.Path;

/**
 * The WildFly 41.0 {@link org.jboss.migration.core.Server}.
 * @author emmartins
 */
public abstract class WildFly41_0Server extends WildFlyServer10 implements WildFly40_0Server {

    private Extensions extensions;

    public WildFly41_0Server(String migrationName, ProductInfo productInfo, Path baseDir, MigrationEnvironment migrationEnvironment) {
        super(migrationName, productInfo, baseDir, migrationEnvironment);
    }

    @Override
    protected void beforeMigration(Server source, TaskContext context) {
        extensions = Extensions.builder()
                .extensions(SupportedExtensionsDiscovery.discoverSupportedExtensions(this))
                .build();
    }

    @Override
    public Extensions getExtensions() {
        if (extensions == null) {
            throw new IllegalStateException("Extensions not available");
        }
        return extensions;
    }
}
