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

import org.jboss.migration.core.jboss.JBossServer;
import org.jboss.migration.core.jboss.JBossServerConfiguration;
import org.jboss.migration.core.jboss.ModulesMigrationTask;
import org.jboss.migration.wfly10.config.task.DomainConfigurationMigration;
import org.jboss.migration.wfly10.config.task.HostConfigurationMigration;
import org.jboss.migration.wfly10.config.task.MigrationBuilders;
import org.jboss.migration.wfly10.config.task.ServerConfigurationMigration;
import org.jboss.migration.wfly10.config.task.ServerMigration;
import org.jboss.migration.wfly10.config.task.StandaloneServerConfigurationMigration;

/**
 * @author emmartins
 */
public class ServerUpdate<S extends JBossServer<S>> extends ServerMigration<S> {

    public ServerUpdate(ServerMigration.Builder<S> builder) {
        super(builder);
    }

    public static class Builder<S extends JBossServer<S>> extends ServerMigration.Builder<S> {

        public Builder() {
            subtask((source, target) -> new ModulesMigrationTask(source, target));
        }

        @Override
        public Builder<S> subtask(SubtaskFactory<S> subtaskFactory) {
            super.subtask(subtaskFactory);
            return this;
        }

        public Builder<S> domain(DomainUpdate<S> domainUpdate) {
            return subtask(domainUpdate);
        }

        public Builder<S> domain(DomainUpdate.Builder<S> domainUpdateBuilder) {
            return domain(domainUpdateBuilder.build());
        }

        public Builder<S> standaloneServer(StandaloneServerUpdate<S> standaloneServerUpdate) {
            return subtask(standaloneServerUpdate);
        }

        public Builder<S> standaloneServer(StandaloneServerConfigurationsUpdate<S> configurationsMigration) {
            return standaloneServer(new StandaloneServerUpdate<>(configurationsMigration));
        }

        public Builder<S> standaloneServer(StandaloneServerConfigurationMigration<JBossServerConfiguration<S>> standaloneServerConfigurationUpdate) {
           return standaloneServer(new StandaloneServerConfigurationsUpdate<>(standaloneServerConfigurationUpdate));
        }

        public Builder<S> standaloneServer(StandaloneServerConfigurationMigration.Builder<JBossServerConfiguration<S>> standaloneServerConfigurationUpdateBuilder) {
            return standaloneServer(standaloneServerConfigurationUpdateBuilder.build());
        }

        public ServerUpdate<S> build() {
            return new ServerUpdate(this);
        }
    }

    public static class Builders<S extends JBossServer<S>> extends MigrationBuilders<S, JBossServerConfiguration<S>> {

        private final ServerConfigurationMigration.XMLConfigurationProvider<JBossServerConfiguration<S>> defaultXmlConfigurationProvider = new CopySourceXMLConfiguration<>();

        public ServerUpdate.Builder<S> serverUpdateBuilder() {
            return new ServerUpdate.Builder();
        }

        public DomainConfigurationMigration.Builder<JBossServerConfiguration<S>> domainConfigurationBuilder() {
            return new DomainConfigurationMigration.Builder<>(defaultXmlConfigurationProvider);
        }

        public HostConfigurationMigration.Builder<JBossServerConfiguration<S>> hostConfigurationBuilder() {
            return new HostConfigurationMigration.Builder<>(defaultXmlConfigurationProvider);
        }

        public StandaloneServerConfigurationMigration.Builder<JBossServerConfiguration<S>> standaloneConfigurationBuilder() {
            return new StandaloneServerConfigurationMigration.Builder<>(defaultXmlConfigurationProvider);
        }

        public DomainUpdate.Builder<S> domainBuilder() {
            return new DomainUpdate.Builder<>();
        }

        public HostUpdate.Builder<S> hostBuilder() {
            return new HostUpdate.Builder<>();
        }
    }
}
