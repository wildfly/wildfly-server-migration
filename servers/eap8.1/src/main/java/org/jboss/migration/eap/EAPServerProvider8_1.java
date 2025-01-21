/*
 * Copyright 2025 Red Hat, Inc.
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
import org.jboss.migration.core.jboss.ManifestProductInfo;

import java.nio.file.Path;

/**
 * The JBoss EAP 8.1 {@link org.jboss.migration.core.ServerProvider}.
 * @author emmartins
 */
public class EAPServerProvider8_1 extends EAPServerProvider8_0 {

    @Override
    protected String getProductVersionRegex() {
        return "8\\.1.*";
    }

    @Override
    protected Server constructServer(String migrationName, ProductInfo productInfo, Path baseDir, MigrationEnvironment migrationEnvironment) {
        final ManifestProductInfo xpManifestProductInfo = getXpManifestProductInfo(baseDir);
        return xpManifestProductInfo != null ? new EAPXPServer8_1(migrationName, new ProductInfo("JBoss EAP XP", xpManifestProductInfo.getVersion()), baseDir, migrationEnvironment) :  new EAPServer8_1(migrationName, productInfo, baseDir, migrationEnvironment);
    }

    @Override
    public String getName() {
        return "JBoss EAP 8.1";
    }
}
