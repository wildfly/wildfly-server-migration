/*
 * Copyright 2023 Red Hat, Inc.
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
import org.jboss.migration.wfly10.config.management.ManageableServerConfiguration;
import org.jboss.migration.wfly10.config.management.SubsystemResource;
import org.jboss.migration.wfly10.config.task.management.configuration.ManageableServerConfigurationCompositeSubtasks;
import org.jboss.migration.wfly10.config.task.management.configuration.ManageableServerConfigurationCompositeTask;
import org.jboss.migration.wfly10.config.task.management.configuration.ManageableServerConfigurationLeafTask;
import org.jboss.migration.wfly10.config.task.management.resource.ManageableResourceTaskRunnableBuilder;

import java.util.Map;
import java.util.stream.Collectors;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SASL_AUTHENTICATION_FACTORY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.operations.common.Util.getUndefineAttributeOperation;
import static org.jboss.as.controller.operations.common.Util.getWriteAttributeOperation;

/**
 * @author istudens
 */
public class MigrateDeprecatedSecurityRealmsToSaslAuthenticationFactory<S> extends ManageableServerConfigurationCompositeTask.Builder<S> {

    private static final String TASK_NAME = "security.migrate-deprecated-security-realms-to-sasl-authentication-factory";

    public MigrateDeprecatedSecurityRealmsToSaslAuthenticationFactory(LegacySecurityConfigurations legacySecurityConfigurations) {
        name(TASK_NAME);
        skipPolicy(TaskSkipPolicy.skipIfDefaultTaskSkipPropertyIsSet());
        beforeRun(context -> context.getLogger().debugf("Migrating deprecated security realm to SASL authentication factory..."));
        subtasks(ManageableServerConfigurationCompositeSubtasks.of(new MigrateSecurityRealmsToSaslAuthenticationFactory<>(legacySecurityConfigurations)));
        afterRun(context -> context.getLogger().debugf("Deprecated security realm migrated to SASL authentication factory."));
    }

    public static class MigrateSecurityRealmsToSaslAuthenticationFactory<S> extends ManageableServerConfigurationLeafTask.Builder<S> {

        private static final String SUBTASK_NAME = TASK_NAME + ".update-subsystems";
        private static final String SETTING = "setting";
        private static final String HTTP_INVOKER = "http-invoker";
        private static final String HTTPS_LISTENER = "https-listener";
        private static final String HTTP_CONNECTOR = "http-connector";

        protected MigrateSecurityRealmsToSaslAuthenticationFactory(final LegacySecurityConfigurations legacySecurityConfigurations) {
            name(SUBTASK_NAME);
            skipPolicy(TaskSkipPolicy.skipIfDefaultTaskSkipPropertyIsSet());
            final ManageableResourceTaskRunnableBuilder<S, SubsystemResource> runnableBuilder = params -> context -> {
                final SubsystemResource subsystemResource = params.getResource();
                final LegacySecurityConfiguration legacySecurityConfiguration = legacySecurityConfigurations.getSecurityConfigurations().get(subsystemResource.getServerConfiguration().getConfigurationPath().getPath().toString());
                if (legacySecurityConfiguration == null) {
                    return ServerMigrationTaskResult.SKIPPED;
                }
                // evaluate security-realms, check if there is a connection between security-realm to security-domain and then to sasl auth factory
                Map<String, String> realmsToSaslAuthFactories = legacySecurityConfiguration.getElytronSaslAuthenticationFactoryNames().entrySet()
                        .stream()
                        .filter(saslAuthFactoryToSecDomain -> legacySecurityConfiguration.getElytronSecurityDomainNames().containsKey(saslAuthFactoryToSecDomain.getValue()))
                        .collect(Collectors.toMap(e -> legacySecurityConfiguration.getElytronSecurityDomainNames().get(e.getValue()), e -> e.getKey()));

                if (realmsToSaslAuthFactories.isEmpty()) {
                    return ServerMigrationTaskResult.SKIPPED;
                }

                // look for deprecated security realms in Undertow subsystem configuration
                boolean foundAndMigrated = false;
                foundAndMigrated |= migrateDeprecatedSecurityRealmsInUndertow(realmsToSaslAuthFactories, subsystemResource, context);
                foundAndMigrated |= migrateDeprecatedSecurityRealmsInRemoting(realmsToSaslAuthFactories, subsystemResource, context);
                return (foundAndMigrated) ? ServerMigrationTaskResult.SUCCESS : ServerMigrationTaskResult.SKIPPED;
            };
            runBuilder(SubsystemResource.class, JBossSubsystemNames.ELYTRON, runnableBuilder);
        }

        protected boolean migrateDeprecatedSecurityRealmsInUndertow(Map<String, String> realmsToSaslAuthFactories, SubsystemResource subsystemResource, TaskContext taskContext) {
            final ManageableServerConfiguration configuration = subsystemResource.getServerConfiguration();
            final Operations.CompositeOperationBuilder compositeOperationBuilder = Operations.CompositeOperationBuilder.create();

            if (! addUndertowOperationSteps(realmsToSaslAuthFactories, subsystemResource, compositeOperationBuilder, taskContext)) {
                return false;
            }

            final ModelNode migrateOp = compositeOperationBuilder.build().getOperation();
            taskContext.getLogger().debugf("Deprecated security realms migration to SASL authentication factory in Undertow: %s", migrateOp);
            configuration.executeManagementOperation(migrateOp);
            taskContext.getLogger().infof("Deprecated security realms migrated to SASL authentication factory in Undertow.");

            return true;
        }

        protected boolean addUndertowOperationSteps(Map<String, String> realmsToSaslAuthFactories, SubsystemResource subsystemResource, Operations.CompositeOperationBuilder compositeOperationBuilder, TaskContext taskContext) {
            boolean found = false;
            final SubsystemResource undertowSubsystemResource = subsystemResource.getParentResource().getSubsystemResource(JBossSubsystemNames.UNDERTOW);
            if (undertowSubsystemResource != null) {
                for (Property server : undertowSubsystemResource.getResourceConfiguration().get(SERVER).asPropertyList()) {
                    taskContext.getLogger().debugf("Looking for Undertow subsystem with security configuration referring to deprecated security-realm attribute in https-listener.");
                    for (Property httpsListener : server.getValue().get(HTTPS_LISTENER).asPropertyList()) {
                        final String securityRealm = httpsListener.getValue().get(SECURITY_REALM).asStringOrNull();
                        if (securityRealm != null) {
                            taskContext.getLogger().debugf("Found deprecated security realm: %s", securityRealm);
                            final String saslAuthenticationFactory = realmsToSaslAuthFactories.get(securityRealm);
                            if (saslAuthenticationFactory != null) {
                                PathAddress httpsListenerPath = undertowSubsystemResource.getResourcePathAddress().append(SERVER, server.getName()).append(HTTPS_LISTENER, httpsListener.getName());
                                compositeOperationBuilder.addStep(getUndefineAttributeOperation(httpsListenerPath, SECURITY_REALM));
                                compositeOperationBuilder.addStep(getWriteAttributeOperation(httpsListenerPath, SASL_AUTHENTICATION_FACTORY, saslAuthenticationFactory));
                                found = true;
                            } else {
                                taskContext.getLogger().warnf("Unknown deprecated security realm %s configured on https-listener in Undertow subsystem.", securityRealm);
                            }
                        }
                    }
                    taskContext.getLogger().debugf("Looking for Undertow subsystem with security configuration referring to deprecated security-realm attribute in http-invoker.");
                    for (Property host : server.getValue().get(HOST).asPropertyList()) {
                        if (host.getValue().hasDefined(SETTING, HTTP_INVOKER)) {
                            final String securityRealm = host.getValue().get(SETTING).get(HTTP_INVOKER).get(SECURITY_REALM).asStringOrNull();
                            if (securityRealm != null) {
                                taskContext.getLogger().debugf("Found deprecated security realm: %s", securityRealm);
                                final String saslAuthenticationFactory = realmsToSaslAuthFactories.get(securityRealm);
                                if (saslAuthenticationFactory != null) {
                                    PathAddress httpInvokerPath = undertowSubsystemResource.getResourcePathAddress().append(SERVER, server.getName()).append(HOST, host.getName()).append(SETTING, HTTP_INVOKER);
                                    compositeOperationBuilder.addStep(getUndefineAttributeOperation(httpInvokerPath, SECURITY_REALM));
                                    compositeOperationBuilder.addStep(getWriteAttributeOperation(httpInvokerPath, SASL_AUTHENTICATION_FACTORY, saslAuthenticationFactory));
                                    found = true;
                                } else {
                                    taskContext.getLogger().warnf("Unknown deprecated security realm %s configured on http-invoker in Undertow subsystem.", securityRealm);
                                }
                            }
                        }
                    }
                }
            }
            return found;
        }

        protected boolean migrateDeprecatedSecurityRealmsInRemoting(Map<String, String> realmsToSaslAuthFactories, SubsystemResource subsystemResource, TaskContext taskContext) {
            final ManageableServerConfiguration configuration = subsystemResource.getServerConfiguration();
            final Operations.CompositeOperationBuilder compositeOperationBuilder = Operations.CompositeOperationBuilder.create();

            if (! addRemotingOperationSteps(realmsToSaslAuthFactories, subsystemResource, compositeOperationBuilder, taskContext)) {
                return false;
            }

            final ModelNode migrateOp = compositeOperationBuilder.build().getOperation();
            taskContext.getLogger().debugf("Deprecated security realms migration to SASL authentication factory in Remoting: %s", migrateOp);
            configuration.executeManagementOperation(migrateOp);
            taskContext.getLogger().infof("Deprecated security realms migrated to SASL authentication factory in Remoting.");

            return true;
        }

        protected boolean addRemotingOperationSteps(Map<String, String> realmsToSaslAuthFactories, SubsystemResource subsystemResource, Operations.CompositeOperationBuilder compositeOperationBuilder, TaskContext taskContext) {
            boolean found = false;
            final SubsystemResource remotingSubsystemResource = subsystemResource.getParentResource().getSubsystemResource(JBossSubsystemNames.REMOTING);
            if (remotingSubsystemResource != null) {
                taskContext.getLogger().debugf("Looking for Remoting subsystem with security configuration referring to deprecated security-realm attribute in http-connector.");
                for (Property httpConnector : remotingSubsystemResource.getResourceConfiguration().get(HTTP_CONNECTOR).asPropertyList()) {
                    final String securityRealm = httpConnector.getValue().get(SECURITY_REALM).asStringOrNull();
                    if (securityRealm != null) {
                        taskContext.getLogger().debugf("Found deprecated security realm: %s", securityRealm);
                        final String saslAuthenticationFactory = realmsToSaslAuthFactories.get(securityRealm);
                        if (saslAuthenticationFactory != null) {
                            PathAddress httpConnectorPath = remotingSubsystemResource.getResourcePathAddress().append(HTTP_CONNECTOR, httpConnector.getName());
                            compositeOperationBuilder.addStep(getUndefineAttributeOperation(httpConnectorPath, SECURITY_REALM));
                            compositeOperationBuilder.addStep(getWriteAttributeOperation(httpConnectorPath, SASL_AUTHENTICATION_FACTORY, saslAuthenticationFactory));
                            found = true;
                        } else {
                            taskContext.getLogger().warnf("Unknown deprecated security realm %s configured on http-connector in Remoting subsystem.", securityRealm);
                        }
                    }
                }
            }
            return found;
        }
    }
}