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

import org.jboss.migration.wfly.task.hostexclude.WildFly23_0AddHostExcludes;
import org.jboss.migration.wfly.task.subsystem.health.WildFly23_0AddHealthSubsystem;
import org.jboss.migration.wfly.task.subsystem.metrics.WildFly23_0AddMetricsSubsystem;
import org.jboss.migration.wfly.task.subsystem.microprofile.WildFly23_0AddMicroprofileJwtSmallryeSubsystem;
import org.jboss.migration.wfly10.WildFlyServer10;
import org.jboss.migration.wfly10.WildFlyServerMigration10;
import org.jboss.migration.wfly10.config.task.module.MigrateReferencedModules;
import org.jboss.migration.wfly10.config.task.paths.MigrateReferencedPaths;
import org.jboss.migration.wfly10.config.task.update.MigrateCompatibleSecurityRealms;
import org.jboss.migration.wfly10.config.task.update.MigrateDeployments;
import org.jboss.migration.wfly10.config.task.update.RemoveUnsupportedExtensions;
import org.jboss.migration.wfly10.config.task.update.RemoveUnsupportedSubsystems;
import org.jboss.migration.wfly10.config.task.update.ServerUpdate;

/**
 * Server migration to WFLY 23.0, from WFLY 17.0.
 * @author emmartins
 */
public class WildFly17_0ToWildFly23_0ServerMigrationProvider implements WildFly23_0ServerMigrationProvider {

    @Override
    public WildFlyServerMigration10 getServerMigration() {
        final ServerUpdate.Builders<WildFlyServer10> serverUpdateBuilders = new ServerUpdate.Builders<>();
        return serverUpdateBuilders.serverUpdateBuilder()
                .standaloneServer(serverUpdateBuilders.standaloneConfigurationBuilder()
                        .subtask(new RemoveUnsupportedExtensions<>())
                        .subtask(new RemoveUnsupportedSubsystems<>())
                        .subtask(new MigrateReferencedModules<>())
                        .subtask(new MigrateReferencedPaths<>())
                        .subtask(new WildFly23_0AddHealthSubsystem<>())
                        .subtask(new WildFly23_0AddMetricsSubsystem<>())
                        .subtask(new WildFly23_0AddMicroprofileJwtSmallryeSubsystem<>())
                        .subtask(new MigrateCompatibleSecurityRealms<>())
                        .subtask(new MigrateDeployments<>()))
                .domain(serverUpdateBuilders.domainBuilder()
                        .domainConfigurations(serverUpdateBuilders.domainConfigurationBuilder()
                                .subtask(new RemoveUnsupportedExtensions<>())
                                .subtask(new RemoveUnsupportedSubsystems<>())
                                .subtask(new MigrateReferencedModules<>())
                                .subtask(new MigrateReferencedPaths<>())
                                .subtask(new WildFly23_0AddMicroprofileJwtSmallryeSubsystem<>())
                                .subtask(new WildFly23_0AddHostExcludes<>())
                                .subtask(new MigrateDeployments<>()))
                        .hostConfigurations(serverUpdateBuilders.hostConfigurationBuilder()
                                .subtask(new MigrateReferencedModules<>())
                                .subtask(new MigrateReferencedPaths<>())
                                .subtask(serverUpdateBuilders.hostBuilder()
                                        .subtask(new MigrateCompatibleSecurityRealms<>()))))
                .build();
    }

    @Override
    public Class<WildFly17_0Server> getSourceType() {
        return WildFly17_0Server.class;
    }
}
