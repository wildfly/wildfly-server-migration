/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.wfly10.config.task.paths;

import org.jboss.migration.core.ProductInfo;
import org.jboss.migration.core.env.MigrationEnvironment;
import org.jboss.migration.core.jboss.JBossServer;

import java.nio.file.Path;

/**
 * Minimal {@link JBossServer} subclass for use in paths-migration unit tests.
 * @author emmartins
 */
public class TestJBossServerForPaths extends JBossServer<TestJBossServerForPaths> {

    private static final ProductInfo PRODUCT_INFO = new ProductInfo("TestServerForPaths", "test");

    public TestJBossServerForPaths(Path baseDir, MigrationEnvironment env) {
        super(PRODUCT_INFO.getName() + "Migration", PRODUCT_INFO, baseDir, env);
    }
}
