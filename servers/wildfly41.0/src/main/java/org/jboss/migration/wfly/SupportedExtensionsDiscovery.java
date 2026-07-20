/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.wfly;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODULE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.migration.core.ServerMigrationFailureException;
import org.jboss.migration.core.env.MigrationEnvironment;
import org.jboss.migration.core.jboss.Extension;
import org.jboss.migration.core.jboss.ExtensionsDiscovery;
import org.jboss.migration.core.jboss.JBossServer;
import org.jboss.migration.core.jboss.JBossServerConfiguration;
import org.jboss.migration.core.jboss.Subsystem;
import org.jboss.migration.core.logger.ServerMigrationLogger;
import org.jboss.migration.wfly10.config.management.impl.EmbeddedStandaloneServerConfiguration;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.wildfly.core.embedded.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Discovers which extensions are actually supported by a server by checking which ones
 * successfully load when the server starts with its standard configuration.
 * This validation is necessary because not all extensions found in the modules directory
 * may be compatible with the current server version.
 *
 * @author emmartins
 */
public class SupportedExtensionsDiscovery {

    public static final String PROPERTY_CONFIG_FILE = "discovery.extensions.configFile";
    public static final String DEFAULT_CONFIG_FILE = "standalone.xml";

    /**
     * Discovers extensions module names and validates which are supported by checking which ones exist as resources
     * in a server.
     *
     * @param server the server
     * @return a set of Extensions that are supported
     */
    public static Set<Extension> discoverSupportedExtensions(WildFly41_0Server server, MigrationEnvironment migrationEnvironment) throws ServerMigrationFailureException {
        return discoverSupportedExtensions(server, ExtensionsDiscovery.discoverExtensionModuleNames(server.getModules()), migrationEnvironment);
    }

    /**
     * Validates which extensions are supported by checking which ones exist as resources
     * in a server.
     *
     * @param server the server
     * @param candidateExtensions the set of module names of candidate Extensions to validate
     * @return a set of Extensions that are supported
     */
    public static Set<Extension> discoverSupportedExtensions(WildFly41_0Server server, Set<String> candidateExtensions, MigrationEnvironment migrationEnvironment) {
        final String configFileName = migrationEnvironment.getPropertyAsString(JBossServer.Environment.getFullEnvironmentPropertyName(server.getMigrationName(), PROPERTY_CONFIG_FILE), DEFAULT_CONFIG_FILE);
        final Path configFilePath = server.getStandaloneConfigurationDir().resolve(configFileName);
        if (!Files.exists(configFilePath)) {
            throw new ServerMigrationFailureException("Failed to discover target server supported extensions, "+configFilePath.toAbsolutePath()+" config file not found.");
        }
        if (candidateExtensions.isEmpty()) {
            return Set.of();
        }
        final Set<Extension> supportedExtensions = new HashSet<>();
        final String cloneConfigFileName = SupportedExtensionsDiscovery.class.getName()+java.util.UUID.randomUUID()+'-'+configFileName;
        final Path clonedConfigFilePath = server.getStandaloneConfigurationDir().resolve(cloneConfigFileName);
        try {
            // Clone the configuration file to avoid modifying the original
            Files.copy(configFilePath, clonedConfigFilePath, StandardCopyOption.REPLACE_EXISTING);
            // Start the embedded server
            final EmbeddedStandaloneServerConfiguration embeddedStandaloneServerConfiguration = new EmbeddedStandaloneServerConfiguration(new JBossServerConfiguration<>(clonedConfigFilePath, JBossServerConfiguration.Type.STANDALONE, server), server);
            embeddedStandaloneServerConfiguration.start();
            final Configuration.Builder configurationBuilder = Configuration.Builder.of(server.getBaseDir());
            configurationBuilder.addSystemPackage("org.jboss.logmanager");
            final Configuration configuration = configurationBuilder.build();
            final ModuleLoader moduleLoader = configuration.getModuleLoader();
            try {
                final ModelControllerClient client = embeddedStandaloneServerConfiguration.getModelControllerClient();
                // Check which candidate extensions are supported and populate their subsystems
                for (String candidateExtension : candidateExtensions) {
                    // unsupported Extensions extend AbstractLegacyExtension
                    Module module = moduleLoader.loadModule(candidateExtension);
                    boolean unsupported = false;
                    Class<?> extensionClass = module.getClassLoader().loadClass("org.jboss.as.controller.Extension");
                    Class<?> abstractLegacyExtensionClass = module.getClassLoader().loadClass("org.jboss.as.controller.extension.AbstractLegacyExtension");
                    Iterator<?> iterator = module.loadService(extensionClass).iterator();
                    while (iterator.hasNext()) {
                        Object extension = iterator.next();
                        if (abstractLegacyExtensionClass.isAssignableFrom(extension.getClass())) {
                            ServerMigrationLogger.ROOT_LOGGER.debugf("Extension class %s is legacy.", extension.getClass().getName());
                            unsupported = true;
                            break;
                        }
                    }
                    if (!unsupported) {
                        ServerMigrationLogger.ROOT_LOGGER.debugf("Extension %s is supported.", candidateExtension);
                        final Extension extensionWithSubsystems = discoverExtensionWithSubsystems(client, candidateExtension);
                        if (extensionWithSubsystems != null) {
                            supportedExtensions.add(extensionWithSubsystems);
                        }
                    } else {
                        ServerMigrationLogger.ROOT_LOGGER.debugf("Extension %s is not supported.", candidateExtension);
                    }
                }
            } finally {
                // Stop the embedded server
                embeddedStandaloneServerConfiguration.stop();
            }
        } catch (Exception e) {
            throw new ServerMigrationFailureException("Failed to validate extensions", e);
        } finally {
            // Clean up test config file
            try {
                Files.deleteIfExists(clonedConfigFilePath);
            } catch (IOException e) {
                ServerMigrationLogger.ROOT_LOGGER.debugf("Failed to delete test config file: %s", e.getMessage());
            }
        }
        return supportedExtensions;
    }

    /**
     * Discovers an extension with its subsystems populated.
     * Returns null if the extension is not supported.
     */
    public static Extension discoverExtensionWithSubsystems(ModelControllerClient client, String extensionModule) {
        try {
            final PathAddress extensionAddress = PathAddress.pathAddress(EXTENSION, extensionModule);
            // First, check if extension resource already exists
            ModelNode readOp = Util.createEmptyOperation("read-resource", extensionAddress);
            readOp.get("include-runtime").set(true);
            ModelNode readResult = client.execute(readOp);
            boolean extensionExists = isSuccessful(readResult);
            if (!extensionExists) {
                // Not loaded, try to add it
                final ModelNode addOp = Util.createAddOperation(extensionAddress);
                addOp.get(MODULE).set(extensionModule);
                final ModelNode addResult = client.execute(addOp);
                if (!isSuccessful(addResult)) {
                    throw new ServerMigrationFailureException("Failed to add extension " + extensionModule + " to the standalone server.");
                }
                // Re-read the extension resource to get subsystems
                readResult = client.execute(readOp);
                if (!isSuccessful(readResult)) {
                    throw new ServerMigrationFailureException("Extension " + extensionModule + " could not be read after adding.");
                }
            }
            // discover extension's subsystems
            return buildExtensionWithSubsystems(client, extensionModule, readResult);
        } catch (IOException e) {
            throw new ServerMigrationFailureException("Failed to validate extension " + extensionModule, e);
        }
    }

    /**
     * Builds an Extension with subsystems populated from the extension resource.
     */
    private static Extension buildExtensionWithSubsystems(ModelControllerClient client, String extensionModule, ModelNode extensionResource) throws IOException {
        final Extension.Builder extensionBuilder = Extension.builder().module(extensionModule);

        // Read child resources to get subsystems
        final PathAddress extensionAddress = PathAddress.pathAddress(EXTENSION, extensionModule);
        final ModelNode readChildrenOp = Util.createEmptyOperation("read-children-names", extensionAddress);
        readChildrenOp.get("child-type").set("subsystem");
        final ModelNode subsystemsResult = client.execute(readChildrenOp);

        if (isSuccessful(subsystemsResult) && subsystemsResult.hasDefined(RESULT)) {
            final List<ModelNode> subsystemNames = subsystemsResult.get(RESULT).asList();

            for (ModelNode subsystemNameNode : subsystemNames) {
                final String subsystemName = subsystemNameNode.asString();
                final String namespaceWithoutVersion = discoverNamespaceWithoutVersion(client, extensionModule, subsystemName);

                extensionBuilder.subsystem(
                    Subsystem.builder()
                        .name(subsystemName)
                        .namespaceWithoutVersion(namespaceWithoutVersion)
                );

                ServerMigrationLogger.ROOT_LOGGER.debugf("  Subsystem %s (namespace: %s)", subsystemName, namespaceWithoutVersion);
            }
        }

        return extensionBuilder.build();
    }

    /**
     * Discovers the namespace without version for a subsystem by reading xml-namespaces
     * and finding the common prefix.
     */
    private static String discoverNamespaceWithoutVersion(ModelControllerClient client, String extensionModule, String subsystemName) {
        try {
            final PathAddress subsystemAddress = PathAddress.pathAddress(EXTENSION, extensionModule)
                    .append("subsystem", subsystemName);

            final ModelNode readOp = Util.createEmptyOperation("read-resource", subsystemAddress);
            readOp.get("include-runtime").set(true);
            final ModelNode result = client.execute(readOp);

            if (isSuccessful(result) && result.hasDefined(RESULT)) {
                final ModelNode subsystemResource = result.get(RESULT);


                if (subsystemResource.hasDefined("xml-namespaces")) {
                    final List<ModelNode> namespaces = subsystemResource.get("xml-namespaces").asList();

                    if (!namespaces.isEmpty()) {
                        // Find common prefix of all namespace versions
                        return findCommonNamespacePrefix(namespaces);
                    }
                }
            }
        } catch (IOException e) {
            throw new ServerMigrationFailureException("Failed to read xml-namespaces for subsystem " + subsystemName, e);
        }
        // Fallback to default pattern
        return "urn:jboss:domain:" + subsystemName;
    }

    /**
     * Finds the common prefix from a list of namespace URIs by finding the longest
     * common substring up to a colon separator.
     *
     * For example:
     * - ["urn:jboss:domain:undertow:1.0", "urn:jboss:domain:undertow:2.0"]
     *   returns "urn:jboss:domain:undertow"
     * - ["urn:wildfly:elytron:1.0", "urn:wildfly:elytron:community:2.0"]
     *   returns "urn:wildfly:elytron" (handles stability levels)
     */
    private static String findCommonNamespacePrefix(List<ModelNode> namespaces) {
        if (namespaces.isEmpty()) {
            return null;
        }

        if (namespaces.size() == 1) {
            // Single namespace - strip version components from the end
            String namespace = namespaces.get(0).asString();
            return stripVersionComponents(namespace);
        }

        // Multiple namespaces - find common prefix
        String firstNamespace = namespaces.get(0).asString();

        // Try progressively shorter prefixes (removing from the last colon backwards)
        int colonIndex = firstNamespace.lastIndexOf(':');

        while (colonIndex > 0) {
            final String candidate = firstNamespace.substring(0, colonIndex);

            // Check if all namespaces start with this candidate
            boolean allMatch = true;
            for (ModelNode namespaceNode : namespaces) {
                if (!namespaceNode.asString().startsWith(candidate + ":")) {
                    allMatch = false;
                    break;
                }
            }

            if (allMatch) {
                return candidate;
            }

            // Try the next colon position going backwards
            colonIndex = firstNamespace.lastIndexOf(':', colonIndex - 1);
        }

        // Fallback - just use the first namespace stripped
        return stripVersionComponents(firstNamespace);
    }

    /**
     * Strips version components from the end of a namespace URI.
     * Removes the last component if it looks like a version (starts with a digit).
     */
    private static String stripVersionComponents(String namespace) {
        int lastColon = namespace.lastIndexOf(':');
        if (lastColon > 0) {
            String lastComponent = namespace.substring(lastColon + 1);
            // Check if it looks like a version (starts with digit)
            if (lastComponent.length() > 0 && Character.isDigit(lastComponent.charAt(0))) {
                return namespace.substring(0, lastColon);
            }
        }
        return namespace;
    }

    /**
     * Checks if a management operation result indicates success.
     */
    private static boolean isSuccessful(ModelNode result) {
        return result != null && SUCCESS.equals(result.get(OUTCOME).asString());
    }
}
