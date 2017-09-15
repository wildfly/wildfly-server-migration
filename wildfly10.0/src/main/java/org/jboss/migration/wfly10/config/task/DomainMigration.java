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
import org.jboss.migration.core.console.ConsoleWrapper;
import org.jboss.migration.core.console.UserConfirmationServerMigrationTask;
import org.jboss.migration.core.env.SkippableByEnvServerMigrationTask;
import org.jboss.migration.core.jboss.JBossServerConfiguration;
import org.jboss.migration.core.task.ServerMigrationTask;
import org.jboss.migration.core.task.ServerMigrationTaskName;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.wfly10.WildFlyServer10;

/**
 * Implementation for the domain migration.
 * @author emmartins
 * @param <S> the source server type
 */
public class DomainMigration<S extends Server> implements ServerMigration.SubtaskFactory<S> {

    public static final String DOMAIN = "domain";
    public static final ServerMigrationTaskName SERVER_MIGRATION_TASK_NAME = new ServerMigrationTaskName.Builder(DOMAIN).build();

    private final DomainConfigurationsMigration<S, ?> domainConfigurationsMigration;
    private final HostConfigurationsMigration<S, ?> hostConfigurationsMigration;

    public DomainMigration(Builder<S> builder) {
        this.domainConfigurationsMigration = builder.domainConfigurationsMigration;
        this.hostConfigurationsMigration = builder.hostConfigurationsMigration;
    }

    @Override
    public ServerMigrationTask getTask(final S source, final WildFlyServer10 target) {
        final ServerMigrationTask task = new ServerMigrationTask() {
            @Override
            public ServerMigrationTaskName getName() {
                return SERVER_MIGRATION_TASK_NAME;
            }

            @Override
            public ServerMigrationTaskResult run(TaskContext context) {
                final ConsoleWrapper consoleWrapper = context.getConsoleWrapper();
                consoleWrapper.printf("%n");
                context.getLogger().infof("Domain migration starting...");
                beforeConfigurationsMigration(source, target, context);
                if (domainConfigurationsMigration != null) {
                    context.execute(domainConfigurationsMigration.getServerMigrationTask(source, target, JBossServerConfiguration.Type.DOMAIN));
                }
                if (hostConfigurationsMigration != null) {
                    context.execute(hostConfigurationsMigration.getServerMigrationTask(source, target, JBossServerConfiguration.Type.HOST));
                }
                afterConfigurationsMigration(source, target, context);
                consoleWrapper.printf("%n");
                context.getLogger().infof("Domain migration done.");
                return context.hasSucessfulSubtasks() ? ServerMigrationTaskResult.SUCCESS : ServerMigrationTaskResult.SKIPPED;
            }
        };
        return new SkippableByEnvServerMigrationTask(new UserConfirmationServerMigrationTask(task, "Migrate the source's managed domain?"));
    }

    protected void beforeConfigurationsMigration(S source, WildFlyServer10 target, TaskContext context) {
    }

    protected void afterConfigurationsMigration(S source, WildFlyServer10 target, TaskContext context) {
    }

    public static class Builder<S extends Server>  {

        private DomainConfigurationsMigration<S, ?> domainConfigurationsMigration;
        private HostConfigurationsMigration<S, ?> hostConfigurationsMigration;

        public Builder<S> domainConfigurations(DomainConfigurationsMigration<S, ?> domainConfigurationsMigration) {
            this.domainConfigurationsMigration = domainConfigurationsMigration;
            return this;
        }

        public Builder<S> hostConfigurations(HostConfigurationsMigration<S, ?> hostConfigurationsMigration) {
            this.hostConfigurationsMigration = hostConfigurationsMigration;
            return this;
        }

        public DomainMigration<S> build() {
            return new DomainMigration<>(this);
        }
    }
}
