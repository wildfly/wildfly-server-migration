/*
 * Copyright 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.migration.wfly10.dist.full;

import org.jboss.migration.core.AbstractServerProvider;
import org.jboss.migration.core.ProductInfo;
import org.jboss.migration.core.Server;
import org.jboss.migration.core.env.MigrationEnvironment;
import org.jboss.migration.core.jboss.JBossServer;
import org.jboss.migration.core.jboss.ManifestProductInfo;

import java.nio.file.Path;

/**
 * The WildFly 10 Full {@link org.jboss.migration.core.ServerProvider}.
 *
 * @author emmartins
 */
public class WildFlyFullServerProvider10_0 extends AbstractServerProvider {

    protected ProductInfo getProductInfo(Path baseDir, MigrationEnvironment migrationEnvironment) throws IllegalArgumentException {
        final JBossServer.Module module = new JBossServer.Modules(baseDir).getModule(getModuleName());
        if (module == null) {
            return null;
        }
        final Path manifestPath = module.getModuleDir().resolve("dir").resolve("META-INF").resolve("MANIFEST.MF");
        final ManifestProductInfo productInfo = ManifestProductInfo.from(manifestPath);
        return productInfo;
    }

    protected String getModuleName() {
        return "org.jboss.as.product:wildfly-full";
    }

    @Override
    protected String getProductNameRegex() {
        return "WildFly Full";
    }

    @Override
    protected String getProductVersionRegex() {
        return "10.0\\..*";
    }

    @Override
    protected Server constructServer(String migrationName, ProductInfo productInfo, Path baseDir, MigrationEnvironment migrationEnvironment) {
        return new WildFlyFullServer10_0(migrationName, productInfo, baseDir, migrationEnvironment);
    }

    @Override
    public String getName() {
        return "WildFly 10.0";
    }
}
