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
package org.jboss.migration.core;

import org.jboss.migration.core.env.MigrationEnvironment;
import org.jboss.migration.core.logger.ServerMigrationLogger;

import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Abstract {@link ServerProvider} impl that uses regex patterns to check if a product info, obtained from based dir, matches a provided server.
 * @author emmartins
 */
public abstract class AbstractServerProvider implements ServerProvider {

    @Override
    public Server getServer(String migrationName, Path baseDir, MigrationEnvironment migrationEnvironment) throws ServerMigrationFailureException {
        final ProductInfo productInfo = getProductInfo(baseDir, migrationEnvironment);
        return isProviderFor(productInfo) ? constructServer(migrationName, productInfo, baseDir, migrationEnvironment) : null;
    }

    /**
     * Retrieves the {@link ProductInfo} from the specified base dir.
     * @param baseDir the server's base dir
     * @param migrationEnvironment
     * @return the {@link ProductInfo} from the specified base dir
     * @throws ServerMigrationFailureException if the product's info failed to be retrieved.
     */
    protected abstract ProductInfo getProductInfo(Path baseDir, MigrationEnvironment migrationEnvironment) throws ServerMigrationFailureException;

    protected boolean isProviderFor(ProductInfo productInfo) {
        if (productInfo == null) {
            ServerMigrationLogger.ROOT_LOGGER.debugf("Failed to retrieve Product Info.");
            return false;
        }
        ServerMigrationLogger.ROOT_LOGGER.debugf("Product Info retrieved... %s", productInfo);
        final String productName = productInfo.getName();
        if (productName == null || !Pattern.matches(getProductNameRegex(), productName)) {
            ServerMigrationLogger.ROOT_LOGGER.debugf("Product name %s doesn't match!", productName);
            return false;
        }
        final String productVersion = productInfo.getVersion();
        if (productVersion == null || !Pattern.matches(getProductVersionRegex(), productVersion)) {
            ServerMigrationLogger.ROOT_LOGGER.debugf("Product version %s doesn't match!", productVersion);
            return false;
        }
        return true;
    }

    /**
     * Retrieves the pattern to match product's name.
     * @return the pattern to match product's name
     */
    protected abstract String getProductNameRegex();

    /**
     * Retrieves the pattern to match product's version.
     * @return the pattern to match product's version
     */
    protected abstract String getProductVersionRegex();

    /**
     * Constructs the server, from specified product info and base dir's path.
     * @param migrationName the migration server's name
     * @param productInfo the server's product info
     * @param baseDir the server's base dir
     * @param migrationEnvironment
     * @return the contructed server
     */
    protected abstract Server constructServer(String migrationName, ProductInfo productInfo, Path baseDir, MigrationEnvironment migrationEnvironment);
}
