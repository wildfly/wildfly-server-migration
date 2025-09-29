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
 * The WildFly 38.x {@link org.jboss.migration.core.ServerProvider}.
 *  @author emmartins
 */
public class WildFly38_0ServerProvider extends WildFly37_0ServerProvider {

    @Override
    protected String getProductNameRegex() {
        return "WildFly( EE)?";
    }

    @Override
    protected String getProductVersionRegex() {
        return "38\\..*";
    }

    @Override
    protected Server constructServer(String migrationName, ProductInfo productInfo, Path baseDir, MigrationEnvironment migrationEnvironment) {
        return new WildFly38_0Server(migrationName, productInfo, baseDir, migrationEnvironment);
    }

    @Override
    public String getName() {
        return "WildFly 38.x";
    }
}
