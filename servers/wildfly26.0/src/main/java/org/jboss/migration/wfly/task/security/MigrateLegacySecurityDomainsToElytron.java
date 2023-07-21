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

        public static final String SECURITY_DOMAIN = "security-domain";

        public static final String SECURITY_ENABLED = "security-enabled";
        public static final String APPLICATION_SECURITY_DOMAIN = "application-security-domain";
        public static final String DEFAULT_SECURITY_DOMAIN = "default-security-domain";
        public static final String REALM = "realm";
        public static final String SECURITY = "security";
        public static final String CLIENT = "client";
        public static final String IDENTITY = "identity";
        public static final String ELYTRON = "elytron";
        public static final String ELYTRON_DOMAIN = "elytron-domain";

        protected UpdateSubsystems(final LegacySecurityConfigurations legacySecurityConfigurations) {
            name(SUBTASK_NAME);
            skipPolicy(TaskSkipPolicy.skipIfDefaultTaskSkipPropertyIsSet());
            final ManageableResourceTaskRunnableBuilder<S, SubsystemResource> runnableBuilder = params -> context -> {
                ServerMigrationTaskResult taskResult = ServerMigrationTaskResult.SKIPPED;
                final SubsystemResource subsystemResource = params.getResource();
                final LegacySecurityConfiguration legacySecurityConfiguration = legacySecurityConfigurations.getSecurityConfigurations().get(subsystemResource.getServerConfiguration().getConfigurationPath().getPath().toString());
                if (legacySecurityConfiguration != null) {
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
                    subsystemResource.getServerConfiguration().executeManagementOperation(getUndefineAttributeOperation(iiopSubsystemResource.getResourcePathAddress(), SECURITY));
                    taskContext.getLogger().warnf("Migrated iiop-openjdk subsystem resource using legacy security domain to Elytron defaults, which is no security. Please note that further manual Elytron configuration should be needed!");
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
    }
}