/*
 * Copyright 2024 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.migration.wfly;

import org.jboss.migration.core.ProductInfo;
import org.jboss.migration.core.Server;
import org.jboss.migration.core.ServerMigrationFailureException;
import org.jboss.migration.core.env.MigrationEnvironment;
import org.jboss.migration.core.jboss.JBossServer;
import org.jboss.migration.core.jboss.ManifestProductInfo;
import org.jboss.migration.wfly10.dist.full.WildFlyFullServerProvider10_0;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarInputStream;

/**
 * The WildFly 32.x {@link org.jboss.migration.core.ServerProvider}.
 *  @author emmartins
 */
public class WildFly32_0ServerProvider extends WildFlyFullServerProvider10_0 {

    @Override
    protected ProductInfo getProductInfo(Path baseDir, MigrationEnvironment migrationEnvironment) throws IllegalArgumentException {
        final JBossServer.Module module = new JBossServer.Modules(baseDir).getModule("org.jboss.as.product:main");
        if (module == null) {
            return null;
        }
        try {
            Path moduleJar = Files.list(module.getModuleDir()).filter(path -> path.toString().endsWith(".jar")).findFirst().get();
            if (moduleJar == null || !Files.isRegularFile(moduleJar)) {
                return null;
            }
            try (JarInputStream jarStream = new JarInputStream(Files.newInputStream(moduleJar))) {
                return ManifestProductInfo.from(jarStream.getManifest());
            }
        } catch (IOException e) {
            throw new ServerMigrationFailureException(e);
        }
    }

    @Override
    protected String getProductVersionRegex() {
        return "32\\..*";
    }

    @Override
    protected Server constructServer(String migrationName, ProductInfo productInfo, Path baseDir, MigrationEnvironment migrationEnvironment) {
        return new WildFly32_0Server(migrationName, productInfo, baseDir, migrationEnvironment);
    }

    @Override
    public String getName() {
        return "WildFly 32.x";
    }
}
