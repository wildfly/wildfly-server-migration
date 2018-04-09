/*
 * Copyright 2018 Red Hat, Inc.
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
package org.jboss.migration.eap6.to.eap7;

import org.jboss.migration.eap.EAPServer6_4;
import org.jboss.migration.eap.EAPServerMigrationProvider7_2;
import org.jboss.migration.eap6.to.eap7.tasks.EAP6_4ToEAP7_2UpdateJGroupsSubsystem;
import org.jboss.migration.wfly10.config.task.update.RemoveUnsupportedExtensions;
import org.jboss.migration.wfly10.config.task.update.RemoveUnsupportedSubsystems;
import org.jboss.migration.wfly11.task.subsystem.coremanagement.AddCoreManagementSubsystem;
import org.jboss.migration.eap.task.hostexclusion.AddHostExcludesResource;
import org.jboss.migration.eap.task.subsystem.elytron.AddElytronSubsystem;
import org.jboss.migration.eap.task.subsystem.web.EAP7_1MigrateWebSubsystem;
import org.jboss.migration.wfly11.task.subsystem.logging.RemoveConsoleHandlerFromLoggingSubsystem;
import org.jboss.migration.eap6.to.eap7.tasks.AddSocketBindingPortExpressions;
import org.jboss.migration.eap6.to.eap7.tasks.EAP6_4ToEAP7_2UpdateEESubsystem;
import org.jboss.migration.eap6.to.eap7.tasks.EAP6_4ToEAP7_2UpdateEJB3Subsystem;
import org.jboss.migration.eap6.to.eap7.tasks.EAP6_4ToEAP7_2UpdateInfinispanSubsystem;
import org.jboss.migration.eap6.to.eap7.tasks.EAP6_4ToEAP7_2UpdateMessagingActiveMQSubsystem;
import org.jboss.migration.eap6.to.eap7.tasks.EAP6_4ToEAP7_2UpdateRemotingSubsystem;
import org.jboss.migration.eap6.to.eap7.tasks.EAP6_4ToEAP7_2UpdateUndertowSubsystem;
import org.jboss.migration.eap6.to.eap7.tasks.SetupHttpUpgradeManagement;
import org.jboss.migration.wfly10.WildFlyServerMigration10;
import org.jboss.migration.wfly10.config.task.module.MigrateReferencedModules;
import org.jboss.migration.wfly10.config.task.paths.MigrateReferencedPaths;
import org.jboss.migration.wfly10.config.task.subsystem.jacorb.MigrateJacorbSubsystem;
import org.jboss.migration.wfly10.config.task.subsystem.jberet.AddBatchJBeretSubsystem;
import org.jboss.migration.wfly10.config.task.subsystem.messaging.MigrateMessagingSubsystem;
import org.jboss.migration.wfly10.config.task.subsystem.requestcontroller.AddRequestControllerSubsystem;
import org.jboss.migration.wfly10.config.task.subsystem.securitymanager.AddSecurityManagerSubsystem;
import org.jboss.migration.wfly10.config.task.subsystem.singleton.AddSingletonSubsystem;
import org.jboss.migration.wfly10.config.task.update.AddApplicationRealmSSLServerIdentity;
import org.jboss.migration.wfly10.config.task.update.AddJmxSubsystemToHosts;
import org.jboss.migration.wfly10.config.task.update.AddPrivateInterface;
import org.jboss.migration.wfly10.config.task.update.AddSocketBindingMulticastAddressExpressions;
import org.jboss.migration.wfly10.config.task.update.MigrateCompatibleSecurityRealms;
import org.jboss.migration.wfly10.config.task.update.MigrateDeployments;
import org.jboss.migration.wfly10.config.task.update.RemovePermgenAttributesFromJVMConfigs;
import org.jboss.migration.wfly10.config.task.update.RemoveUnsecureInterface;
import org.jboss.migration.wfly10.config.task.update.ServerUpdate;
import org.jboss.migration.wfly10.config.task.update.UpdateUnsecureInterface;
import org.jboss.migration.wfly10.config.task.update.AddLoadBalancerProfile;

/**
 * Server migration, from EAP 6.4 to EAP 7.2.
 * @author emmartins
 */
public class EAP6_4ToEAP7_2ServerMigrationProvider implements EAPServerMigrationProvider7_2 {
    @Override
    public WildFlyServerMigration10 getServerMigration() {
        final ServerUpdate.Builders<EAPServer6_4> serverUpdateBuilders = new ServerUpdate.Builders<>();
        return serverUpdateBuilders.serverUpdateBuilder()
                .standaloneServer(serverUpdateBuilders.standaloneConfigurationBuilder()
                        .subtask(new RemoveUnsupportedExtensions<>())
                        .subtask(new RemoveUnsupportedSubsystems<>())
                        .subtask(new MigrateReferencedModules<>())
                        .subtask(new MigrateReferencedPaths<>())
                        .subtask(new MigrateJacorbSubsystem<>())
                        .subtask(new AddElytronSubsystem<>())
                        .subtask(new EAP7_1MigrateWebSubsystem<>())
                        .subtask(new EAP6_4ToEAP7_2UpdateUndertowSubsystem<>())
                        .subtask(new MigrateMessagingSubsystem<>())
                        .subtask(new EAP6_4ToEAP7_2UpdateMessagingActiveMQSubsystem<>())
                        .subtask(new EAP6_4ToEAP7_2UpdateInfinispanSubsystem<>())
                        .subtask(new EAP6_4ToEAP7_2UpdateEESubsystem<>())
                        .subtask(new EAP6_4ToEAP7_2UpdateEJB3Subsystem<>())
                        .subtask(new EAP6_4ToEAP7_2UpdateJGroupsSubsystem<>())
                        .subtask(new EAP6_4ToEAP7_2UpdateRemotingSubsystem<>())
                        .subtask(new AddBatchJBeretSubsystem<>())
                        .subtask(new AddCoreManagementSubsystem<>())
                        .subtask(new AddHostExcludesResource<>())
                        .subtask(new AddRequestControllerSubsystem<>())
                        .subtask(new AddSecurityManagerSubsystem<>())
                        .subtask(new AddSingletonSubsystem<>())
                        .subtask(new SetupHttpUpgradeManagement<>())
                        .subtask(new AddPrivateInterface<>())
                        .subtask(new AddSocketBindingPortExpressions<>())
                        .subtask(new AddSocketBindingMulticastAddressExpressions<>())
                        .subtask(new MigrateCompatibleSecurityRealms<>())
                        .subtask(new AddApplicationRealmSSLServerIdentity<>())
                        .subtask(new MigrateDeployments<>()))
                .domain(serverUpdateBuilders.domainBuilder()
                        .domainConfigurations(serverUpdateBuilders.domainConfigurationBuilder()
                                .subtask(new RemoveUnsupportedExtensions<>())
                                .subtask(new RemoveUnsupportedSubsystems<>())
                                .subtask(new MigrateReferencedModules<>())
                                .subtask(new MigrateReferencedPaths<>())
                                .subtask(new MigrateJacorbSubsystem<>())
                                .subtask(new AddElytronSubsystem<>())
                                .subtask(new EAP7_1MigrateWebSubsystem<>())
                                .subtask(new EAP6_4ToEAP7_2UpdateUndertowSubsystem<>())
                                .subtask(new MigrateMessagingSubsystem<>())
                                .subtask(new EAP6_4ToEAP7_2UpdateMessagingActiveMQSubsystem<>())
                                .subtask(new EAP6_4ToEAP7_2UpdateInfinispanSubsystem<>())
                                .subtask(new EAP6_4ToEAP7_2UpdateEESubsystem<>())
                                .subtask(new EAP6_4ToEAP7_2UpdateEJB3Subsystem<>())
                                .subtask(new EAP6_4ToEAP7_2UpdateJGroupsSubsystem<>())
                                .subtask(new EAP6_4ToEAP7_2UpdateRemotingSubsystem<>())
                                .subtask(new AddBatchJBeretSubsystem<>())
                                .subtask(new AddCoreManagementSubsystem<>())
                                .subtask(new AddRequestControllerSubsystem<>())
                                .subtask(new AddSecurityManagerSubsystem<>())
                                .subtask(new AddSingletonSubsystem<>())
                                .subtask(new UpdateUnsecureInterface<>())
                                .subtask(new AddPrivateInterface<>())
                                .subtask(new AddSocketBindingPortExpressions<>())
                                .subtask(new AddSocketBindingMulticastAddressExpressions<>())
                                .subtask(new AddLoadBalancerProfile<>())
                                .subtask(new RemoveConsoleHandlerFromLoggingSubsystem<>())
                                .subtask(new RemovePermgenAttributesFromJVMConfigs<>())
                                .subtask(new MigrateDeployments<>()))
                        .hostConfigurations(serverUpdateBuilders.hostConfigurationBuilder()
                                .subtask(new MigrateReferencedModules<>())
                                .subtask(new MigrateReferencedPaths<>())
                                .subtask(serverUpdateBuilders.hostBuilder()
                                        .subtask(new AddCoreManagementSubsystem<>())
                                        .subtask(new AddElytronSubsystem<>())
                                        .subtask(new AddJmxSubsystemToHosts<>())
                                        .subtask(new RemoveUnsecureInterface<>())
                                        .subtask(new SetupHttpUpgradeManagement<>())
                                        .subtask(new RemovePermgenAttributesFromJVMConfigs<>())
                                        .subtask(new MigrateCompatibleSecurityRealms<>())
                                        .subtask(new AddApplicationRealmSSLServerIdentity<>()))))
                .build();
    }

    @Override
    public Class<EAPServer6_4> getSourceType() {
        return EAPServer6_4.class;
    }
}
