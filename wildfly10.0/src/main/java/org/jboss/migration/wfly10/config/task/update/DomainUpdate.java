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
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.wfly10.WildFlyServer10;
import org.jboss.migration.wfly10.config.task.DomainConfigurationMigration;
import org.jboss.migration.wfly10.config.task.DomainMigration;
import org.jboss.migration.wfly10.config.task.HostConfigurationMigration;

/**
 * @author emmartins
 */
public class DomainUpdate<S extends JBossServer<S>> extends DomainMigration<S> {

    public DomainUpdate(Builder<S> builder) {
        super(builder);
    }

    @Override
    protected void beforeConfigurationsMigration(S source, WildFlyServer10 target, TaskContext context) {
        // FIXME first let's migrate all domain's contents, to go around current limitation where content missing for deployment overlays fails to boot target server...
        context.execute(new MigrateContentDir<>("domain", source.getDomainContentDir(), target.getDomainContentDir()).build());
        super.beforeConfigurationsMigration(source, target, context);
    }

    public static class Builder<S extends JBossServer<S>> extends DomainMigration.Builder<S> {

        public Builder<S> domainConfigurations(DomainConfigurationsUpdate<S> domainConfigurationsUpdate) {
            super.domainConfigurations(domainConfigurationsUpdate);
            return this;
        }

        public Builder<S> domainConfigurations(DomainConfigurationMigration<JBossServerConfiguration<S>> domainConfigurationUpdate) {
            return domainConfigurations(new DomainConfigurationsUpdate<>(domainConfigurationUpdate));
        }

        public Builder<S> domainConfigurations(DomainConfigurationMigration.Builder<JBossServerConfiguration<S>> domainConfigurationUpdatebuilder) {
            return domainConfigurations(domainConfigurationUpdatebuilder.build());
        }

        public Builder<S> hostConfigurations(HostConfigurationsUpdate<S> hostConfigurationsUpdate) {
            super.hostConfigurations(hostConfigurationsUpdate);
            return this;
        }

        public Builder<S> hostConfigurations(HostConfigurationMigration<JBossServerConfiguration<S>> hostConfigurationUpdate) {
            return hostConfigurations(new HostConfigurationsUpdate<>(hostConfigurationUpdate));
        }

        public Builder<S> hostConfigurations(HostConfigurationMigration.Builder<JBossServerConfiguration<S>> hostConfigurationUpdateBuilder) {
            return hostConfigurations(hostConfigurationUpdateBuilder.build());
        }

        public DomainUpdate<S> build() {
            return new DomainUpdate<>(this);
        }
    }
}
