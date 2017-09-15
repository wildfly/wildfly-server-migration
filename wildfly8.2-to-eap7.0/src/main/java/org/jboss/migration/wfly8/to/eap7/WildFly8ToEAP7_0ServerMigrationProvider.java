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
package org.jboss.migration.wfly8.to.eap7;

import org.jboss.migration.eap.EAPServerMigrationProvider7_0;
import org.jboss.migration.wfly10.WildFlyServerMigration10;
import org.jboss.migration.wfly10.config.task.module.MigrateReferencedModules;
import org.jboss.migration.wfly10.config.task.paths.MigrateReferencedPaths;
import org.jboss.migration.wfly10.config.task.subsystem.jacorb.MigrateJacorbSubsystem;
import org.jboss.migration.wfly10.config.task.subsystem.jberet.AddBatchJBeretSubsystem;
import org.jboss.migration.wfly10.config.task.subsystem.messaging.MigrateMessagingSubsystem;
import org.jboss.migration.wfly10.config.task.subsystem.requestcontroller.AddRequestControllerSubsystem;
import org.jboss.migration.wfly10.config.task.subsystem.securitymanager.AddSecurityManagerSubsystem;
import org.jboss.migration.wfly10.config.task.subsystem.singleton.AddSingletonSubsystem;
import org.jboss.migration.wfly10.config.task.update.AddJmxSubsystemToHosts;
import org.jboss.migration.wfly10.config.task.update.AddPrivateInterface;
import org.jboss.migration.wfly10.config.task.update.MigrateCompatibleSecurityRealms;
import org.jboss.migration.wfly10.config.task.update.MigrateDeployments;
import org.jboss.migration.wfly10.config.task.update.RemoveAllUnsupportedSubsystems;
import org.jboss.migration.wfly10.config.task.update.RemovePermgenAttributesFromJVMConfigs;
import org.jboss.migration.wfly10.config.task.update.RemoveUnsecureInterface;
import org.jboss.migration.wfly10.config.task.update.ServerUpdate;
import org.jboss.migration.wfly10.config.task.update.UpdateUnsecureInterface;
import org.jboss.migration.wfly8.WildFlyServer8;

/**
 * Server migration, from WildFly 8 to EAP 7.0.
 * @author emmartins
 */
public class WildFly8ToEAP7_0ServerMigrationProvider implements EAPServerMigrationProvider7_0 {

    @Override
    public WildFlyServerMigration10 getServerMigration() {
        final ServerUpdate.Builders<WildFlyServer8> serverUpdateBuilders = new ServerUpdate.Builders<>();
        return serverUpdateBuilders.serverUpdateBuilder()
                .standaloneServer(serverUpdateBuilders.standaloneConfigurationBuilder()
                        .subtask(new RemoveAllUnsupportedSubsystems<>())
                        .subtask(new MigrateReferencedModules<>())
                        .subtask(new MigrateReferencedPaths<>())
                        .subtask(new MigrateJacorbSubsystem<>())
                        .subtask(new MigrateMessagingSubsystem<>())
                        .subtask(new WildFly8ToEAP7_0UpdateInfinispanSubsystem<>())
                        .subtask(new WildFly8ToEAP7_0UpdateUndertowSubsystem<>())
                        .subtask(new AddBatchJBeretSubsystem<>())
                        .subtask(new AddRequestControllerSubsystem<>())
                        .subtask(new AddSecurityManagerSubsystem<>())
                        .subtask(new AddSingletonSubsystem<>())
                        .subtask(new AddPrivateInterface<>())
                        .subtask(new MigrateCompatibleSecurityRealms<>())
                        .subtask(new MigrateDeployments<>()))
                .domain(serverUpdateBuilders.domainBuilder()
                        .domainConfigurations(serverUpdateBuilders.domainConfigurationBuilder()
                                .subtask(new RemoveAllUnsupportedSubsystems<>())
                                .subtask(new MigrateReferencedModules<>())
                                .subtask(new MigrateReferencedPaths<>())
                                .subtask(new MigrateJacorbSubsystem<>())
                                .subtask(new MigrateMessagingSubsystem<>())
                                .subtask(new WildFly8ToEAP7_0UpdateInfinispanSubsystem<>())
                                .subtask(new WildFly8ToEAP7_0UpdateUndertowSubsystem<>())
                                .subtask(new AddBatchJBeretSubsystem<>())
                                .subtask(new AddRequestControllerSubsystem<>())
                                .subtask(new AddSecurityManagerSubsystem<>())
                                .subtask(new AddSingletonSubsystem<>())
                                .subtask(new UpdateUnsecureInterface<>())
                                .subtask(new AddPrivateInterface<>())
                                .subtask(new RemovePermgenAttributesFromJVMConfigs<>())
                                .subtask(new MigrateDeployments<>()))
                        .hostConfigurations(serverUpdateBuilders.hostConfigurationBuilder()
                                .subtask(new MigrateReferencedModules<>())
                                .subtask(new MigrateReferencedPaths<>())
                                .subtask(serverUpdateBuilders.hostBuilder()
                                        .subtask(new AddJmxSubsystemToHosts<>())
                                        .subtask(new RemoveUnsecureInterface<>())
                                        .subtask(new RemovePermgenAttributesFromJVMConfigs<>())
                                        .subtask(new MigrateCompatibleSecurityRealms<>())
                                )
                        )
                )
                .build();
    }

    @Override
    public Class<WildFlyServer8> getSourceType() {
        return WildFlyServer8.class;
    }
}
