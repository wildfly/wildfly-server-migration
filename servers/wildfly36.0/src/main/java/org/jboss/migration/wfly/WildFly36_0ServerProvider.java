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
 * The WildFly 36.x {@link org.jboss.migration.core.ServerProvider}.
 *  @author emmartins
 */
public class WildFly36_0ServerProvider extends WildFly35_0ServerProvider {

    @Override
    protected String getProductNameRegex() {
        return "WildFly";
    }

    @Override
    protected String getProductVersionRegex() {
        return "36\\..*";
    }

    @Override
    protected Server constructServer(String migrationName, ProductInfo productInfo, Path baseDir, MigrationEnvironment migrationEnvironment) {
        return new WildFly36_0Server(migrationName, productInfo, baseDir, migrationEnvironment);
    }

    @Override
    public String getName() {
        return "WildFly 36.x";
    }
}
