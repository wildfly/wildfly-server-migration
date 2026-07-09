/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.core.jboss;

import org.jboss.migration.core.logger.ServerMigrationLogger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 * Discovers extension modules in a JBoss/WildFly server installation.
 *
 * Extensions are identified by the presence of META-INF/services/org.jboss.as.controller.Extension
 * files in module resources. This class scans the modules directory structure (including layers,
 * overlays, and add-ons) to find all modules that provide extensions.
 *
 * @author emmartins
 */
public class ExtensionsDiscovery {

    private static final String EXTENSION_SERVICE_FILE = "META-INF/services/org.jboss.as.controller.Extension";

    /**
     * Discovers all extension module names in the given server's modules.
     * Follows the same priority order as module loading: overlay -> layers -> add-ons.
     * Once a module is found in a higher-priority directory, it's not processed from lower-priority directories.
     *
     * @param modules the server's modules instance
     * @return a set of module names that provide extensions
     */
    public static Set<String> discoverExtensionModuleNames(JBossServer.Modules modules) {
        final Set<String> extensionModules = new HashSet<>();
        final Set<String> processedModules = new HashSet<>();

        // Scan all module directories (overlays, layers, add-ons) in priority order
        final List<Path> moduleDirs = getAllModuleDirs(modules);

        for (Path moduleDir : moduleDirs) {
            if (!Files.exists(moduleDir)) {
                continue;
            }

            try {
                scanForExtensions(moduleDir, extensionModules, processedModules);
            } catch (IOException e) {
                ServerMigrationLogger.ROOT_LOGGER.warnf("Failed to scan module directory %s: %s", moduleDir, e.getMessage());
            }
        }

        return extensionModules;
    }

    /**
     * Gets all module directories to scan, in priority order (overlay, layers, add-ons).
     */
    private static List<Path> getAllModuleDirs(JBossServer.Modules modules) {
        final List<Path> moduleDirs = new ArrayList<>();

        // Add overlay dir (highest priority)
        if (modules.getOverlayDir() != null) {
            moduleDirs.add(modules.getOverlayDir());
        }

        // Add layer dirs
        moduleDirs.addAll(modules.getLayerDirs());

        // Add add-on dirs
        if (modules.getAddonDirs() != null) {
            moduleDirs.addAll(modules.getAddonDirs());
        }

        return moduleDirs;
    }

    /**
     * Scans a module directory tree for extensions.
     * Skips modules that have already been processed in higher-priority directories.
     * Also tracks module alias targets to avoid processing them again as standalone modules.
     */
    private static void scanForExtensions(Path moduleBaseDir, Set<String> extensionModules, Set<String> processedModules) throws IOException {
        // Collect all module.xml files first
        final List<Path> moduleXmlPaths = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(moduleBaseDir)) {
            paths.filter(path -> path.getFileName().toString().equals("module.xml"))
                 .forEach(moduleXmlPaths::add);
        }

        // First pass: collect all alias targets
        final Set<String> aliasTargets = new HashSet<>();
        for (Path moduleXmlPath : moduleXmlPaths) {
            try {
                final ModuleInfo moduleInfo = extractModuleInfo(moduleXmlPath);
                if (moduleInfo != null && moduleInfo.targetName != null) {
                    aliasTargets.add(moduleInfo.targetName);
                }
            } catch (Exception e) {
                // Ignore errors in first pass
            }
        }

        // Second pass: process modules
        for (Path moduleXmlPath : moduleXmlPaths) {
            try {
                final ModuleInfo moduleInfo = extractModuleInfo(moduleXmlPath);
                if (moduleInfo != null && moduleInfo.name != null) {
                    // Skip if already processed from a higher-priority directory
                    if (processedModules.contains(moduleInfo.name)) {
                        continue;
                    }

                    // Mark as processed
                    processedModules.add(moduleInfo.name);

                    // Skip if this module is a target of an alias (the alias is the public API)
                    if (aliasTargets.contains(moduleInfo.name)) {
                        continue;
                    }

                    // If this is a module alias, resolve the target module path
                    final Path modulePathToCheck = moduleInfo.targetName != null ?
                            resolveTargetModulePath(moduleBaseDir, moduleInfo.targetName) : moduleXmlPath;

                    // Check if it has extension service
                    if (modulePathToCheck != null && hasExtensionService(modulePathToCheck)) {
                        extensionModules.add(moduleInfo.name);
                    }
                }
            } catch (Exception e) {
                ServerMigrationLogger.ROOT_LOGGER.warnf("Failed to process module.xml %s: %s", moduleXmlPath, e.getMessage());
            }
        }
    }

    /**
     * Checks if a module has the Extension service file.
     */
    private static boolean hasExtensionService(Path moduleXmlPath) throws IOException, XMLStreamException {
        final Path moduleDir = moduleXmlPath.getParent();

        // Check for loose service file
        final Path serviceFile = moduleDir.resolve(EXTENSION_SERVICE_FILE);
        if (Files.exists(serviceFile)) {
            return true;
        }

        // Check in resource JARs
        final List<String> resourceJars = extractResourceJars(moduleXmlPath);
        for (String jarName : resourceJars) {
            final Path jarPath = moduleDir.resolve(jarName);
            if (Files.exists(jarPath) && hasServiceFileInJar(jarPath)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Extracts resource JAR names from module.xml.
     */
    private static List<String> extractResourceJars(Path moduleXmlPath) throws IOException, XMLStreamException {
        final List<String> jarNames = new ArrayList<>();

        try (InputStream in = new BufferedInputStream(new FileInputStream(moduleXmlPath.toFile()))) {
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(in);

            boolean inResources = false;
            while (reader.hasNext()) {
                int type = reader.next();
                switch (type) {
                    case START_ELEMENT:
                        if ("resources".equals(reader.getLocalName())) {
                            inResources = true;
                        } else if (inResources && "resource-root".equals(reader.getLocalName())) {
                            final String path = reader.getAttributeValue(null, "path");
                            if (path != null && path.endsWith(".jar")) {
                                jarNames.add(path);
                            }
                        }
                        break;
                    case END_ELEMENT:
                        if ("resources".equals(reader.getLocalName())) {
                            inResources = false;
                        }
                        break;
                }
            }
        }

        return jarNames;
    }

    /**
     * Checks if a JAR file contains the Extension service file.
     */
    private static boolean hasServiceFileInJar(Path jarPath) {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            final JarEntry entry = jarFile.getJarEntry(EXTENSION_SERVICE_FILE);
            return entry != null;
        } catch (IOException e) {
            ServerMigrationLogger.ROOT_LOGGER.warnf("Failed to read JAR %s: %s", jarPath, e.getMessage());
            return false;
        }
    }

    /**
     * Holds information extracted from a module.xml file.
     */
    private static class ModuleInfo {
        final String name;
        final String targetName;  // for module-alias

        ModuleInfo(String name, String targetName) {
            this.name = name;
            this.targetName = targetName;
        }
    }

    /**
     * Extracts module information from module.xml.
     * For regular modules, returns the module name.
     * For module aliases, returns both the alias name and target-name.
     */
    private static ModuleInfo extractModuleInfo(Path moduleXmlPath) throws IOException, XMLStreamException {
        try (InputStream in = new BufferedInputStream(new FileInputStream(moduleXmlPath.toFile()))) {
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(in);

            while (reader.hasNext()) {
                int type = reader.next();
                if (type == START_ELEMENT) {
                    if ("module".equals(reader.getLocalName())) {
                        final String name = reader.getAttributeValue(null, "name");
                        return new ModuleInfo(name, null);
                    } else if ("module-alias".equals(reader.getLocalName())) {
                        final String name = reader.getAttributeValue(null, "name");
                        final String targetName = reader.getAttributeValue(null, "target-name");
                        return new ModuleInfo(name, targetName);
                    }
                }
            }
        }

        return null;
    }

    /**
     * Resolves a module name to its module.xml path by converting the module name
     * to a directory path structure (e.g., "org.jboss.foo" -> "org/jboss/foo/main/module.xml").
     */
    private static Path resolveTargetModulePath(Path moduleBaseDir, String targetModuleName) {
        // Module names use dots, directory structure uses slashes
        final String modulePath = targetModuleName.replace('.', '/');

        // Try with default slot (main)
        Path targetPath = moduleBaseDir.resolve(modulePath).resolve("main").resolve("module.xml");
        if (Files.exists(targetPath)) {
            return targetPath;
        }

        // Try without slot (some modules may not use the slot directory)
        targetPath = moduleBaseDir.resolve(modulePath).resolve("module.xml");
        if (Files.exists(targetPath)) {
            return targetPath;
        }

        ServerMigrationLogger.ROOT_LOGGER.warnf("Could not resolve target module: %s", targetModuleName);
        return null;
    }


}
