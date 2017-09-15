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

package org.jboss.migration.wfly10.config.task.update;

import org.jboss.migration.core.env.MigrationEnvironment;
import org.jboss.migration.core.env.SkippableByEnvServerMigrationTask;
import org.jboss.migration.core.jboss.JBossServerConfiguration;
import org.jboss.migration.core.task.ServerMigrationTask;
import org.jboss.migration.core.task.ServerMigrationTaskName;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.core.util.xml.XMLFileFilter;
import org.jboss.migration.core.util.xml.XMLFiles;
import org.jboss.migration.wfly10.config.task.ServerConfigurationMigration;
import org.jboss.migration.wfly10.config.task.subsystem.EnvironmentProperties;
import org.jboss.migration.wfly10.config.task.subsystem.Extension;
import org.jboss.migration.wfly10.config.task.subsystem.ExtensionBuilder;
import org.jboss.migration.wfly10.config.task.subsystem.Subsystem;

import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author emmartins
 */
public class RemoveUnsupportedSubsystems<S> implements ServerConfigurationMigration.XMLConfigurationSubtaskFactory<S> {

    public static final ServerMigrationTaskName XML_CONFIG_SERVER_MIGRATION_TASK_NAME = new ServerMigrationTaskName.Builder("subsystems.remove-unsupported-subsystems").build();
    public static final String SERVER_MIGRATION_TASK_NAME_REMOVE_SUBSYSTEM = XML_CONFIG_SERVER_MIGRATION_TASK_NAME.getName()+".remove-unsupported-subsystem";
    public static final String SERVER_MIGRATION_TASK_NAME_REMOVE_EXTENSION = XML_CONFIG_SERVER_MIGRATION_TASK_NAME.getName()+".remove-unsupported-extension";
    public static final String SERVER_MIGRATION_TASK_NAME_ATTRIBUTE_MODULE = "module";
    public static final String SERVER_MIGRATION_TASK_NAME_ATTRIBUTE_NAMESPACE = "namespace";

    private final List<Extension> supportedExtensions;

    protected RemoveUnsupportedSubsystems(Builder<S> builder) {
        this.supportedExtensions = Collections.unmodifiableList(builder.supportedExtensions);
    }

    @Override
    public ServerMigrationTask getTask(final S source, final JBossServerConfiguration targetConfigurationPath) {
        final ServerMigrationTask task = new ServerMigrationTask() {
            @Override
            public ServerMigrationTaskName getName() {
                return XML_CONFIG_SERVER_MIGRATION_TASK_NAME;
            }
            @Override
            public ServerMigrationTaskResult run(TaskContext context) {
                //context.getConsoleWrapper().printf("%n%n");
                context.getLogger().debugf("Searching for extensions and subsystems not supported by the target server...");
                removeExtensionsAndSubsystems(source, targetConfigurationPath.getPath(), context);
                if (!context.hasSucessfulSubtasks()) {
                    context.getLogger().debugf("No unsupported extensions and subsystems found.");
                }
                return ServerMigrationTaskResult.SUCCESS;
            }
        };
        return new SkippableByEnvServerMigrationTask(task);
    }

    protected void removeExtensionsAndSubsystems(final S source, final Path xmlConfigurationPath, final TaskContext context) {
        final List<Extension> migrationExtensions = getMigrationExtensions(context.getMigrationEnvironment());
        final List<Subsystem> migrationSubsystems = getMigrationSubsystems(migrationExtensions, context.getMigrationEnvironment());
        final Set<String> extensionsRemoved = new HashSet<>();
        final Set<String> subsystemsRemoved = new HashSet<>();
        // setup the extensions filter
        final XMLFileFilter extensionsFilter = (startElement, xmlEventReader, xmlEventWriter) -> {
            if (startElement.getName().getLocalPart().equals("extension")) {
                Attribute moduleAttr = startElement.getAttributeByName(new QName("module"));
                final String moduleName = moduleAttr.getValue();
                // keep if module matches a supported extension name
                for (Extension extension : migrationExtensions) {
                    if (extension.getName().equals(moduleName)) {
                        return XMLFileFilter.Result.KEEP;
                    }
                }
                // not supported, remove it
                final ServerMigrationTaskName subtaskName = new ServerMigrationTaskName.Builder(SERVER_MIGRATION_TASK_NAME_REMOVE_EXTENSION).addAttribute(SERVER_MIGRATION_TASK_NAME_ATTRIBUTE_MODULE, moduleName).build();
                final ServerMigrationTask subtask = new ServerMigrationTask() {
                    @Override
                    public ServerMigrationTaskName getName() {
                        return subtaskName;
                    }

                    @Override
                    public ServerMigrationTaskResult run(TaskContext context1) {
                        context1.getLogger().debugf("Extension with module %s removed.", moduleName);
                        extensionsRemoved.add(moduleName);
                        return ServerMigrationTaskResult.SUCCESS;
                    }
                };
                context.execute(subtask);
                return XMLFileFilter.Result.REMOVE;
            } else {
                return XMLFileFilter.Result.NOT_APPLICABLE;
            }
        };
        // setup subsystems filter
        final XMLFileFilter subsystemsFilter = (startElement, xmlEventReader, xmlEventWriter) -> {
            if (startElement.getName().getLocalPart().equals("subsystem")) {
                final String namespaceURI = startElement.getName().getNamespaceURI();
                // keep if the namespace uri starts with a supported subsystem's namespace without version
                for (Subsystem subsystem : migrationSubsystems) {
                    final String namespaceWithoutVersion = subsystem.getNamespaceWithoutVersion();
                    if (namespaceWithoutVersion != null && namespaceURI.startsWith(namespaceWithoutVersion + ':')) {
                        return XMLFileFilter.Result.KEEP;
                    }
                }
                // not supported, remove subsystem
                final ServerMigrationTaskName subtaskName = new ServerMigrationTaskName.Builder(SERVER_MIGRATION_TASK_NAME_REMOVE_SUBSYSTEM).addAttribute(SERVER_MIGRATION_TASK_NAME_ATTRIBUTE_NAMESPACE, namespaceURI).build();
                final ServerMigrationTask subtask = new ServerMigrationTask() {
                    @Override
                    public ServerMigrationTaskName getName() {
                        return subtaskName;
                    }

                    @Override
                    public ServerMigrationTaskResult run(TaskContext context12) {
                        context12.getLogger().debugf("Subsystem with namespace %s removed.", namespaceURI);
                        subsystemsRemoved.add(namespaceURI);
                        return ServerMigrationTaskResult.SUCCESS;
                    }
                };
                context.execute(subtask);
                return XMLFileFilter.Result.REMOVE;
            } else {
                return XMLFileFilter.Result.NOT_APPLICABLE;
            }
        };
        XMLFiles.filter(xmlConfigurationPath, extensionsFilter, subsystemsFilter);
        context.getLogger().infof("Removed the following unsupported extensions: %s", extensionsRemoved);
        context.getLogger().infof("Removed the following unsupported subsystems: %s", subsystemsRemoved);

    }

    private List<Extension> getMigrationExtensions(MigrationEnvironment migrationEnvironment) {
        final List<String> removedByEnv = migrationEnvironment.getPropertyAsList(EnvironmentProperties.EXTENSIONS_REMOVE);
        if (removedByEnv == null || removedByEnv.isEmpty()) {
            return supportedExtensions;
        } else {
            final List<Extension> migrationExtensions = new ArrayList<>();
            for (Extension supportedExtension : supportedExtensions) {
                if (!removedByEnv.contains(supportedExtension.getName())) {
                    migrationExtensions.add(supportedExtension);
                }
            }
            return migrationExtensions;
        }
    }

    private List<Subsystem> getMigrationSubsystems(List<Extension> migrationExtensions, MigrationEnvironment migrationEnvironment) {
        final List<String> removedByEnv = migrationEnvironment.getPropertyAsList(EnvironmentProperties.SUBSYSTEMS_REMOVE);
        List<Subsystem> migrationSubsystems = new ArrayList<>();
        for (Extension extension : migrationExtensions) {
            for (Subsystem subsystem : extension.getSubsystems()) {
                if (removedByEnv == null || !removedByEnv.contains(subsystem.getName())) {
                    migrationSubsystems.add(subsystem);
                }
            }
        }
        return migrationSubsystems;
    }

    public static class Builder<S> {

        private final List<Extension> supportedExtensions = new ArrayList<>();

        public Builder<S> extension(Extension extension) {
            supportedExtensions.add(extension);
            return this;
        }

        public Builder<S> extension(ExtensionBuilder extensionBuilder) {
            supportedExtensions.add(extensionBuilder.build());
            return this;
        }

        public Builder<S> extensions(Collection<Extension> extensions) {
            supportedExtensions.addAll(extensions);
            return this;
        }

        public RemoveUnsupportedSubsystems<S> build() {
            return new RemoveUnsupportedSubsystems<>(this);
        }
    }
}
