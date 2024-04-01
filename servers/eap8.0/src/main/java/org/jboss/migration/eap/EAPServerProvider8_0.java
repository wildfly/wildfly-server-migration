/*
 * Copyright 2022 Red Hat, Inc.
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
package org.jboss.migration.eap;

import org.jboss.migration.core.ProductInfo;
import org.jboss.migration.core.Server;
import org.jboss.migration.core.ServerMigrationFailureException;
import org.jboss.migration.core.env.MigrationEnvironment;
import org.jboss.migration.core.jboss.JBossServer;
import org.jboss.migration.core.jboss.ManifestProductInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarInputStream;

/**
 * The JBoss EAP 8.0 {@link org.jboss.migration.core.ServerProvider}.
 * @author emmartins
 */
public class EAPServerProvider8_0 extends EAPServerProvider7_4 {

    @Override
    protected ProductInfo getProductInfo(Path baseDir, MigrationEnvironment migrationEnvironment) throws IllegalArgumentException, ServerMigrationFailureException {
        final JBossServer.Module module = new JBossServer.Modules(baseDir).getModule("org.jboss.as.product:main");
        if (module == null) {
            return null;
        }
        // Starting with EAP 8.0 GA, manifest is inside the module's jar
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
        return "8\\.0.*";
    }

    @Override
    protected Server constructServer(String migrationName, ProductInfo productInfo, Path baseDir, MigrationEnvironment migrationEnvironment) {
        final ManifestProductInfo xpManifestProductInfo = getXpManifestProductInfo(baseDir);
        return xpManifestProductInfo != null ? new EAPXPServer8_0(migrationName, new ProductInfo("JBoss EAP XP", xpManifestProductInfo.getVersion()), baseDir, migrationEnvironment) :  new EAPServer8_0(migrationName, productInfo, baseDir, migrationEnvironment);
    }

    protected ManifestProductInfo getXpManifestProductInfo(Path baseDir) {
        final JBossServer.Module module = new JBossServer.Modules(baseDir).getModule("org.jboss.eap.expansion.pack:main");
        if (module == null) {
            return null;
        }
        try {
            Path moduleJar = Files.list(module.getModuleDir()).filter(path -> path.toString().endsWith(".jar")).findFirst().get();
            if (moduleJar == null || !Files.isRegularFile(moduleJar)) {
                return null;
            }
            try (JarInputStream jarStream = new JarInputStream(Files.newInputStream(moduleJar))) {
                return ManifestProductInfo.from(jarStream.getManifest(), "Implementation-Title", "Implementation-Version");
            }
        } catch (IOException e) {
            throw new ServerMigrationFailureException(e);
        }
    }

    @Override
    public String getName() {
        return "JBoss EAP 8.0";
    }
}
