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
import org.jboss.migration.wfly10.WildFlyServer10;
import org.jboss.migration.wfly10.config.task.DomainConfigurationMigration;
import org.jboss.migration.wfly10.config.task.DomainConfigurationsMigration;

import java.util.Collection;

/**
 * @author emmartins
 */
class DomainConfigurationsUpdate<S extends JBossServer<S>> extends DomainConfigurationsMigration<S, JBossServerConfiguration<S>> {

    DomainConfigurationsUpdate(DomainConfigurationMigration<JBossServerConfiguration<S>> configurationMigration) {
        super(new SourceDomainConfigurations<>(), configurationMigration);
    }

    private static class SourceDomainConfigurations<S extends JBossServer<S>> implements SourceConfigurations<S, JBossServerConfiguration<S>> {
        @Override
        public Collection<JBossServerConfiguration<S>> getConfigurations(S source, WildFlyServer10 target) {
            return source.getDomainDomainConfigs();
        }
    }
}
