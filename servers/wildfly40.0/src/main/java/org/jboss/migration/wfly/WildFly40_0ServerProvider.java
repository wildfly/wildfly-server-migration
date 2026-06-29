/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.wfly;

import org.jboss.migration.core.ProductInfo;
import org.jboss.migration.core.Server;
import org.jboss.migration.core.ServerMigrationFailureException;
import org.jboss.migration.core.ServerProvider;
import org.jboss.migration.core.env.MigrationEnvironment;
import org.jboss.migration.core.logger.ServerMigrationLogger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * The WildFly 40.x {@link org.jboss.migration.core.ServerProvider}.
 *  @author emmartins
 */
public class WildFly40_0ServerProvider implements ServerProvider {

    /**
     *
     * @param baseDir the server dist's base dir
     * @return a map with .galleon/proviwsioning.xml <feature-pack/> location data (name -> version)
     * @throws Exception
     */
    protected Map<String, String> getFeaturePackLocations(Path baseDir) throws Exception {
        final Path provisioningXml = baseDir.resolve(".galleon").resolve("provisioning.xml");
        if (!Files.isRegularFile(provisioningXml)) {
            return Map.of();
        }
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        final NodeList featurePacks = dbf.newDocumentBuilder().parse(provisioningXml.toFile()).getElementsByTagNameNS("*", "feature-pack");
        final Map<String, String> result = new HashMap<>();
        for (int i = 0; i < featurePacks.getLength(); i++) {
            final Element fp = (Element) featurePacks.item(i);
            final String location = fp.getAttribute("location");
            final int at = location.indexOf('@');
            final int hash = location.lastIndexOf('#');
            if (at > 0 && hash > at && hash < location.length() - 1) {
                final String name = location.substring(0, at);
                final String version = location.substring(hash + 1);
                result.put(name, version);
            }
        }
        return result;
    }

    protected Server constructServer(String migrationName, ProductInfo productInfo, Path baseDir, MigrationEnvironment migrationEnvironment) {
        return new WildFlyLegacyDist40_0Server(migrationName, productInfo, baseDir, migrationEnvironment);
    }

    @Override
    public Server getServer(String migrationName, Path baseDir, MigrationEnvironment migrationEnvironment) throws ServerMigrationFailureException {
        try {
            final Map<String, String> featurePackLocations = getFeaturePackLocations(baseDir);
            final String distVersion = featurePackLocations.get(getDistFeaturePackName());
            if (distVersion != null) {
                // full dist
                if (isValidVersion(distVersion)) {
                    if (isValidVersion(featurePackLocations.get(getStandardEeDistFeaturePackName()))) {
                        return constructStandardDistServer(migrationName, distVersion, baseDir, migrationEnvironment);
                    } else if (isValidVersion(featurePackLocations.get(getLegacyEeDistFeaturePackName()))) {
                        return constructLegacyDistServer(migrationName, distVersion, baseDir, migrationEnvironment);
                    }
                }
            } else {
                // ee dist
                final String eeDistVersion = featurePackLocations.get(getStandardEeDistFeaturePackName());
                if (eeDistVersion != null) {
                    // standard ee dist
                    if (isValidVersion(eeDistVersion)) {
                        return constructStandardEeDistServer(migrationName,eeDistVersion, baseDir, migrationEnvironment);
                    }
                } else {
                    final String legacyEeDistVersion = featurePackLocations.get(getLegacyEeDistFeaturePackName());
                    if (legacyEeDistVersion != null) {
                        // legacy ee dist
                        if (isValidVersion(legacyEeDistVersion)) {
                            return constructLegacyEeDistServer(migrationName,legacyEeDistVersion, baseDir, migrationEnvironment);
                        }
                    }
                }
            }
        } catch (Exception e) {
            ServerMigrationLogger.ROOT_LOGGER.error("failed to obtain provisioned feature pack locations ", e);
        }
        return null;
    }

    protected String getLegacyEeDistFeaturePackName() {
        return "wildfly-ee-10";
    }

    protected String getDistFeaturePackName() {
        return "wildfly";
    }

    protected String getStandardEeDistFeaturePackName() {
        return "wildfly-ee";
    }

    protected boolean isValidVersion(String version) {
        return version != null && version.startsWith("40.");
    }

    protected Server constructStandardDistServer(String migrationName, String version, Path baseDir, MigrationEnvironment migrationEnvironment) {
        return new WildFlyStandardDist40_0Server(migrationName, new ProductInfo("WildFly Dist", version), baseDir, migrationEnvironment);
    }

    protected Server constructStandardEeDistServer(String migrationName, String version, Path baseDir, MigrationEnvironment migrationEnvironment) {
        return new WildFlyStandardEeDist40_0Server(migrationName, new ProductInfo("WildFly EE Dist", version), baseDir, migrationEnvironment);

    }

    protected Server constructLegacyDistServer(String migrationName, String version, Path baseDir, MigrationEnvironment migrationEnvironment) {
        return new WildFlyLegacyEeDist40_0Server(migrationName, new ProductInfo("WildFly Legacy Dist", version), baseDir, migrationEnvironment);
    }

    protected Server constructLegacyEeDistServer(String migrationName, String version, Path baseDir, MigrationEnvironment migrationEnvironment) {
        return new WildFlyLegacyEeDist40_0Server(migrationName, new ProductInfo("WildFly Legacy EE Dist", version), baseDir, migrationEnvironment);
    }

    public String getName() {
        return "WildFly 40.x";
    }
}
