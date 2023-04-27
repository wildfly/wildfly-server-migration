/*
 * Copyright 2021 Red Hat, Inc.
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
package org.jboss.migration.wfly;

import org.jboss.migration.wfly.task.hostexclude.WildFly27_0AddHostExcludes;
import org.jboss.migration.wfly.task.paths.WildFly26_0MigrateReferencedPaths;
import org.jboss.migration.wfly.task.xml.WildFly27_0MigrateJBossDomainProperties;
import org.jboss.migration.wfly10.WildFlyServer10;
import org.jboss.migration.wfly10.WildFlyServerMigration10;
import org.jboss.migration.wfly10.config.task.module.MigrateReferencedModules;
import org.jboss.migration.wfly10.config.task.update.RemoveUnsupportedExtensions;
import org.jboss.migration.wfly10.config.task.update.RemoveUnsupportedSubsystems;
import org.jboss.migration.wfly10.config.task.update.ServerUpdate;

/**
 * Server migration to WFLY 27.0, from WFLY 25.0.
 * @author emmartins
 */
public class WildFly25_0ToWildFly27_0ServerMigrationProvider implements WildFly27_0ServerMigrationProvider {

    @Override
    public WildFlyServerMigration10 getServerMigration() {
        final ServerUpdate.Builders<WildFlyServer10> serverUpdateBuilders = new ServerUpdate.Builders<>();
        return serverUpdateBuilders.serverUpdateBuilder()
                .standaloneServer(serverUpdateBuilders.standaloneConfigurationBuilder()
                        .subtask(new WildFly27_0MigrateJBossDomainProperties<>())
                        .subtask(new RemoveUnsupportedExtensions<>())
                        .subtask(new RemoveUnsupportedSubsystems<>())
                        .subtask(new MigrateReferencedModules<>())
                        .subtask(new WildFly26_0MigrateReferencedPaths<>())
                )
                .domain(serverUpdateBuilders.domainBuilder()
                        .domainConfigurations(serverUpdateBuilders.domainConfigurationBuilder()
                                .subtask(new WildFly27_0MigrateJBossDomainProperties<>())
                                .subtask(new RemoveUnsupportedExtensions<>())
                                .subtask(new RemoveUnsupportedSubsystems<>())
                                .subtask(new MigrateReferencedModules<>())
                                .subtask(new WildFly26_0MigrateReferencedPaths<>())
                                .subtask(new WildFly27_0AddHostExcludes<>())
                        )
                        .hostConfigurations(serverUpdateBuilders.hostConfigurationBuilder()
                                .subtask(new WildFly27_0MigrateJBossDomainProperties<>())
                                .subtask(new MigrateReferencedModules<>())
                                .subtask(new WildFly26_0MigrateReferencedPaths<>())
                        )
                ).build();
    }

    @Override
    public Class<WildFly25_0Server> getSourceType() {
        return WildFly25_0Server.class;
    }
}
