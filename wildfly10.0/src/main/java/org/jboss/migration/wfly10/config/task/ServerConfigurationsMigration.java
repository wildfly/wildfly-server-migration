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

import org.jboss.migration.core.Server;
import org.jboss.migration.core.ServerMigrationFailureException;
import org.jboss.migration.core.console.BasicResultHandlers;
import org.jboss.migration.core.console.ConsoleWrapper;
import org.jboss.migration.core.console.UserConfirmation;
import org.jboss.migration.core.jboss.JBossServerConfiguration;
import org.jboss.migration.core.task.ServerMigrationTask;
import org.jboss.migration.core.task.ServerMigrationTaskName;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.wfly10.WildFlyServer10;
import org.jboss.migration.wfly10.config.management.ManageableServerConfiguration;

import java.util.Collection;

import static org.jboss.migration.core.logger.ServerMigrationLogger.ROOT_LOGGER;

/**
 * Migration of multiple standalone config files.
 * @author emmartins
 */
public class ServerConfigurationsMigration<S extends Server, C, T extends ManageableServerConfiguration> {

    private final ServerMigrationTaskName taskName;
    private final SourceConfigurations<S, C> sourceConfigurations;
    private final ServerConfigurationMigration<C, T> configFileMigration;

    public ServerConfigurationsMigration(SourceConfigurations<S, C> sourceConfigurations, ServerConfigurationMigration<C, T> configFileMigration) {
        this.sourceConfigurations = sourceConfigurations;
        this.configFileMigration = configFileMigration;
        this.taskName = new ServerMigrationTaskName.Builder(configFileMigration.getConfigType()+"-configurations").build();
    }

    public ServerMigrationTask getServerMigrationTask(S source, WildFlyServer10 target, JBossServerConfiguration.Type targetConfigurationType) {
        return new Task<>(getServerMigrationTaskName(), sourceConfigurations.getConfigurations(source, target), target, targetConfigurationType, configFileMigration);
    }

    /**
     *
     * @return
     */
    protected ServerMigrationTaskName getServerMigrationTaskName() {
        return taskName;
    }

    protected static class Task<S, T extends ManageableServerConfiguration> implements ServerMigrationTask {

        private final ServerMigrationTaskName name;
        private final Collection<S> sourceConfigs;
        private final JBossServerConfiguration.Type targetConfigurationType;
        private final WildFlyServer10 target;
        private final ServerConfigurationMigration<S, T> configFileMigration;

        protected Task(ServerMigrationTaskName name, Collection<S> sourceConfigs, WildFlyServer10 target, JBossServerConfiguration.Type targetConfigurationType, ServerConfigurationMigration<S, T> configFileMigration) {
            this.name = name;
            this.sourceConfigs = sourceConfigs;
            this.targetConfigurationType = targetConfigurationType;
            this.target = target;
            this.configFileMigration = configFileMigration;
        }

        @Override
        public ServerMigrationTaskName getName() {
            return name;
        }

        @Override
        public ServerMigrationTaskResult run(final TaskContext taskContext) {
            final ConsoleWrapper consoleWrapper = taskContext.getConsoleWrapper();
            consoleWrapper.printf("%n");
            taskContext.getLogger().infof("Retrieving source's %s configurations...", configFileMigration.getConfigType());
            if (!sourceConfigs.isEmpty()) {
                for (S sourceConfig : sourceConfigs) {
                    taskContext.getLogger().infof("%s", sourceConfig);
                }
            } else {
                taskContext.getLogger().infof("No source's %s configurations found.", configFileMigration.getConfigType());
                return ServerMigrationTaskResult.SKIPPED;
            }

            if (taskContext.isInteractive()) {
                final BasicResultHandlers.UserConfirmation resultHandler = new BasicResultHandlers.UserConfirmation();
                new UserConfirmation(consoleWrapper, "Migrate all configurations?", ROOT_LOGGER.yesNo(), resultHandler).execute();
                switch (resultHandler.getResult()) {
                    case NO:
                        confirmAllConfigs(sourceConfigs, targetConfigurationType, target, taskContext);
                        break;
                    case YES:
                        migrateAllConfigs(sourceConfigs, targetConfigurationType, target, taskContext);
                        break;
                    case ERROR:
                        return run(taskContext);
                    default:
                        throw new ServerMigrationFailureException("unexpected user interaction result");
                }
            } else {
                migrateAllConfigs(sourceConfigs, targetConfigurationType, target, taskContext);
            }
            return taskContext.hasSucessfulSubtasks() ? ServerMigrationTaskResult.SUCCESS : ServerMigrationTaskResult.SKIPPED;
        }

        protected void migrateAllConfigs(Collection<S> sourceConfigs, final JBossServerConfiguration.Type targetConfigurationType, WildFlyServer10 target, final TaskContext taskContext) {
            for (S sourceConfig : sourceConfigs) {
                taskContext.execute(configFileMigration.getServerMigrationTask(sourceConfig, targetConfigurationType, target));
            }
        }

        protected void confirmAllConfigs(Collection<S> sourceConfigs, final JBossServerConfiguration.Type targetConfigurationType, WildFlyServer10 target, final TaskContext taskContext) {
            for (S sourceConfig : sourceConfigs) {
                confirmConfig(sourceConfig, targetConfigurationType, target, taskContext);
            }
        }

        protected void confirmConfig(final S sourceConfig, final JBossServerConfiguration.Type targetConfigurationType, final WildFlyServer10 target, final TaskContext taskContext) {
            final UserConfirmation.ResultHandler resultHandler = new UserConfirmation.ResultHandler() {
                @Override
                public void onNo() {
                }
                @Override
                public void onYes() {
                    taskContext.execute(configFileMigration.getServerMigrationTask(sourceConfig, targetConfigurationType, target));
                }
                @Override
                public void onError() {
                    // repeat
                    confirmConfig(sourceConfig, targetConfigurationType, target, taskContext);
                }
            };
            final ConsoleWrapper consoleWrapper = taskContext.getConsoleWrapper();
            new UserConfirmation(consoleWrapper, "Migrate configuration "+sourceConfig+" ?", ROOT_LOGGER.yesNo(), resultHandler).execute();
        }
    }

    /**
     * Component responsible for providing the source server's configurations.
     * @param <S> the source server type
     * @param <C> the source configuration type
     */
    public interface SourceConfigurations<S extends Server, C> {
        Collection<C> getConfigurations(S source, WildFlyServer10 target);
    }
}
