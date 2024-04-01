/*
 * Copyright 2016 Red Hat, Inc.
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
package org.jboss.migration.core.jboss;

import org.jboss.migration.core.ProductInfo;
import org.jboss.migration.core.ServerMigrationFailureException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Manifest;

/**
 * The {@link ProductInfo} obtained from a Manifest file.
 * @author emmartins
 */
public class ManifestProductInfo extends ProductInfo {
    private ManifestProductInfo(String name, String version) {
        super(name, version);
    }

    /**
     * Retrieves the product info from the specified manifest, with default attr names "JBoss-Product-Release-Name","JBoss-Product-Release-Version".
     * @param manifest the manifest file
     * @return the product info from the specified manifest inputstream
     * @throws ServerMigrationFailureException if there is an error reading the manifest input stream
     */
    public static ManifestProductInfo from(Manifest manifest) throws ServerMigrationFailureException {
        return from(manifest, "JBoss-Product-Release-Name","JBoss-Product-Release-Version");
    }

    /**
     * Retrieves the product info from the specified manifest, from attributes with the specified prefix.
     * @param manifest the manifest file
     * @param productNameAttr the product name's attribute name
     * @param productVersionAttr the product version's attribute name
     * @return the product info from the specified manifest inputstream
     * @throws ServerMigrationFailureException if there is an error reading the manifest input stream
     */
    public static ManifestProductInfo from(Manifest manifest, String productNameAttr, String productVersionAttr) throws ServerMigrationFailureException {
        final String productName = manifest.getMainAttributes().getValue(productNameAttr);
        if (productName == null) {
            throw new IllegalArgumentException();
        }
        final String productVersion = manifest.getMainAttributes().getValue(productVersionAttr);
        if (productVersion == null) {
            throw new IllegalArgumentException();
        }
        return new ManifestProductInfo(productName.trim(), productVersion.trim());
    }

    /**
     * Retrieves the product info from the specified's manifest inputstream.
     * @param inputStream the inputstream to read the manifest file
     * @return the product info from the specified's manifest inputstream
     * @throws ServerMigrationFailureException if there is an error reading the manifest input stream
     */
    public static ManifestProductInfo from(InputStream inputStream) throws ServerMigrationFailureException {
        try {
            return from(new Manifest(inputStream));
        } catch (IOException e) {
            throw new ServerMigrationFailureException("MANIFEST stream load failure.", e);
        }
    }

    /**
     * Retrieves the product info from the specified's manifest file path.
     * @param path the path pointing to the manifest file
     * @return the product info from the specified's manifest file path
     * @throws ServerMigrationFailureException if there is an error reading the manifest file
     */
    public static ManifestProductInfo from(Path path) throws ServerMigrationFailureException {
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try {
            return from(Files.newInputStream(path));
        } catch (IOException e) {
            throw new ServerMigrationFailureException("Manifest file load failed.", e);
        }
    }
}
