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

import org.jboss.dmr.ValueExpression;
import org.jboss.migration.core.AbstractServer;
import org.jboss.migration.core.ProductInfo;
import org.jboss.migration.core.ServerMigrationFailureException;
import org.jboss.migration.core.env.MigrationEnvironment;
import org.jboss.migration.core.logger.ServerMigrationLogger;
import org.jboss.migration.core.util.xml.SimpleXMLFileMatcher;
import org.jboss.migration.core.util.xml.XMLFileMatcher;
import org.jboss.migration.core.util.xml.XMLFiles;

import javax.xml.stream.XMLStreamException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An abstract JBoss {@link org.jboss.migration.core.Server} impl, which is usable only as migration source.
 * @author emmartins
 */
public abstract class JBossServer<S extends JBossServer<S>> extends AbstractServer implements AbsolutePathResolver {

    private final Environment environment;
    private final Path domainBaseDir;
    private final Path domainConfigDir;
    private final Path domainDataDir;
    private final Path domainContentDir;

    private final Path standaloneServerDir;
    private final Path standaloneConfigDir;
    private final Path standaloneDataDir;
    private final Path standaloneContentDir;

    private final ValueExpressionResolver expressionResolver;

    private final Map<String, Path> pathResolver;

    private final Modules modules;

    private final Extensions extensions;

    public JBossServer(String migrationName, ProductInfo productInfo, Path baseDir, MigrationEnvironment migrationEnvironment, Extensions extensions) {
        super(migrationName, productInfo, baseDir, migrationEnvironment);
        this.environment = new Environment(this);
        this.domainBaseDir = getServerDir(environment.getDomainBaseDir(), baseDir);
        this.domainConfigDir = getServerDir(environment.getDomainConfigDir(), domainBaseDir);
        this.domainDataDir = getServerDir(environment.getDomainDataDir(), domainBaseDir);
        this.domainContentDir = getServerDir(environment.getDomainContentDir(), domainDataDir);
        this.standaloneServerDir = getServerDir(environment.getStandaloneServerDir(), baseDir);
        this.standaloneConfigDir = getServerDir(environment.getStandaloneConfigDir(), standaloneServerDir);
        this.standaloneDataDir = getServerDir(environment.getStandaloneDataDir(), standaloneServerDir);
        this.standaloneContentDir = getServerDir(environment.getStandaloneContentDir(), standaloneDataDir);

        this.expressionResolver = new ValueExpressionResolver();

        this.pathResolver = new HashMap<>();
        this.pathResolver.put("jboss.home.dir", baseDir);
        this.pathResolver.put("jboss.server.base.dir", standaloneServerDir);
        this.pathResolver.put("jboss.server.config.dir", standaloneConfigDir);
        this.pathResolver.put("jboss.server.content.dir", standaloneContentDir);
        this.pathResolver.put("jboss.server.data.dir", standaloneDataDir);
        this.pathResolver.put("jboss.server.deploy.dir", standaloneContentDir);
        this.pathResolver.put("jboss.server.log.dir", standaloneServerDir.resolve("log"));
        this.pathResolver.put("jboss.server.temp.dir", standaloneServerDir.resolve("tmp"));
        this.pathResolver.put("jboss.domain.base.dir", domainBaseDir);
        this.pathResolver.put("jboss.domain.config.dir", domainConfigDir);
        this.pathResolver.put("jboss.domain.content.dir", domainContentDir);
        this.pathResolver.put("jboss.domain.data.dir", domainDataDir);
        this.pathResolver.put("jboss.domain.deployment.dir", domainContentDir);
        this.pathResolver.put("jboss.domain.log.dir", domainBaseDir.resolve("log"));
        this.pathResolver.put("jboss.domain.servers.dir", domainBaseDir.resolve("servers"));
        this.pathResolver.put("jboss.domain.temp.dir", domainBaseDir.resolve("tmp"));

        this.modules = new Modules(baseDir);

        this.extensions = extensions;
    }

    public Environment getEnvironment() {
        return environment;
    }

    protected List<JBossServerConfiguration<S>> getConfigs(final JBossServerConfiguration.Type configurationType) {
        final List<JBossServerConfiguration<S>> configs = new ArrayList<>();
        final String xmlDocumentElementName;
        final List<String> envConfigs;
        switch (configurationType) {
            case STANDALONE:
                envConfigs = environment.getStandaloneConfigFiles();
                xmlDocumentElementName = "server";
                break;
            case DOMAIN:
                envConfigs = environment.getDomainConfigFiles();
                xmlDocumentElementName = "domain";
                break;
            case HOST:
                envConfigs = environment.getHostConfigFiles();
                xmlDocumentElementName = "host";
                break;
            default:
                throw new IllegalArgumentException("unknown config type "+configurationType);
        }
        if (envConfigs != null && !envConfigs.isEmpty()) {
            for (String envConfig : envConfigs) {
                Path config = Paths.get(envConfig);
                if (!config.isAbsolute()) {
                    config = getConfigurationDir(configurationType).resolve(config);
                }
                if (Files.exists(config)) {
                    configs.add(new JBossServerConfiguration<>(config, configurationType, (S) this));
                } else {
                    ServerMigrationLogger.ROOT_LOGGER.warnf("Config file %s, specified by the environment properties, does not exists.", config);
                }
            }
        } else {
            // scan config dir
            final XMLFileMatcher scanMatcher = new SimpleXMLFileMatcher() {
                @Override
                protected boolean documentElementLocalNameMatches(String localName) {
                    return xmlDocumentElementName.equals(localName);
                }
                @Override
                protected boolean documentNamespaceURIMatches(String namespaceURI) {
                    return namespaceURI.startsWith("urn:jboss:domain:");
                }
            };
            for (Path path : XMLFiles.scan(getConfigurationDir(configurationType), false, scanMatcher)) {
                configs.add(new JBossServerConfiguration<>(path, configurationType, (S) this));
            }
        }
        return Collections.unmodifiableList(configs);
    }

    public List<JBossServerConfiguration<S>> getStandaloneConfigs() {
        return getConfigs(JBossServerConfiguration.Type.STANDALONE);
    }

    public List<JBossServerConfiguration<S>> getDomainDomainConfigs() {
        return getConfigs(JBossServerConfiguration.Type.DOMAIN);
    }

    public List<JBossServerConfiguration<S>> getDomainHostConfigs() {
        return getConfigs(JBossServerConfiguration.Type.HOST);
    }

    private Path getServerDir(String name, Path relativeTo) {
        Path path = Paths.get(name);
        if (!path.isAbsolute()) {
            path = relativeTo.resolve(path);
        }
        return path;
    }

    public Path getDomainDir() {
        return domainBaseDir;
    }

    public Path getDefaultDomainDir() {
        return getServerDir(environment.getDefaultDomainBaseDir(), getBaseDir());
    }

    public Path getDomainConfigurationDir() {
        return domainConfigDir;
    }

    public Path getDefaultDomainConfigurationDir() {
        return getServerDir(environment.getDefaultDomainConfigDir(), getDefaultDomainDir());
    }

    public Path getDomainDataDir() {
        return domainDataDir;
    }

    public Path getDefaultDomainDataDir() {
        return getServerDir(environment.getDefaultDomainDataDir(), getDefaultDomainDir());
    }

    public Path getDomainContentDir() {
        return domainContentDir;
    }

    public Path getDefaultDomainContentDir() {
        return getServerDir(environment.getDefaultDomainContentDir(), getDefaultDomainDataDir());
    }

    public Path getStandaloneDir() {
        return standaloneServerDir;
    }

    public Path getDefaultStandaloneDir() {
        return getServerDir(environment.getDefaultStandaloneServerDir(), getBaseDir());
    }

    public Path getStandaloneConfigurationDir() {
        return standaloneConfigDir;
    }

    public Path getDefaultStandaloneConfigurationDir() {
        return getServerDir(environment.getDefaultStandaloneConfigDir(), getDefaultStandaloneDir());
    }

    public Path getStandaloneDataDir() {
        return standaloneDataDir;
    }

    public Path getDefaultStandaloneDataDir() {
        return getServerDir(environment.getDefaultStandaloneDataDir(), getDefaultStandaloneDir());
    }

    public Path getStandaloneContentDir() {
        return standaloneContentDir;
    }

    public Path getDefaultStandaloneContentDir() {
        return getServerDir(environment.getDefaultStandaloneContentDir(), getDefaultStandaloneDataDir());
    }

    public Path getConfigurationDir(JBossServerConfiguration.Type configurationType) {
        return configurationType == JBossServerConfiguration.Type.STANDALONE ? getStandaloneConfigurationDir() : getDomainConfigurationDir();
    }

    public Path getDataDir(JBossServerConfiguration.Type configurationType) {
        return configurationType == JBossServerConfiguration.Type.STANDALONE ? getStandaloneDataDir() : getDomainDataDir();
    }

    public Path getContentDir(JBossServerConfiguration.Type configurationType) {
        return configurationType == JBossServerConfiguration.Type.STANDALONE ? getStandaloneContentDir() : getDomainContentDir();
    }

    public String resolveExpression(String expression) {
        return expressionResolver.resolve(expression);
    }

    @Override
    public Path resolveNamedPath(String name) {
        Path path = pathResolver.get(name);
        if (path == null) {
            // try expression resolver
            final String s = expressionResolver.resolvePart(name);
            if (s != null) {
                path = Paths.get(s);
            }
        }
        return path;
    }

    @Override
    public Path resolvePath(String path, String relativeTo) {
        path = resolveExpression(path);
        if (path == null && relativeTo == null) {
            return null;
        }
        final Path resolvedPath;
        if (relativeTo == null) {
            resolvedPath = Paths.get(path).toAbsolutePath();
        } else {
            final Path resolvedRelativeTo = resolveNamedPath(relativeTo);
            if (resolvedRelativeTo == null) {
                return null;
            }
            resolvedPath = path != null ? resolvedRelativeTo.resolve(path).toAbsolutePath() : resolvedRelativeTo.toAbsolutePath();
        }
        return resolvedPath;
    }

    public Modules getModules() {
        return modules;
    }

    public Extensions getExtensions() {
        return extensions;
    }

    public class ValueExpressionResolver extends org.jboss.dmr.ValueExpressionResolver {
        @Override
        protected String resolvePart(String name) {
            final Path path = pathResolver.get(name);
            if (path != null) {
                return path.toAbsolutePath().toString();
            } else {
                return super.resolvePart(name);
            }
        }

        public String resolve(String value) {
            return isExpression(value) ? resolve(new ValueExpression(value)) : value;
        }

        protected boolean isExpression(String value) {
            int openIdx = value.indexOf("${");
            return openIdx > -1 && value.lastIndexOf('}') > openIdx;
        }
    }

    public static class Module {
        private final Path moduleDir;
        private final ModuleSpecification moduleSpecification;

        public Module(Path moduleDir, ModuleSpecification moduleSpecification) {
            this.moduleDir = moduleDir;
            this.moduleSpecification = moduleSpecification;
        }

        public ModuleSpecification getModuleSpecification() {
            return moduleSpecification;
        }

        public Path getModuleDir() {
            return moduleDir;
        }
    }

    public static class Modules {

        private final Path modulesDir;
        private final List<Path> layerDirs;
        private final List<Path> addonDirs;
        private final Path overlayDir;

        public Modules(Path serverBaseDir) {
            this.modulesDir = serverBaseDir.resolve("modules");
            this.layerDirs = new ArrayList<>();
            // process layers.conf (if exists)
            final Path layersConfigFile = modulesDir.resolve("layers.conf");
            if (Files.exists(layersConfigFile)) {
                try (BufferedReader reader = Files.newBufferedReader(layersConfigFile)) {
                    Properties properties = new Properties();
                    properties.load(reader);
                    final String layers = properties.getProperty("layers");
                    if (layers != null) {
                        for (String layer : layers.split(",")) {
                            layer.trim();
                            if (!layer.isEmpty()) {
                                layerDirs.add(modulesDir.resolve("system").resolve("layers").resolve(layer));
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new ServerMigrationFailureException("failed to read layers file", e);
                }
            }
            // add base layer
            final Path baseLayerDir = modulesDir.resolve("system").resolve("layers").resolve("base");
            layerDirs.add(baseLayerDir);
            // check for base overlays
            final Path overlaysDir = baseLayerDir.resolve(".overlays");
            final Path overlaysFile = overlaysDir.resolve(".overlays");
            if (Files.exists(overlaysFile)) {
                try {
                    final String activeOverlayFileName = new String(Files.readAllBytes(overlaysFile)).trim();
                    if (!activeOverlayFileName.isEmpty()) {
                        overlayDir = overlaysDir.resolve(activeOverlayFileName);
                    } else {
                        overlayDir = null;
                    }
                } catch (IOException e) {
                    throw new ServerMigrationFailureException("failed to read overlays file", e);
                }
            } else {
                overlayDir = null;
            }
            // add add-ons
            final Path baseAddonDir = modulesDir.resolve("system").resolve("add-ons");
            if (Files.exists(baseAddonDir)) {
                try (Stream<Path> stream = Files.list(baseAddonDir)) {
                    addonDirs = stream
                            .filter(Files::isDirectory)
                            .collect(Collectors.toList());
                } catch (IOException e) {
                    throw new ServerMigrationFailureException("failed to read add-ons directory", e);
                }
            } else {
                addonDirs = null;
            }
        }

        public Path getModulesDir() {
            return modulesDir;
        }

        public Module getModule(ModuleIdentifier moduleId) throws ServerMigrationFailureException {
            final Path moduleDir = getModuleDir(moduleId);
            if (!Files.exists(moduleDir)) {
                return null;
            }
            final Path moduleSpecPath = moduleDir.resolve("module.xml");
            if (!Files.exists(moduleSpecPath)) {
                return null;
            }
            final ModuleSpecification moduleSpecification;
            try {
                moduleSpecification = ModuleSpecification.Parser.parse(moduleSpecPath);
            } catch (XMLStreamException | IOException e) {
                throw new ServerMigrationFailureException(e);
            }
            return new Module(moduleDir, moduleSpecification);
        }

        public Module getModule(String moduleId) throws ServerMigrationFailureException {
            return getModule(ModuleIdentifier.fromString(moduleId));
        }

        public Path getModuleDir(ModuleIdentifier moduleId) {
            if (moduleId == null) {
                throw new IllegalArgumentException("The module identifier cannot be null.");
            }
            final Path modulePath = Paths.get(moduleId.getName().replace('.', File.separatorChar) +
                    File.separator +
                    moduleId.getSlot());
            if (overlayDir != null) {
                final Path overlayModuleDir = overlayDir.resolve(modulePath);
                if (Files.exists(overlayModuleDir)) {
                    return overlayModuleDir;
                }
            }
            for (Path layerDir : layerDirs) {
                final Path layerModuleDir = layerDir.resolve(modulePath);
                if (Files.exists(layerModuleDir)) {
                    return layerModuleDir;
                }
            }
            if (addonDirs != null) {
                for (Path addonDir : addonDirs) {
                    final Path addonModuleDir = addonDir.resolve(modulePath);
                    if (Files.exists(addonModuleDir)) {
                        return addonModuleDir;
                    }
                }
            }
            return modulesDir.resolve(modulePath);
        }
    }

    public static class Extensions {

        private final Map<String, Extension> extensionMap;

        protected Extensions(Builder builder) {
            this.extensionMap = Collections.unmodifiableMap(builder.extensionMap);
        }

        public Collection<Extension> getExtensions() {
            return extensionMap.values();
        }

        public Set<String> getExtensionModuleNames() {
            return extensionMap.keySet();
        }

        public Extension getExtension(String moduleName) {
            return extensionMap.get(moduleName);
        }

        public abstract static class Builder<T extends Builder<T>> {

            private final Map<String, Extension> extensionMap = new HashMap<>();

            protected abstract T getThis();

            public T extension(Extension extension) {
                this.extensionMap.put(extension.getModule(), extension);
                return getThis();
            }

            public T extension(Extension.Builder extensionBuilder) {
                return extension(extensionBuilder.build());
            }

            public T extensions(Extensions extensions) {
                this.extensionMap.putAll(extensions.extensionMap);
                return getThis();
            }

            public T extensionsExcept(Extensions extensions, String... moduleNamesToExclude) {
                for (Extension extension : extensions.getExtensions()) {
                    boolean exclude = false;
                    if (moduleNamesToExclude != null) {
                        for (String moduleName : moduleNamesToExclude) {
                            if (moduleName.equals(extension.getModule())) {
                                exclude = true;
                                break;
                            }
                        }
                    }
                    if (!exclude) {
                        this.extensionMap.put(extension.getModule(), extension);
                    }
                }
                return getThis();
            }

            public Extensions build() {
                return new Extensions(this);
            }
        }

        private static class DefaultBuilder extends Builder {
            @Override
            protected DefaultBuilder getThis() {
                return this;
            }
        }

        public static Builder builder() {
            return new DefaultBuilder();
        }
    }

    public static class Environment {

        public static String PROPERTIES_PREFIX = "server.";

        public static String PROPERTIES_DOMAIN_PREFIX = "domain.";
        public static String PROPERTY_DOMAIN_BASE_DIR = PROPERTIES_DOMAIN_PREFIX + "domainDir";
        public static String PROPERTY_DOMAIN_CONFIG_DIR = PROPERTIES_DOMAIN_PREFIX + "configDir";
        public static String PROPERTY_DOMAIN_DATA_DIR = PROPERTIES_DOMAIN_PREFIX + "dataDir";
        public static String PROPERTY_DOMAIN_CONTENT_DIR = PROPERTIES_DOMAIN_PREFIX + "contentDir";

        public static String PROPERTY_DOMAIN_DOMAIN_CONFIG_FILES = PROPERTIES_DOMAIN_PREFIX + "domainConfigFiles";
        public static String PROPERTY_DOMAIN_HOST_CONFIG_FILES = PROPERTIES_DOMAIN_PREFIX + "hostConfigFiles";

        public static String PROPERTIES_STANDALONE_PREFIX = "standalone.";
        public static String PROPERTY_STANDALONE_SERVER_DIR = PROPERTIES_STANDALONE_PREFIX + "serverDir";
        public static String PROPERTY_STANDALONE_CONFIG_DIR = PROPERTIES_STANDALONE_PREFIX + "configDir";
        public static String PROPERTY_STANDALONE_DATA_DIR = PROPERTIES_STANDALONE_PREFIX + "dataDir";
        public static String PROPERTY_STANDALONE_CONTENT_DIR = PROPERTIES_STANDALONE_PREFIX + "contentDir";
        public static String PROPERTY_STANDALONE_CONFIG_FILES = PROPERTIES_STANDALONE_PREFIX + "configFiles";

        private final String domainBaseDir;
        private final String domainConfigDir;
        private final String domainDataDir;
        private final String domainContentDir;
        private final List<String> domainConfigFiles;
        private final List<String> hostConfigFiles;
        private final String standaloneServerDir;
        private final String standaloneConfigDir;
        private final String standaloneDataDir;
        private final String standaloneContentDir;
        private final List<String> standaloneConfigFiles;

        public Environment(JBossServer server) {
            final MigrationEnvironment migrationEnvironment = server.getMigrationEnvironment();
            final String serverMigrationName = server.getMigrationName();
            domainBaseDir = migrationEnvironment.getPropertyAsString(getFullEnvironmentPropertyName(serverMigrationName, PROPERTY_DOMAIN_BASE_DIR));
            domainConfigDir = migrationEnvironment.getPropertyAsString(getFullEnvironmentPropertyName(serverMigrationName, PROPERTY_DOMAIN_CONFIG_DIR));
            domainDataDir = migrationEnvironment.getPropertyAsString(getFullEnvironmentPropertyName(serverMigrationName, PROPERTY_DOMAIN_DATA_DIR));
            domainContentDir = migrationEnvironment.getPropertyAsString(getFullEnvironmentPropertyName(serverMigrationName, PROPERTY_DOMAIN_CONTENT_DIR));
            domainConfigFiles = migrationEnvironment.getPropertyAsList(getFullEnvironmentPropertyName(serverMigrationName, PROPERTY_DOMAIN_DOMAIN_CONFIG_FILES));
            hostConfigFiles = migrationEnvironment.getPropertyAsList(getFullEnvironmentPropertyName(serverMigrationName, PROPERTY_DOMAIN_HOST_CONFIG_FILES));
            standaloneServerDir = migrationEnvironment.getPropertyAsString(getFullEnvironmentPropertyName(serverMigrationName, PROPERTY_STANDALONE_SERVER_DIR));
            standaloneConfigDir = migrationEnvironment.getPropertyAsString(getFullEnvironmentPropertyName(serverMigrationName, PROPERTY_STANDALONE_CONFIG_DIR));
            standaloneDataDir = migrationEnvironment.getPropertyAsString(getFullEnvironmentPropertyName(serverMigrationName, PROPERTY_STANDALONE_DATA_DIR));
            standaloneContentDir = migrationEnvironment.getPropertyAsString(getFullEnvironmentPropertyName(serverMigrationName, PROPERTY_STANDALONE_CONTENT_DIR));
            standaloneConfigFiles = migrationEnvironment.getPropertyAsList(getFullEnvironmentPropertyName(serverMigrationName, PROPERTY_STANDALONE_CONFIG_FILES));
        }

        public String getEnvironmentDomainBaseDir() {
            return domainBaseDir;
        }

        public String getEnvironmentDomainConfigDir() {
            return domainConfigDir;
        }

        public String getEnvironmentDomainDataDir() {
            return domainDataDir;
        }

        public String getEnvironmentDomainContentDir() {
            return domainContentDir;
        }

        public List<String> getEnvironmentDomainConfigFiles() {
            return domainConfigFiles;
        }

        public List<String> getEnvironmentHostConfigFiles() {
            return hostConfigFiles;
        }

        public String getEnvironmentStandaloneServerDir() {
            return standaloneServerDir;
        }

        public String getEnvironmentStandaloneConfigDir() {
            return standaloneConfigDir;
        }

        public String getEnvironmentStandaloneDataDir() {
            return standaloneDataDir;
        }

        public String getEnvironmentStandaloneContentDir() {
            return standaloneContentDir;
        }

        public List<String> getEnvironmentStandaloneConfigFiles() {
            return standaloneConfigFiles;
        }

        public String getDefaultDomainBaseDir() {
            return "domain";
        }

        public String getDefaultDomainConfigDir() {
            return "configuration";
        }

        public String getDefaultDomainDataDir() {
            return "data";
        }

        public String getDefaultDomainContentDir() {
            return "content";
        }

        public String getDefaultStandaloneServerDir() {
            return "standalone";
        }

        public String getDefaultStandaloneConfigDir() {
            return "configuration";
        }

        public String getDefaultStandaloneDataDir() {
            return "data";
        }

        public String getDefaultStandaloneContentDir() {
            return "content";
        }

        public String getDomainBaseDir() {
            return domainBaseDir == null ? getDefaultDomainBaseDir() : domainBaseDir;
        }

        public String getDomainConfigDir() {
            return domainConfigDir == null ? getDefaultDomainConfigDir() : domainConfigDir;
        }

        public String getDomainDataDir() {
            return domainDataDir == null ? getDefaultDomainDataDir() : domainDataDir;
        }

        public String getDomainContentDir() {
            return domainContentDir == null ? getDefaultDomainContentDir() : domainContentDir;
        }

        public List<String> getDomainConfigFiles() {
            return getEnvironmentDomainConfigFiles();
        }

        public List<String> getHostConfigFiles() {
            return getEnvironmentHostConfigFiles();
        }

        public String getStandaloneServerDir() {
            return standaloneServerDir == null ? getDefaultStandaloneServerDir() : standaloneServerDir;
        }

        public String getStandaloneConfigDir() {
            return standaloneConfigDir == null ? getDefaultStandaloneConfigDir() : standaloneConfigDir;
        }

        public String getStandaloneDataDir() {
            return standaloneDataDir == null ? getDefaultStandaloneDataDir() : standaloneDataDir;
        }

        public String getStandaloneContentDir() {
            return standaloneContentDir == null ? getDefaultStandaloneContentDir() : standaloneContentDir;
        }

        public List<String> getStandaloneConfigFiles() {
            return getEnvironmentStandaloneConfigFiles();
        }

        public boolean isDefaultDomainBaseDir() {
            return getEnvironmentDomainBaseDir() == null || getEnvironmentDomainBaseDir().equals(getDefaultDomainBaseDir());
        }

        public boolean isDefaultDomainConfigDir() {
            return getEnvironmentDomainConfigDir() == null || getEnvironmentDomainConfigDir().equals(getDefaultDomainConfigDir());
        }

        public boolean isDefaultDomainDataDir() {
            return getEnvironmentDomainDataDir() == null || getEnvironmentDomainDataDir().equals(getDefaultDomainDataDir());
        }

        public boolean isDefaultDomainContentDir() {
            return getEnvironmentDomainContentDir() == null || getEnvironmentDomainContentDir().equals(getDefaultDomainContentDir());
        }

        public boolean isDefaultStandaloneServerDir() {
            return getEnvironmentStandaloneServerDir() == null || getEnvironmentStandaloneServerDir().equals(getDefaultStandaloneServerDir());
        }

        public boolean isDefaultStandaloneConfigDir() {
            return getEnvironmentStandaloneConfigDir() == null || getEnvironmentStandaloneConfigDir().equals(getDefaultStandaloneConfigDir());
        }

        public boolean isDefaultStandaloneDataDir() {
            return getEnvironmentStandaloneDataDir() == null || getEnvironmentStandaloneDataDir().equals(getDefaultStandaloneDataDir());
        }

        public boolean isDefaultStandaloneContentDir() {
            return getEnvironmentStandaloneContentDir() == null || getEnvironmentStandaloneContentDir().equals(getDefaultStandaloneContentDir());
        }

        public static String getFullEnvironmentPropertyName(String serverMigrationName, String propertyName) {
            return PROPERTIES_PREFIX + serverMigrationName + "." + propertyName;
        }
    }
}
