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

package org.jboss.migration.eap7.to.eap7;

import org.jboss.migration.eap.EAPServer7_2;
import org.jboss.migration.eap.EAPServerMigrationProvider7_4;
import org.jboss.migration.eap.task.hostexclude.EAP7_4AddHostExcludes;
import org.jboss.migration.eap.task.subsystem.metrics.EAP7_4AddMetricsSubsystem;
import org.jboss.migration.wfly.task.subsystem.health.WildFly22_0AddHealthSubsystem;
import org.jboss.migration.wfly.task.update.WildFly22_0UpdateInfinispanSubsystem;
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
 * Server migration, from EAP 7.2 to EAP 7.4.
 * @author emmartins
 */
public class EAP7_2ToEAP7_4ServerMigrationProvider implements EAPServerMigrationProvider7_4 {

    @Override
    public WildFlyServerMigration10 getServerMigration() {
        final ServerUpdate.Builders<WildFlyServer10> serverUpdateBuilders = new ServerUpdate.Builders<>();
        return serverUpdateBuilders.serverUpdateBuilder()
                .standaloneServer(
                        serverUpdateBuilders.standaloneConfigurationBuilder()
                                .subtask(new RemoveUnsupportedExtensions<>())
                                .subtask(new RemoveUnsupportedSubsystems<>())
                                .subtask(new MigrateReferencedModules<>())
                                .subtask(new MigrateReferencedPaths<>())
                                .subtask(new WildFly22_0UpdateInfinispanSubsystem<>())
                                .subtask(new WildFly22_0AddHealthSubsystem<>())
                                .subtask(new EAP7_4AddMetricsSubsystem<>())
                                .subtask(new MigrateCompatibleSecurityRealms<>())
                                .subtask(new MigrateDeployments<>()))
                .domain(serverUpdateBuilders.domainBuilder()
                        .domainConfigurations(serverUpdateBuilders.domainConfigurationBuilder()
                                .subtask(new RemoveUnsupportedExtensions<>())
                                .subtask(new RemoveUnsupportedSubsystems<>())
                                .subtask(new MigrateReferencedModules<>())
                                .subtask(new MigrateReferencedPaths<>())
                                .subtask(new WildFly22_0UpdateInfinispanSubsystem<>())
                                .subtask(new EAP7_4AddHostExcludes<>())
                                .subtask(new MigrateDeployments<>()))
                        .hostConfigurations(serverUpdateBuilders.hostConfigurationBuilder()
                                .subtask(new MigrateReferencedModules<>())
                                .subtask(new MigrateReferencedPaths<>())
                                .subtask(serverUpdateBuilders.hostBuilder()
                                        .subtask(new MigrateCompatibleSecurityRealms<>()))))
                .build();
    }

    @Override
    public Class<EAPServer7_2> getSourceType() {
        return EAPServer7_2.class;
    }
}
