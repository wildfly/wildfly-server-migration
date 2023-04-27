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
import org.jboss.migration.core.env.MigrationEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The JBoss EAP 8.0 {@link org.jboss.migration.core.ServerProvider}.
 * @author emmartins
 */
public class EAPServerProvider8_0 extends EAPServerProvider7_4 {

    @Override
    protected String getProductVersionRegex() {
        return "8.0\\..*";
    }

    @Override
    protected Server constructServer(String migrationName, ProductInfo productInfo, Path baseDir, MigrationEnvironment migrationEnvironment) {
        return isXp(baseDir) ? new EAPXPServer8_0(migrationName, new ProductInfo("JBoss EAP XP", productInfo.getVersion()), baseDir, migrationEnvironment) :  new EAPServer8_0(migrationName, productInfo, baseDir, migrationEnvironment);
    }

    protected boolean isXp(Path baseDir) {
        if (Files.exists(baseDir.resolve(".installation").resolve("jboss-eap-xp-4.0.conf"))) {
            return true;
        }
        // fallback for internal XP builds
        if (Files.isDirectory(baseDir.resolve("modules").resolve("system").resolve("layers").resolve("microprofile").resolve("org").resolve("wildfly").resolve("extension").resolve("microprofile"))) {
            return true;
        }
        return false;
    }

    @Override
    public String getName() {
        return "JBoss EAP 8.0";
    }
}
