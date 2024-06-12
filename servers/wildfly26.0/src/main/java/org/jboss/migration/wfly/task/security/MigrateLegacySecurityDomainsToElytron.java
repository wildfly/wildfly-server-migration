/*
 * Copyright 2022 Red Hat, Inc.
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

package org.jboss.migration.wfly.task.security;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.migration.core.jboss.JBossSubsystemNames;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.core.task.component.TaskSkipPolicy;
import org.jboss.migration.wfly10.config.management.SubsystemResource;
import org.jboss.migration.wfly10.config.task.management.configuration.ManageableServerConfigurationCompositeSubtasks;
import org.jboss.migration.wfly10.config.task.management.configuration.ManageableServerConfigurationCompositeTask;
import org.jboss.migration.wfly10.config.task.management.configuration.ManageableServerConfigurationLeafTask;
import org.jboss.migration.wfly10.config.task.management.resource.ManageableResourceTaskRunnableBuilder;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.operations.common.Util.createAddOperation;
import static org.jboss.as.controller.operations.common.Util.getUndefineAttributeOperation;
import static org.jboss.as.controller.operations.common.Util.getWriteAttributeOperation;

/**
 * @author emmartins
 */
public class MigrateLegacySecurityDomainsToElytron<S> extends ManageableServerConfigurationCompositeTask.Builder<S> {

    private static final String TASK_NAME = "security.migrate-legacy-security-domains-to-elytron";

    public MigrateLegacySecurityDomainsToElytron(LegacySecurityConfigurations legacySecurityConfigurations) {
        name(TASK_NAME);
        skipPolicy(TaskSkipPolicy.skipIfDefaultTaskSkipPropertyIsSet());
        beforeRun(context -> context.getLogger().debugf("Migrating legacy security domains to Elytron..."));
        subtasks(ManageableServerConfigurationCompositeSubtasks.of(new UpdateSubsystems<>(legacySecurityConfigurations)));
        afterRun(context -> context.getLogger().debugf("Legacy security domains migrated to Elytron."));
    }

    public static class UpdateSubsystems<S> extends ManageableServerConfigurationLeafTask.Builder<S> {

        private static final String SUBTASK_NAME = TASK_NAME + ".update-subsystems";

        public static final String SECURITY_ENABLED = "security-enabled";
        public static final String APPLICATION_SECURITY_DOMAIN = "application-security-domain";
        public static final String DEFAULT_SECURITY_DOMAIN = "default-security-domain";
        public static final String SECURITY = "security";
        public static final String CLIENT = "client";
        public static final String IDENTITY = "identity";
        public static final String ELYTRON = "elytron";
        public static final String ELYTRON_DOMAIN = "elytron-domain";
        public static final String DATA_SOURCE = "data-source";
        public static final String XA_DATA_SOURCE = "xa-data-source";
        public static final String SECURITY_DOMAIN = "security-domain";
        public static final String RESOURCE_ADAPTERS = "resource-adapters";
        public static final String RESOURCE_ADAPTER = "resource-adapter";
        public static final String CONNECTION_DEFINITIONS = "connection-definitions";
        public static final String CONNECTION_DEFINITION = "connection-definition";
        public static final String SECURITY_DOMAIN_AND_APPLICATION = "security-domain-and-application";
        public static final String ELYTRON_ENABLED = "elytron-enabled";
        public static final String RECOVERY_SECURITY_DOMAIN = "recovery-security-domain";
        public static final String RECOVERY_ELYTRON_ENABLED = "recovery-elytron-enabled";

        protected UpdateSubsystems(final LegacySecurityConfigurations legacySecurityConfigurations) {
            name(SUBTASK_NAME);
            skipPolicy(TaskSkipPolicy.skipIfDefaultTaskSkipPropertyIsSet());
            final ManageableResourceTaskRunnableBuilder<S, SubsystemResource> runnableBuilder = params -> context -> {
                ServerMigrationTaskResult taskResult = ServerMigrationTaskResult.SKIPPED;
                final SubsystemResource subsystemResource = params.getResource();
                final LegacySecurityConfiguration legacySecurityConfiguration = legacySecurityConfigurations.getSecurityConfigurations().get(subsystemResource.getServerConfiguration().getConfigurationPath().getPath().toString());
                if (legacySecurityConfiguration != null && legacySecurityConfiguration.requiresMigration()) {
                    if (migrateSubsystemEJB3(legacySecurityConfiguration, subsystemResource, context)) {
                        taskResult = ServerMigrationTaskResult.SUCCESS;
                    }
                    if (migrateSubsystemUndertow(legacySecurityConfiguration, subsystemResource, context)) {
                        taskResult = ServerMigrationTaskResult.SUCCESS;
                    }
                    if (migrateSubsystemMessaging(legacySecurityConfiguration, subsystemResource, context)) {
                        taskResult = ServerMigrationTaskResult.SUCCESS;
                    }
                    if (migrateSubsystemIIOP(legacySecurityConfiguration, subsystemResource, context)) {
                        taskResult = ServerMigrationTaskResult.SUCCESS;
                    }
                    if (migrateSubsystemDatasources(legacySecurityConfiguration, subsystemResource, context)) {
                        taskResult = ServerMigrationTaskResult.SUCCESS;
                    }
                    if (migrateSubsystemResourceAdapters(legacySecurityConfiguration, subsystemResource, context)) {
                        taskResult = ServerMigrationTaskResult.SUCCESS;
                    }
                }
                return taskResult;
            };
            runBuilder(SubsystemResource.class, JBossSubsystemNames.ELYTRON, runnableBuilder);
        }

        protected boolean migrateSubsystemIIOP(LegacySecurityConfiguration legacySecurityConfiguration, SubsystemResource subsystemResource, TaskContext taskContext) {
            taskContext.getLogger().debugf("Looking for iiop-openjdk subsystem using legacy security domains.");
            final SubsystemResource iiopSubsystemResource = subsystemResource.getParentResource().getSubsystemResource(JBossSubsystemNames.IIOP_OPENJDK);
            if (iiopSubsystemResource != null) {
                final String securityAttribute = iiopSubsystemResource.getResourceConfiguration().get(SECURITY).asStringOrNull();
                if (CLIENT.equals(securityAttribute) || IDENTITY.equals(securityAttribute)) {
                    subsystemResource.getServerConfiguration().executeManagementOperation(getWriteAttributeOperation(iiopSubsystemResource.getResourcePathAddress(), SECURITY, ELYTRON));
                    taskContext.getLogger().warnf("Migrated iiop-openjdk subsystem resource using legacy security domain to Elytron defaults. Please note that further manual Elytron configuration should be needed!");
                    return true;
                }
            }
            return false;
        }

        protected boolean migrateSubsystemEJB3(LegacySecurityConfiguration legacySecurityConfiguration, SubsystemResource subsystemResource, TaskContext taskContext) {
            taskContext.getLogger().debugf("Looking for ejb3 subsystem resources using a legacy security-domain...");
            final SubsystemResource ejbSubsystemResource = subsystemResource.getParentResource().getSubsystemResource(JBossSubsystemNames.EJB3);
            if (ejbSubsystemResource != null) {
                final ModelNode subsystemConfig = ejbSubsystemResource.getResourceConfiguration();
                final String defaultSecurityDomain = subsystemConfig.get(DEFAULT_SECURITY_DOMAIN).asStringOrNull();
                if (defaultSecurityDomain != null) {
                    if (!subsystemConfig.hasDefined(APPLICATION_SECURITY_DOMAIN, defaultSecurityDomain)) {
                        final PathAddress pathAddress = ejbSubsystemResource.getResourcePathAddress().append(APPLICATION_SECURITY_DOMAIN, defaultSecurityDomain);
                        final ModelNode op = createAddOperation(pathAddress);
                        op.get(SECURITY_DOMAIN).set(legacySecurityConfiguration.getDefaultElytronApplicationDomainName());
                        subsystemResource.getServerConfiguration().executeManagementOperation(op);
                        taskContext.getLogger().warnf("Migrated ejb3 subsystem resource %s using legacy security domain %s, to Elytron's default application Security Domain. Please note that further manual Elytron configuration may be needed if the legacy security domain being used was not the source server's default Application Domain configuration!", pathAddress.toPathStyleString(), defaultSecurityDomain);
                        return true;
                    }
                }
            }
            return false;
        }

        protected boolean migrateSubsystemUndertow(LegacySecurityConfiguration legacySecurityConfiguration, SubsystemResource subsystemResource, TaskContext taskContext) {
            taskContext.getLogger().debugf("Looking for undertow subsystem resources using a legacy security-domain...");
            final SubsystemResource undertowSubsystemResource = subsystemResource.getParentResource().getSubsystemResource(JBossSubsystemNames.UNDERTOW);
            if (undertowSubsystemResource != null) {
                final ModelNode subsystemConfig = undertowSubsystemResource.getResourceConfiguration();
                final String defaultSecurityDomain = subsystemConfig.get(DEFAULT_SECURITY_DOMAIN).asStringOrNull();
                if (defaultSecurityDomain != null) {
                    if (!subsystemConfig.hasDefined(APPLICATION_SECURITY_DOMAIN, defaultSecurityDomain)) {
                        final PathAddress pathAddress = undertowSubsystemResource.getResourcePathAddress().append(APPLICATION_SECURITY_DOMAIN, defaultSecurityDomain);
                        final ModelNode op = createAddOperation(pathAddress);
                        op.get(SECURITY_DOMAIN).set(legacySecurityConfiguration.getDefaultElytronApplicationDomainName());
                        subsystemResource.getServerConfiguration().executeManagementOperation(op);
                        taskContext.getLogger().warnf("Migrated undertow subsystem resource %s using legacy security domain %s, to Elytron's default application Security Domain. Please note that further manual Elytron configuration may be needed if the legacy security domain being used was not the source server's default Application Domain configuration!", pathAddress.toPathStyleString(), defaultSecurityDomain);
                        return true;
                    }
                }
            }
            return false;
        }

        protected boolean migrateSubsystemMessaging(LegacySecurityConfiguration legacySecurityConfiguration, SubsystemResource subsystemResource, TaskContext taskContext) {
            taskContext.getLogger().debugf("Looking for messaging-activemq subsystem resources using a legacy security-domain...");
            final Operations.CompositeOperationBuilder compositeOperationBuilder = Operations.CompositeOperationBuilder.create();
            boolean requiresUpdate = false;
            final SubsystemResource messagingSubsystemResource = subsystemResource.getParentResource().getSubsystemResource(JBossSubsystemNames.MESSAGING_ACTIVEMQ);
            if (messagingSubsystemResource != null) {
                final ModelNode subsystemConfig = messagingSubsystemResource.getResourceConfiguration();
                if (subsystemConfig.hasDefined(SERVER)) {
                    for (Property serverProperty : subsystemConfig.get(SERVER).asPropertyList()) {
                        final String serverName = serverProperty.getName();
                        final ModelNode serverConfig = serverProperty.getValue();
                        if (!serverConfig.hasDefined(SECURITY_ENABLED) || serverConfig.get(SECURITY_ENABLED).asBoolean()) {
                            if (!serverConfig.hasDefined(ELYTRON_DOMAIN)) {
                                final ModelNode serverSecurityDomainNode = serverConfig.get(SECURITY_DOMAIN);
                                final String messagingSubsystemSecurityDomain = serverSecurityDomainNode.isDefined() ? serverSecurityDomainNode.asString() : "other";
                                taskContext.getLogger().debugf("Found messaging-activemq subsystem server %s using the legacy security-domain %s.", serverName, messagingSubsystemSecurityDomain);
                                final PathAddress pathAddress = messagingSubsystemResource.getResourcePathAddress().append(SERVER, serverName);
                                compositeOperationBuilder.addStep(getUndefineAttributeOperation(pathAddress, SECURITY_DOMAIN));
                                compositeOperationBuilder.addStep(getWriteAttributeOperation(pathAddress, ELYTRON_DOMAIN, legacySecurityConfiguration.getDefaultElytronApplicationDomainName()));
                                taskContext.getLogger().warnf("Migrated messaging-activemq subsystem server resource %s, to Elytron's default application Security Domain. Please note that further manual Elytron configuration may be needed if the legacy security domain being used was not the source server's default Application Domain configuration!", pathAddress.toPathStyleString());
                                requiresUpdate = true;
                            }
                        }
                    }
                }
            }
            if (requiresUpdate) {
                subsystemResource.getServerConfiguration().executeManagementOperation(compositeOperationBuilder.build().getOperation());
            }
            return requiresUpdate;
        }

        protected boolean migrateSubsystemDatasources(LegacySecurityConfiguration legacySecurityConfiguration, SubsystemResource subsystemResource, TaskContext taskContext) {
            taskContext.getLogger().debugf("Looking for datasources subsystem resources using a legacy security-domain...");
            final Operations.CompositeOperationBuilder compositeOperationBuilder = Operations.CompositeOperationBuilder.create();
            boolean requiresUpdate = false;
            final SubsystemResource datasourcesSubsystemResource = subsystemResource.getParentResource().getSubsystemResource(JBossSubsystemNames.DATASOURCES);
            if (datasourcesSubsystemResource != null) {
                final ModelNode subsystemConfig = datasourcesSubsystemResource.getResourceConfiguration();
                if (subsystemConfig.hasDefined(DATA_SOURCE)) {
                    for (Property dataSourceProperty : subsystemConfig.get(DATA_SOURCE).asPropertyList()) {
                        final String dataSourceName = dataSourceProperty.getName();
                        final ModelNode dataSourceConfig = dataSourceProperty.getValue();
                        requiresUpdate |= migrateSecurityDomainInDatasource(datasourcesSubsystemResource.getResourcePathAddress().append(DATA_SOURCE, dataSourceName), dataSourceConfig, compositeOperationBuilder, taskContext);
                    }
                }
                if (subsystemConfig.hasDefined(XA_DATA_SOURCE)) {
                    for (Property xaDataSourceProperty : subsystemConfig.get(XA_DATA_SOURCE).asPropertyList()) {
                        final String xaDataSourceName = xaDataSourceProperty.getName();
                        final ModelNode xaDataSourceConfig = xaDataSourceProperty.getValue();
                        requiresUpdate |= migrateSecurityDomainInDatasource(datasourcesSubsystemResource.getResourcePathAddress().append(XA_DATA_SOURCE, xaDataSourceName), xaDataSourceConfig, compositeOperationBuilder, taskContext);
                    }
                }
            }
            if (requiresUpdate) {
                subsystemResource.getServerConfiguration().executeManagementOperation(compositeOperationBuilder.build().getOperation());
            }
            return requiresUpdate;
        }

        private boolean migrateSecurityDomainInDatasource(PathAddress datasourceAddress, ModelNode dataSourceConfig, Operations.CompositeOperationBuilder compositeOperationBuilder, TaskContext taskContext) {
            boolean requiresUpdate = false;
            if (dataSourceConfig.hasDefined(SECURITY_DOMAIN)) {
                final String securityDomain = dataSourceConfig.get(SECURITY_DOMAIN).asString();
                taskContext.getLogger().debugf("Found resource %s using the legacy security domain %s.", datasourceAddress.toPathStyleString(), securityDomain);
                compositeOperationBuilder.addStep(getUndefineAttributeOperation(datasourceAddress, SECURITY_DOMAIN));
                compositeOperationBuilder.addStep(getWriteAttributeOperation(datasourceAddress, ELYTRON_ENABLED, ModelNode.TRUE));
                taskContext.getLogger().warnf("Undefined legacy security-domain %s attribute of data source resource %s. Please note that further manual Elytron configuration is needed to define appropriate authentication context for it!", securityDomain, datasourceAddress.toPathStyleString());
                requiresUpdate = true;
            }
            if (dataSourceConfig.hasDefined(RECOVERY_SECURITY_DOMAIN)) {
                // this applies to xa-data-source only
                final String recoverySecurityDomain = dataSourceConfig.get(RECOVERY_SECURITY_DOMAIN).asString();
                taskContext.getLogger().debugf("Found resource %s using the legacy recovery security domain %s.", datasourceAddress.toPathStyleString(), recoverySecurityDomain);
                compositeOperationBuilder.addStep(getUndefineAttributeOperation(datasourceAddress, RECOVERY_SECURITY_DOMAIN));
                compositeOperationBuilder.addStep(getWriteAttributeOperation(datasourceAddress, RECOVERY_ELYTRON_ENABLED, ModelNode.TRUE));
                taskContext.getLogger().warnf("Undefined legacy recovery-security-domain %s attribute of data source resource %s. Please note that further manual Elytron configuration is needed to define appropriate authentication context for it!", recoverySecurityDomain, datasourceAddress.toPathStyleString());
                requiresUpdate = true;
            }
            return requiresUpdate;
        }

        protected boolean migrateSubsystemResourceAdapters(LegacySecurityConfiguration legacySecurityConfiguration, SubsystemResource subsystemResource, TaskContext taskContext) {
            taskContext.getLogger().debugf("Looking for resource-adapters subsystem resources using a legacy security-domain...");
            final Operations.CompositeOperationBuilder compositeOperationBuilder = Operations.CompositeOperationBuilder.create();
            boolean requiresUpdate = false;
            final SubsystemResource raSubsystemResource = subsystemResource.getParentResource().getSubsystemResource(JBossSubsystemNames.RESOURCE_ADAPTERS);
            if (raSubsystemResource != null) {
                final ModelNode subsystemConfig = raSubsystemResource.getResourceConfiguration();
                if (subsystemConfig.hasDefined(RESOURCE_ADAPTERS)) {
                    for (Property raProperty : subsystemConfig.get(RESOURCE_ADAPTERS).asPropertyList()) {
                        final String raName = raProperty.getName();
                        final ModelNode raConfig = raProperty.getValue();
                        final PathAddress raAddress = raSubsystemResource.getResourcePathAddress().append(RESOURCE_ADAPTER, raName);
                        requiresUpdate |= migrateSecurityDomainInConnectionDefinition(raAddress, raConfig, compositeOperationBuilder, taskContext);
                    }
                }
            }
            if (requiresUpdate) {
                subsystemResource.getServerConfiguration().executeManagementOperation(compositeOperationBuilder.build().getOperation());
            }
            return requiresUpdate;
        }

        private boolean migrateSecurityDomainInConnectionDefinition(PathAddress parentResourceAddress, ModelNode parentResourceConfig, Operations.CompositeOperationBuilder compositeOperationBuilder, TaskContext taskContext) {
            boolean requiresUpdate = false;
            if (parentResourceConfig.hasDefined(CONNECTION_DEFINITIONS)) {
                for (Property connectionDefinitionProperty : parentResourceConfig.get(CONNECTION_DEFINITIONS).asPropertyList()) {
                    final String connectionDefinitionName = connectionDefinitionProperty.getName();
                    final ModelNode connectionDefinitionConfig = connectionDefinitionProperty.getValue();
                    final PathAddress connectionDefinitionAddress = PathAddress.pathAddress(parentResourceAddress).append(CONNECTION_DEFINITION, connectionDefinitionName);
                    if (connectionDefinitionConfig.hasDefined(SECURITY_DOMAIN)) {
                        final String securityDomain = connectionDefinitionConfig.get(SECURITY_DOMAIN).asString();
                        taskContext.getLogger().debugf("Found resource-adapter resource %s using the legacy security domain %s.", connectionDefinitionAddress.toPathStyleString(), securityDomain);
                        compositeOperationBuilder.addStep(getUndefineAttributeOperation(connectionDefinitionAddress, SECURITY_DOMAIN));
                        compositeOperationBuilder.addStep(getWriteAttributeOperation(connectionDefinitionAddress, ELYTRON_ENABLED, ModelNode.TRUE));
                        taskContext.getLogger().warnf("Undefined legacy security-domain %s attribute of resource-adapter resource %s. Please note that further manual Elytron configuration is needed to define appropriate authentication context for it!", securityDomain, connectionDefinitionAddress.toPathStyleString());
                        requiresUpdate = true;
                    }
                    if (connectionDefinitionConfig.hasDefined(SECURITY_DOMAIN_AND_APPLICATION)) {
                        final String securityDomain = connectionDefinitionConfig.get(SECURITY_DOMAIN_AND_APPLICATION).asString();
                        taskContext.getLogger().debugf("Found resource-adapter resource %s using the legacy security-domain-and-application %s.", connectionDefinitionAddress.toPathStyleString(), securityDomain);
                        compositeOperationBuilder.addStep(getUndefineAttributeOperation(connectionDefinitionAddress, SECURITY_DOMAIN_AND_APPLICATION));
                        compositeOperationBuilder.addStep(getWriteAttributeOperation(connectionDefinitionAddress, ELYTRON_ENABLED, ModelNode.TRUE));
                        taskContext.getLogger().warnf("Undefined legacy security-domain-and-application %s attribute of resource-adapter resource %s. Please note that further manual Elytron configuration is needed to define appropriate authentication-context-and-application for it!", securityDomain, connectionDefinitionAddress.toPathStyleString());
                        requiresUpdate = true;
                    }
                    if (connectionDefinitionConfig.hasDefined(RECOVERY_SECURITY_DOMAIN)) {
                        final String securityDomain = connectionDefinitionConfig.get(RECOVERY_SECURITY_DOMAIN).asString();
                        taskContext.getLogger().debugf("Found resource-adapter resource %s using the legacy recovery security domain %s.", connectionDefinitionAddress.toPathStyleString(), securityDomain);
                        compositeOperationBuilder.addStep(getUndefineAttributeOperation(connectionDefinitionAddress, RECOVERY_SECURITY_DOMAIN));
                        compositeOperationBuilder.addStep(getWriteAttributeOperation(connectionDefinitionAddress, ELYTRON_ENABLED, ModelNode.TRUE));
                        taskContext.getLogger().warnf("Undefined legacy recovery security domain %s attribute of resource-adapter resource %s. Please note that further manual Elytron configuration is needed to define appropriate authentication context for it!", securityDomain, connectionDefinitionAddress.toPathStyleString());
                        requiresUpdate = true;
                    }
                }
            }
            return requiresUpdate;
        }
    }
}