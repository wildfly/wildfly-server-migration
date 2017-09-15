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

package org.jboss.migration.wfly10.config.task;

import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.migration.core.console.ConsoleWrapper;
import org.jboss.migration.core.jboss.JBossServerConfiguration;
import org.jboss.migration.core.task.ServerMigrationTask;
import org.jboss.migration.core.task.ServerMigrationTaskName;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.wfly10.WildFlyServer10;
import org.jboss.migration.wfly10.config.management.ManageableResource;
import org.jboss.migration.wfly10.config.management.ManageableServerConfiguration;
import org.jboss.migration.wfly10.config.task.factory.ManageableServerConfigurationTaskFactory;
import org.jboss.migration.wfly10.config.task.management.configuration.ManageableServerConfigurationComponentTaskBuilder;
import org.jboss.migration.wfly10.config.task.management.resource.ManageableResourceComponentTaskBuilder;
import org.jboss.migration.wfly10.config.task.management.resources.ManageableResourcesComponentTaskBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * Implementation of a server config migration.
 * @param <S> the source for the configuration
 * @param <T> the manageable config type
 * @author emmartins
 */
public class ServerConfigurationMigration<S, T extends ManageableServerConfiguration> {

    public static final String MIGRATION_REPORT_TASK_ATTR_SOURCE = "source";

    private final String configType;
    protected final XMLConfigurationProvider xmlConfigurationProvider;
    protected final ManageableConfigurationProvider<T> manageableConfigurationProvider;
    protected final List<ManageableServerConfigurationTaskFactory<S, T>> manageableConfigurationSubtaskFactories;
    protected final List<XMLConfigurationSubtaskFactory<S>> xmlConfigurationSubtaskFactories;

    protected ServerConfigurationMigration(BaseBuilder<S, T, ?> builder) {
        this.configType = builder.configType;
        this.xmlConfigurationProvider = builder.xmlConfigurationProvider;
        this.manageableConfigurationProvider = builder.manageableConfigurationProvider;
        this.manageableConfigurationSubtaskFactories = Collections.unmodifiableList(builder.manageableConfigurationSubtaskFactories.factories);
        this.xmlConfigurationSubtaskFactories = Collections.unmodifiableList(builder.xmlConfigurationSubtaskFactories);
    }

    public String getConfigType() {
        return configType;
    }

    protected ServerMigrationTask getServerMigrationTask(final S source, final JBossServerConfiguration.Type targetConfigurationType, final WildFlyServer10 target) {
        final ServerMigrationTaskName taskName = new ServerMigrationTaskName.Builder(getConfigType()+"-configuration").addAttribute(MIGRATION_REPORT_TASK_ATTR_SOURCE, source.toString()).build();
        return new ServerMigrationTask() {
            @Override
            public ServerMigrationTaskName getName() {
                return taskName;
            }

            @Override
            public ServerMigrationTaskResult run(TaskContext context) {
                final ConsoleWrapper consoleWrapper = context.getConsoleWrapper();
                consoleWrapper.printf("%n");
                context.getLogger().infof("Migrating %s configuration %s", getConfigType(), source);
                // create xml config
                final JBossServerConfiguration targetConfiguration = xmlConfigurationProvider.getXMLConfiguration(source, targetConfigurationType, target, context);
                // execute xml config subtasks
                for (XMLConfigurationSubtaskFactory subtaskFactory : xmlConfigurationSubtaskFactories) {
                    final ServerMigrationTask subtask = subtaskFactory.getTask(source, targetConfiguration);
                    if (subtask != null) {
                        context.execute(subtask);
                    }
                }
                // config through management
                if (manageableConfigurationProvider != null) {
                    final T configurationManagement = manageableConfigurationProvider.getManageableConfiguration(targetConfiguration, target);
                    //context.getConsoleWrapper().printf("%n%n");
                    context.getLogger().debugf("Starting target configuration %s", targetConfiguration.getPath().getFileName());
                    configurationManagement.start();
                    try {
                        final ModelNode op = Util.createEmptyOperation(READ_RESOURCE_OPERATION, null);
                        op.get(RECURSIVE).set(true);
                        op.get(INCLUDE_DEFAULTS).set(false);
                        //context.getLogger().tracef("Configuration resource description: %s", configurationManagement.executeManagementOperation(op));
                        // execute config management subtasks
                        for (ManageableServerConfigurationTaskFactory subtaskFactory : manageableConfigurationSubtaskFactories) {
                            final ServerMigrationTask subtask = subtaskFactory.getTask(source, configurationManagement);
                            if (subtask != null) {
                                context.execute(subtask);
                            }
                        }
                    } finally {
                        configurationManagement.stop();
                    }
                }
                //consoleWrapper.printf("%n");
                return ServerMigrationTaskResult.SUCCESS;
            }
        };
    }

    /**
     * Component responsible for providing the target XML configuration.
     * @param <S>
     */
    public interface XMLConfigurationProvider<S> {
        JBossServerConfiguration getXMLConfiguration(S source, final JBossServerConfiguration.Type targetConfigurationType, WildFlyServer10 target, TaskContext context);
    }

    /**
     * Provider for the manageable configuration
     * @param <T>
     */
    public interface ManageableConfigurationProvider<T extends ManageableServerConfiguration> {
        T getManageableConfiguration(JBossServerConfiguration targetConfigFilePath, WildFlyServer10 target);
    }

    public interface XMLConfigurationSubtaskFactory<S> {
        ServerMigrationTask getTask(S source, JBossServerConfiguration targetConfigFilePath);
    }


    /**
     * The ServerConfigurationMigration ext base builder.
     * @param <S> the source for the configuration
     * @param <T> the manageable config type
     */
    public abstract static class BaseBuilder<S, T extends ManageableServerConfiguration, B extends BaseBuilder<S, T, B>> {

        private final String configType;
        private final XMLConfigurationProvider<S> xmlConfigurationProvider;
        private ManageableConfigurationProvider<T> manageableConfigurationProvider;
        private final ManageableServerConfigurationTaskFactories<S, T> manageableConfigurationSubtaskFactories;
        private final List<XMLConfigurationSubtaskFactory<S>> xmlConfigurationSubtaskFactories;

        public BaseBuilder(String configType, XMLConfigurationProvider<S> xmlConfigurationProvider) {
            this.configType = configType;
            this.xmlConfigurationProvider = xmlConfigurationProvider;
            manageableConfigurationSubtaskFactories = new ManageableServerConfigurationTaskFactories<>();
            xmlConfigurationSubtaskFactories = new ArrayList<>();
        }

        public B manageableConfigurationProvider(ManageableConfigurationProvider<T> manageableConfigurationProvider) {
            this.manageableConfigurationProvider = manageableConfigurationProvider;
            return getThis();
        }

        public B subtask(ManageableServerConfigurationTaskFactory<S, T> subtaskFactory) {
            manageableConfigurationSubtaskFactories.add(subtaskFactory);
            return getThis();
        }

        public B subtask(ManageableResourceComponentTaskBuilder<S, ManageableResource, ?> subtaskBuilder) {
            return subtask(ManageableServerConfigurationTaskFactory.of(subtaskBuilder));
        }

        public B subtask(ManageableResourcesComponentTaskBuilder<S, ManageableResource, ?> subtaskBuilder) {
            return subtask(ManageableServerConfigurationTaskFactory.of(subtaskBuilder));
        }

        public B subtask(ManageableServerConfigurationComponentTaskBuilder<S, ?> subtaskBuilder) {
            return subtask(ManageableServerConfigurationTaskFactory.of(subtaskBuilder));
        }

        public B subtask(XMLConfigurationSubtaskFactory<S> subtaskFactory) {
            xmlConfigurationSubtaskFactories.add(subtaskFactory);
            return getThis();
        }

        protected abstract B getThis();

        public ServerConfigurationMigration<S, T> build() {
            return new ServerConfigurationMigration(this);
        }
    }

    /**
     * The ServerConfigurationMigration concrete builder.
     * @param <S> the source for the configuration
     * @param <T> the manageable config type
     */
    public static class Builder<S, T extends ManageableServerConfiguration> extends BaseBuilder<S, T, Builder<S, T>> {

        public Builder(String configType, XMLConfigurationProvider xmlConfigurationProvider) {
            super(configType, xmlConfigurationProvider);
        }

        @Override
        protected Builder<S, T> getThis() {
            return this;
        }
    }

    public static class ManageableServerConfigurationTaskFactories<S, T extends ManageableServerConfiguration> {

        private final List<ManageableServerConfigurationTaskFactory<S, T>> factories;

        public ManageableServerConfigurationTaskFactories() {
            this.factories = new ArrayList<>();
        }

        public List<ManageableServerConfigurationTaskFactory<S, T>> getFactories() {
            return Collections.unmodifiableList(factories);
        }

        public void add(ManageableServerConfigurationTaskFactory<S, T> subtaskFactory) {
            factories.add(subtaskFactory);
        }

        public void addAll(Collection<ManageableServerConfigurationTaskFactory<S, T>> subtaskFactories) {
            factories.addAll(subtaskFactories);
        }
    }
}
