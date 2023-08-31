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

package org.jboss.migration.wfly.task.security;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;
import org.jboss.migration.core.jboss.JBossSubsystemNames;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.core.task.component.TaskSkipPolicy;
import org.jboss.migration.wfly10.config.management.HostConfiguration;
import org.jboss.migration.wfly10.config.management.ManageableServerConfiguration;
import org.jboss.migration.wfly10.config.management.ManagementInterfaceResource;
import org.jboss.migration.wfly10.config.management.StandaloneServerConfiguration;
import org.jboss.migration.wfly10.config.management.SubsystemResource;
import org.jboss.migration.wfly10.config.task.management.configuration.ManageableServerConfigurationCompositeSubtasks;
import org.jboss.migration.wfly10.config.task.management.configuration.ManageableServerConfigurationCompositeTask;
import org.jboss.migration.wfly10.config.task.management.configuration.ManageableServerConfigurationLeafTask;
import org.jboss.migration.wfly10.config.task.management.resource.ManageableResourceTaskRunnableBuilder;
import org.jboss.migration.wfly11.task.subsystem.elytron.HttpAuthenticationFactoryAddOperation;
import org.jboss.migration.wfly11.task.subsystem.elytron.KeyManagerAddOperation;
import org.jboss.migration.wfly11.task.subsystem.elytron.KeystoreAddOperation;
import org.jboss.migration.wfly11.task.subsystem.elytron.MechanismConfiguration;
import org.jboss.migration.wfly11.task.subsystem.elytron.MechanismRealmConfiguration;
import org.jboss.migration.wfly11.task.subsystem.elytron.PropertiesRealmAddOperation;
import org.jboss.migration.wfly11.task.subsystem.elytron.SaslAuthenticationFactoryAddOperation;
import org.jboss.migration.wfly11.task.subsystem.elytron.SecurityDomainAddOperation;
import org.jboss.migration.wfly11.task.subsystem.elytron.ServerSSLContextAddOperation;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_CONTROLLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FILTER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HANDLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HTTP_AUTHENTICATION_FACTORY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HTTP_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HTTP_UPGRADE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NATIVE_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOTE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SASL_AUTHENTICATION_FACTORY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SSL_CONTEXT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.operations.common.Util.getUndefineAttributeOperation;
import static org.jboss.as.controller.operations.common.Util.getWriteAttributeOperation;

/**
 * @author emmartins
 */
public class MigrateLegacySecurityRealmsToElytron<S> extends ManageableServerConfigurationCompositeTask.Builder<S> {

    private static final String TASK_NAME = "security.migrate-legacy-security-realms-to-elytron";

    public MigrateLegacySecurityRealmsToElytron(LegacySecurityConfigurations legacySecurityConfigurations) {
        name(TASK_NAME);
        skipPolicy(TaskSkipPolicy.skipIfDefaultTaskSkipPropertyIsSet());
        beforeRun(context -> context.getLogger().debugf("Migrating legacy security realms to Elytron..."));
        subtasks(ManageableServerConfigurationCompositeSubtasks.of(new MigrateToElytron<>(legacySecurityConfigurations), new UpdateManagementInterfaces<>(legacySecurityConfigurations)));
        afterRun(context -> context.getLogger().debugf("Legacy security realms migrated to Elytron."));
    }

    public static class MigrateToElytron<S> extends ManageableServerConfigurationLeafTask.Builder<S> {

        private static final String SUBTASK_NAME = TASK_NAME + ".update-subsystems";
        private static final String SETTING = "setting";
        private static final String CONNECTOR = "connector";
        private static final String HTTP_INVOKER = "http-invoker";
        private static final String HTTPS_LISTENER = "https-listener";
        private static final String HTTP_CONNECTOR = "http-connector";
        private static final String SERVER_SSL_CONTEXT = "server-ssl-context";
        private static final String SERVER_REQUIRES_SSL = "server-requires-ssl";
        private static final String SSL_SOCKET_BINDING = "ssl-socket-binding";
        private static final String REMOTE_OUTBOUND_CONNECTION = "remote-outbound-connection";
        private static final String CONFIGURATION = "configuration";
        private static final String MOD_CLUSTER = "mod-cluster";
        private static final String REVERSE_PROXY = "reverse-proxy";

        protected MigrateToElytron(final LegacySecurityConfigurations legacySecurityConfigurations) {
            name(SUBTASK_NAME);
            skipPolicy(TaskSkipPolicy.skipIfDefaultTaskSkipPropertyIsSet());
            final ManageableResourceTaskRunnableBuilder<S, SubsystemResource> runnableBuilder = params -> context -> {
                final SubsystemResource subsystemResource = params.getResource();
                final LegacySecurityConfiguration legacySecurityConfiguration = legacySecurityConfigurations.getSecurityConfigurations().get(subsystemResource.getServerConfiguration().getConfigurationPath().getPath().toString());
                if (legacySecurityConfiguration == null) {
                    return ServerMigrationTaskResult.SKIPPED;
                }
                migrateSecurityRealms(legacySecurityConfiguration, subsystemResource, context);
                for (LegacySecurityRealm securityRealm : legacySecurityConfiguration.getLegacySecurityRealms().values()) {
                    //migrateSecurityRealm(securityRealm, legacySecurityConfiguration, subsystemResource, context);
                    // TODO warn about security realms migration to defaults?
                }
                return ServerMigrationTaskResult.SUCCESS;
            };
            runBuilder(SubsystemResource.class, JBossSubsystemNames.ELYTRON, runnableBuilder);
        }

        protected void migrateSecurityRealms(LegacySecurityConfiguration legacySecurityConfiguration, SubsystemResource subsystemResource, TaskContext taskContext) {
            final ManageableServerConfiguration configuration = subsystemResource.getServerConfiguration();
            final Operations.CompositeOperationBuilder compositeOperationBuilder = Operations.CompositeOperationBuilder.create();
            addOperationSteps(legacySecurityConfiguration, subsystemResource, compositeOperationBuilder, taskContext);
            final ModelNode migrateOp = compositeOperationBuilder.build().getOperation();
            taskContext.getLogger().debugf("Legacy security realms migration to Elytron's management op: %s", migrateOp);
            configuration.executeManagementOperation(migrateOp);
            taskContext.getLogger().infof("Legacy security realms migrated to Elytron.");
        }

        protected void addOperationSteps(LegacySecurityConfiguration legacySecurityConfiguration, SubsystemResource subsystemResource, Operations.CompositeOperationBuilder compositeOperationBuilder, TaskContext taskContext) {
            addDefaultApplicationRealm(legacySecurityConfiguration, subsystemResource, compositeOperationBuilder, taskContext);
            addDefaultManagementRealm(legacySecurityConfiguration, subsystemResource, compositeOperationBuilder, taskContext);
            addDefaultApplicationDomain(legacySecurityConfiguration, subsystemResource, compositeOperationBuilder, taskContext);
            addDefaultManagementDomain(legacySecurityConfiguration, subsystemResource, compositeOperationBuilder, taskContext);
            addDefaultApplicationHttpAuthenticationFactory(legacySecurityConfiguration, subsystemResource, compositeOperationBuilder, taskContext);
            addDefaultManagementHttpAuthenticationFactory(legacySecurityConfiguration, subsystemResource, compositeOperationBuilder, taskContext);
            addDefaultApplicationSaslAuthenticationFactory(legacySecurityConfiguration, subsystemResource, compositeOperationBuilder, taskContext);
            addDefaultManagementSaslAuthenticationFactory(legacySecurityConfiguration, subsystemResource, compositeOperationBuilder, taskContext);
            addDefaultTLS(legacySecurityConfiguration, subsystemResource, compositeOperationBuilder, taskContext);
            migrateRemotingSubsystem(legacySecurityConfiguration, subsystemResource, compositeOperationBuilder, taskContext);
            migrateUndertowSubsystem(legacySecurityConfiguration, subsystemResource, compositeOperationBuilder, taskContext);
            migrateIiopSubsystem(legacySecurityConfiguration, subsystemResource, compositeOperationBuilder, taskContext);
        }

        protected void addDefaultApplicationRealm(LegacySecurityConfiguration legacySecurityConfiguration, SubsystemResource subsystemResource, Operations.CompositeOperationBuilder compositeOperationBuilder, TaskContext taskContext) {
            final String securityRealmName = legacySecurityConfiguration.getDefaultElytronApplicationRealmName();
            final PropertiesRealmAddOperation propertiesRealmAddOperation = new PropertiesRealmAddOperation(subsystemResource.getResourcePathAddress(), securityRealmName);
            propertiesRealmAddOperation.usersProperties(new PropertiesRealmAddOperation.Properties("application-users.properties")
                    .relativeTo(subsystemResource.getServerConfiguration().getConfigurationType() == StandaloneServerConfiguration.RESOURCE_TYPE ? "jboss.server.config.dir" : "jboss.domain.config.dir")
                    .digestRealmName(securityRealmName)
            );
            propertiesRealmAddOperation.groupsProperties(new PropertiesRealmAddOperation.Properties("application-roles.properties")
                    .relativeTo(subsystemResource.getServerConfiguration().getConfigurationType() == StandaloneServerConfiguration.RESOURCE_TYPE ? "jboss.server.config.dir" : "jboss.domain.config.dir")
            );
            compositeOperationBuilder.addStep(propertiesRealmAddOperation.toModelNode());
        }

        protected void addDefaultManagementRealm(LegacySecurityConfiguration legacySecurityConfiguration, SubsystemResource subsystemResource, Operations.CompositeOperationBuilder compositeOperationBuilder, TaskContext taskContext) {
            final String securityRealmName = legacySecurityConfiguration.getDefaultElytronManagementRealmName();
            final PropertiesRealmAddOperation propertiesRealmAddOperation = new PropertiesRealmAddOperation(subsystemResource.getResourcePathAddress(), securityRealmName);
            propertiesRealmAddOperation.usersProperties(new PropertiesRealmAddOperation.Properties("mgmt-users.properties")
                    .relativeTo(subsystemResource.getServerConfiguration().getConfigurationType() == StandaloneServerConfiguration.RESOURCE_TYPE ? "jboss.server.config.dir" : "jboss.domain.config.dir")
                    .digestRealmName(securityRealmName)
            );
            propertiesRealmAddOperation.groupsProperties(new PropertiesRealmAddOperation.Properties("mgmt-groups.properties")
                    .relativeTo(subsystemResource.getServerConfiguration().getConfigurationType() == StandaloneServerConfiguration.RESOURCE_TYPE ? "jboss.server.config.dir" : "jboss.domain.config.dir")
            );
            compositeOperationBuilder.addStep(propertiesRealmAddOperation.toModelNode());
        }

        protected void addDefaultApplicationDomain(LegacySecurityConfiguration legacySecurityConfiguration, SubsystemResource subsystemResource, Operations.CompositeOperationBuilder compositeOperationBuilder, TaskContext taskContext) {
            final String securityDomainName = legacySecurityConfiguration.getDefaultElytronApplicationDomainName();
            final SecurityDomainAddOperation securityDomainAddOperation = new SecurityDomainAddOperation(subsystemResource.getResourcePathAddress(),securityDomainName)
                    .permissionMapper("default-permission-mapper")
                    .defaultRealm(legacySecurityConfiguration.getDefaultElytronApplicationRealmName())
                    .addRealm(new SecurityDomainAddOperation.Realm(legacySecurityConfiguration.getDefaultElytronApplicationRealmName())
                            .roleDecoder( "groups-to-roles"))
                    .addRealm(new SecurityDomainAddOperation.Realm("local"));
            compositeOperationBuilder.addStep(securityDomainAddOperation.toModelNode());
        }

        protected void addDefaultManagementDomain(LegacySecurityConfiguration legacySecurityConfiguration, SubsystemResource subsystemResource, Operations.CompositeOperationBuilder compositeOperationBuilder, TaskContext taskContext) {
            final String securityDomainName = legacySecurityConfiguration.getDefaultElytronManagementDomainName();
            final SecurityDomainAddOperation securityDomainAddOperation = new SecurityDomainAddOperation(subsystemResource.getResourcePathAddress(),securityDomainName)
                    .permissionMapper("default-permission-mapper")
                    .defaultRealm(legacySecurityConfiguration.getDefaultElytronManagementRealmName())
                    .addRealm(new SecurityDomainAddOperation.Realm(legacySecurityConfiguration.getDefaultElytronManagementRealmName())
                            .roleDecoder( "groups-to-roles"))
                    .addRealm(new SecurityDomainAddOperation.Realm("local").roleMapper("super-user-mapper"));
            compositeOperationBuilder.addStep(securityDomainAddOperation.toModelNode());
        }

        protected void addDefaultApplicationHttpAuthenticationFactory(LegacySecurityConfiguration legacySecurityConfiguration, SubsystemResource subsystemResource, Operations.CompositeOperationBuilder compositeOperationBuilder, TaskContext taskContext) {
            compositeOperationBuilder.addStep(new HttpAuthenticationFactoryAddOperation(subsystemResource.getResourcePathAddress(), legacySecurityConfiguration.getDefaultElytronApplicationHttpAuthenticationFactoryName())
                    .securityDomain(legacySecurityConfiguration.getDefaultElytronApplicationDomainName())
                    .httpServerMechanismFactory("global")
                    .addMechanismConfiguration(new MechanismConfiguration("BASIC").addMechanismRealmConfiguration(new MechanismRealmConfiguration(legacySecurityConfiguration.getDefaultElytronApplicationRealmName())))
                    .toModelNode());
        }

        protected void addDefaultManagementHttpAuthenticationFactory(LegacySecurityConfiguration legacySecurityConfiguration, SubsystemResource subsystemResource, Operations.CompositeOperationBuilder compositeOperationBuilder, TaskContext taskContext) {
            compositeOperationBuilder.addStep(new HttpAuthenticationFactoryAddOperation(subsystemResource.getResourcePathAddress(), legacySecurityConfiguration.getDefaultElytronManagementHttpAuthenticationFactoryName())
                    .securityDomain(legacySecurityConfiguration.getDefaultElytronManagementDomainName())
                    .httpServerMechanismFactory("global")
                    .addMechanismConfiguration(new MechanismConfiguration("DIGEST").addMechanismRealmConfiguration(new MechanismRealmConfiguration(legacySecurityConfiguration.getDefaultElytronManagementRealmName())))
                    .toModelNode());
        }

        protected void addDefaultApplicationSaslAuthenticationFactory(LegacySecurityConfiguration legacySecurityConfiguration, SubsystemResource subsystemResource, Operations.CompositeOperationBuilder compositeOperationBuilder, TaskContext taskContext) {
            compositeOperationBuilder.addStep(new SaslAuthenticationFactoryAddOperation(subsystemResource.getResourcePathAddress(), legacySecurityConfiguration.getDefaultElytronApplicationSaslAuthenticationFactoryName())
                    .securityDomain(legacySecurityConfiguration.getDefaultElytronApplicationDomainName())
                    .saslServerFactory("configured")
                    .addMechanismConfiguration(new MechanismConfiguration("JBOSS-LOCAL-USER").realmMapper("local"))
                    .addMechanismConfiguration(new MechanismConfiguration("DIGEST-MD5").addMechanismRealmConfiguration(new MechanismRealmConfiguration(legacySecurityConfiguration.getDefaultElytronApplicationRealmName())))
                    .toModelNode());
        }

        protected void addDefaultManagementSaslAuthenticationFactory(LegacySecurityConfiguration legacySecurityConfiguration, SubsystemResource subsystemResource, Operations.CompositeOperationBuilder compositeOperationBuilder, TaskContext taskContext) {
            compositeOperationBuilder.addStep(new SaslAuthenticationFactoryAddOperation(subsystemResource.getResourcePathAddress(), legacySecurityConfiguration.getDefaultElytronManagementSaslAuthenticationFactoryName())
                    .securityDomain(legacySecurityConfiguration.getDefaultElytronManagementDomainName())
                    .saslServerFactory("configured")
                    .addMechanismConfiguration(new MechanismConfiguration("JBOSS-LOCAL-USER").realmMapper("local"))
                    .addMechanismConfiguration(new MechanismConfiguration("DIGEST-MD5").addMechanismRealmConfiguration(new MechanismRealmConfiguration(legacySecurityConfiguration.getDefaultElytronManagementRealmName())))
                    .toModelNode());
        }

        protected void addDefaultTLS(LegacySecurityConfiguration legacySecurityConfiguration, SubsystemResource subsystemResource, Operations.CompositeOperationBuilder compositeOperationBuilder, TaskContext taskContext) {
            // add key-store
            compositeOperationBuilder.addStep(new KeystoreAddOperation(subsystemResource.getResourcePathAddress(), legacySecurityConfiguration.getDefaultElytronTLSKeyStoreName())
                    .keystorePassword("password")
                    .path("application.keystore")
                    .relativeTo(subsystemResource.getServerConfiguration().getConfigurationType() == StandaloneServerConfiguration.RESOURCE_TYPE ? "jboss.server.config.dir" : "jboss.domain.config.dir")
                    .type("JKS")
                    .toModelNode());
            // add key-manager
            compositeOperationBuilder.addStep(new KeyManagerAddOperation(subsystemResource.getResourcePathAddress(), legacySecurityConfiguration.getDefaultElytronTLSKeyManagerName())
                    .keystore(legacySecurityConfiguration.getDefaultElytronTLSKeyStoreName())
                    .generateSelfSignedCertificateHost(true)
                    .keyPassword("password")
                    .toModelNode());
            // add ssl server context
            compositeOperationBuilder.addStep(new ServerSSLContextAddOperation(subsystemResource.getResourcePathAddress(), legacySecurityConfiguration.getDefaultElytronTLSServerSSLContextName())
                    .keyManager(legacySecurityConfiguration.getDefaultElytronTLSKeyManagerName())
                    .toModelNode());
        }

        protected void migrateRemotingSubsystem(LegacySecurityConfiguration legacySecurityConfiguration, SubsystemResource subsystemResource, Operations.CompositeOperationBuilder compositeOperationBuilder, TaskContext taskContext) {
            // replace any remoting subsystem usage of the legacy security realm, with the created elytron sasl factory
            final Logger logger = taskContext.getLogger();
            logger.debugf("Looking for Remoting subsystem's connector and http-connector resources using a legacy security-realm...");
            final SubsystemResource remotingSubsystemResource = subsystemResource.getParentResource().getSubsystemResource(JBossSubsystemNames.REMOTING);
            if (remotingSubsystemResource != null) {
                final ModelNode remotingSubsystemConfig = remotingSubsystemResource.getResourceConfiguration();
                if (remotingSubsystemConfig.hasDefined(CONNECTOR)) {
                    for (Property remotingConnectorProperty : remotingSubsystemConfig.get(CONNECTOR).asPropertyList()) {
                        final String remotingConnectorName = remotingConnectorProperty.getName();
                        final ModelNode remotingConnectorConfig = remotingConnectorProperty.getValue();
                        if (remotingConnectorConfig.hasDefined(SECURITY_REALM)) {
                            // we found a connector using a legacy security-realm, update it to use the created Elytron's application sasl auth factory
                            logger.debugf("Remoting subsystem's connector resource using a legacy security-realm found.");
                            final PathAddress remotingConnectorAddress = remotingSubsystemResource.getResourcePathAddress().append(CONNECTOR, remotingConnectorName);
                            compositeOperationBuilder.addStep(getUndefineAttributeOperation(remotingConnectorAddress, SECURITY_REALM));
                            compositeOperationBuilder.addStep(getWriteAttributeOperation(remotingConnectorAddress, SASL_AUTHENTICATION_FACTORY, legacySecurityConfiguration.getDefaultElytronApplicationSaslAuthenticationFactoryName()));
                            logger.warnf("Migrated Remoting subsystem's connector resource %s using a legacy security-realm, to Elytron's default application SASL Authentication Factory %s. Please note that further manual Elytron configuration may be needed if the legacy security realm being used was not the source server's default Application Realm configuration!", remotingConnectorAddress.toPathStyleString(), legacySecurityConfiguration.getDefaultElytronApplicationSaslAuthenticationFactoryName());
                        }
                    }
                }
                if (remotingSubsystemConfig.hasDefined(HTTP_CONNECTOR)) {
                    for (Property remotingHttpConnectorProperty : remotingSubsystemConfig.get(HTTP_CONNECTOR).asPropertyList()) {
                        final String remotingHttpConnectorName = remotingHttpConnectorProperty.getName();
                        final ModelNode remotingHttpConnectorConfig = remotingHttpConnectorProperty.getValue();
                        if (remotingHttpConnectorConfig.hasDefined(SECURITY_REALM)) {
                            // we found a http connector using the legacy security-realm, update it to use the created Elytron's sasl factory
                            logger.debugf("Remoting subsystem's http connector resource using a legacy security-realm found.");
                            final PathAddress remotingConnectorAddress = remotingSubsystemResource.getResourcePathAddress().append(HTTP_CONNECTOR, remotingHttpConnectorName);
                            compositeOperationBuilder.addStep(getUndefineAttributeOperation(remotingConnectorAddress, SECURITY_REALM));
                            compositeOperationBuilder.addStep(getWriteAttributeOperation(remotingConnectorAddress, SASL_AUTHENTICATION_FACTORY, legacySecurityConfiguration.getDefaultElytronApplicationSaslAuthenticationFactoryName()));
                            logger.warnf("Migrated Remoting subsystem's http connector resource %s using a legacy security-realm, to Elytron's default application SASL Authentication Factory %s. Please note that further manual Elytron configuration may be needed if the legacy security realm being used was not the source server's default Application Realm configuration!", remotingConnectorAddress.toPathStyleString(), legacySecurityConfiguration.getDefaultElytronApplicationSaslAuthenticationFactoryName());
                        }
                    }
                }
                if (remotingSubsystemConfig.hasDefined(REMOTE_OUTBOUND_CONNECTION)) {
                    for (Property remoteOutboundConnectionProperty : remotingSubsystemConfig.get(REMOTE_OUTBOUND_CONNECTION).asPropertyList()) {
                        final String remoteOutboundConnectionName = remoteOutboundConnectionProperty.getName();
                        final ModelNode remoteOutboundConnectionConfig = remoteOutboundConnectionProperty.getValue();
                        if (remoteOutboundConnectionConfig.hasDefined(SECURITY_REALM)) {
                            // we found a remote outbound connection using the legacy security-realm, update it to use the created Elytron's authentication context
                            final String securityRealm = remoteOutboundConnectionConfig.get(SECURITY_REALM).asString();
                            logger.debugf("Remoting subsystem's remote outbound connection resource using a legacy security-realm %s found.", securityRealm);
                            final PathAddress remotingConnectorAddress = remotingSubsystemResource.getResourcePathAddress().append(HTTP_CONNECTOR, remoteOutboundConnectionName);
                            compositeOperationBuilder.addStep(getUndefineAttributeOperation(remotingConnectorAddress, SECURITY_REALM));
                            logger.warnf("Undefined the legacy security-realm %s attribute of Remoting subsystem's remote outbound connection resource %s. Please note that further manual Elytron configuration is needed to define appropriate authentication context for it!", remoteOutboundConnectionName, securityRealm);
                        }
                    }
                }
            }
        }

        protected void migrateUndertowSubsystem(LegacySecurityConfiguration legacySecurityConfiguration, SubsystemResource subsystemResource, Operations.CompositeOperationBuilder compositeOperationBuilder, TaskContext taskContext) {
            final Logger logger = taskContext.getLogger();
            // replace any undertow usage of the legacy security realm, with the created elytron ssl context
            logger.debugf("Looking for Undertow subsystem https-listener resources using a legacy security-realm...");
            final SubsystemResource undertowSubsystemResource = subsystemResource.getParentResource().getSubsystemResource(JBossSubsystemNames.UNDERTOW);
            if (undertowSubsystemResource != null) {
                final ModelNode undertowSubsystemConfig = undertowSubsystemResource.getResourceConfiguration();
                if (undertowSubsystemConfig.hasDefined(SERVER)) {
                    for (Property undertowServerProperty : undertowSubsystemConfig.get(SERVER).asPropertyList()) {
                        final String undertowServerName = undertowServerProperty.getName();
                        final ModelNode undertowServerConfig = undertowServerProperty.getValue();
                        if (undertowServerConfig.hasDefined(HTTPS_LISTENER)) {
                            for (Property undertowHttpsListenerProperty : undertowServerConfig.get(HTTPS_LISTENER).asPropertyList()) {
                                final String undertowHttpsListenerName = undertowHttpsListenerProperty.getName();
                                final ModelNode undertowHttpsListenerConfig = undertowHttpsListenerProperty.getValue();
                                if (undertowHttpsListenerConfig.hasDefined(SECURITY_REALM)) {
                                    // we found a http listener using the legacy security-realm, update it to use Elytron's server ssl context instead
                                    logger.debugf("Undertow subsystem https-listener resource using a legacy security-realm found.");
                                    final PathAddress undertowConnectorAddress = undertowSubsystemResource.getResourcePathAddress().append(SERVER, undertowServerName).append(HTTPS_LISTENER, undertowHttpsListenerName);
                                    compositeOperationBuilder.addStep(getUndefineAttributeOperation(undertowConnectorAddress, SECURITY_REALM));
                                    compositeOperationBuilder.addStep(getWriteAttributeOperation(undertowConnectorAddress, SSL_CONTEXT, legacySecurityConfiguration.getDefaultElytronTLSServerSSLContextName()));
                                    logger.warnf("Migrated Undertow subsystem https-listener resource %s using a legacy security-realm, to Elytron's default TLS ServerSSLContext %s. Please note that further manual Elytron configuration may be needed if the legacy security realm being used was not the source server's default Application Realm configuration!", undertowConnectorAddress.toPathStyleString(), legacySecurityConfiguration.getDefaultElytronTLSServerSSLContextName());
                                }
                            }
                        }
                        if (undertowServerConfig.hasDefined(HOST)) {
                            for (Property undertowHostProperty : undertowServerConfig.get(HOST).asPropertyList()) {
                                final String undertowHostName = undertowHostProperty.getName();
                                final ModelNode undertowHostConfig = undertowHostProperty.getValue();
                                final ModelNode undertowHttpInvokerConfig = undertowHostConfig.get(SETTING, HTTP_INVOKER);
                                if (undertowHttpInvokerConfig.isDefined() && undertowHttpInvokerConfig.hasDefined(SECURITY_REALM)) {
                                    logger.debugf("Undertow subsystem http-invoker resource using a legacy security-realm found.");
                                    final PathAddress pathAddress = undertowSubsystemResource.getResourcePathAddress().append(SERVER, undertowServerName).append(HOST, undertowHostName).append(SETTING, HTTP_INVOKER);
                                    compositeOperationBuilder.addStep(getUndefineAttributeOperation(pathAddress, SECURITY_REALM));
                                    compositeOperationBuilder.addStep(getWriteAttributeOperation(pathAddress, HTTP_AUTHENTICATION_FACTORY, legacySecurityConfiguration.getDefaultElytronApplicationHttpAuthenticationFactoryName()));
                                    logger.warnf("Migrated Undertow subsystem http-invoker resource %s using a legacy security-realm, to Elytron's default Application HTTP AuthenticationFactory %s. Please note that further manual Elytron configuration may be needed if the legacy security realm being used was not the source server's default Application Realm configuration!", pathAddress.toPathStyleString(), legacySecurityConfiguration.getDefaultElytronApplicationHttpAuthenticationFactoryName());
                                }
                            }
                        }
                    }
                    if (undertowSubsystemConfig.hasDefined(CONFIGURATION, FILTER, MOD_CLUSTER)) {
                        for (Property modClusterFilterProperty : undertowSubsystemConfig.get(CONFIGURATION).get(FILTER).get(MOD_CLUSTER).asPropertyList()) {
                            final String modClusterName = modClusterFilterProperty.getName();
                            final ModelNode modClusterConfig = modClusterFilterProperty.getValue();
                            if (modClusterConfig.hasDefined(SECURITY_REALM)) {
                                logger.debugf("Undertow subsystem's mod-cluster filter configuration resource using a legacy security-realm found.");
                                final PathAddress pathAddress = undertowSubsystemResource.getResourcePathAddress().append(CONFIGURATION, FILTER).append(MOD_CLUSTER, modClusterName);
                                compositeOperationBuilder.addStep(getUndefineAttributeOperation(pathAddress, SECURITY_REALM));
                                compositeOperationBuilder.addStep(getWriteAttributeOperation(pathAddress, SSL_CONTEXT, legacySecurityConfiguration.getDefaultElytronTLSServerSSLContextName()));
                                logger.warnf("Migrated Undertow subsystem's mod-cluster filter configuration resource %s using a legacy security-realm, to Elytron's default TLS ServerSSLContext %s. Please note that further manual Elytron configuration may be needed if the legacy security realm being used was not the source server's default Application Realm configuration!", pathAddress.toPathStyleString(), legacySecurityConfiguration.getDefaultElytronTLSServerSSLContextName());
                            }
                        }
                    }
                    if (undertowSubsystemConfig.hasDefined(CONFIGURATION, HANDLER, REVERSE_PROXY)) {
                        for (Property reverseProxyProperty : undertowSubsystemConfig.get(CONFIGURATION).get(HANDLER).get(REVERSE_PROXY).asPropertyList()) {
                            final String reverseProxyName = reverseProxyProperty.getName();
                            final ModelNode reverseProxyConfig = reverseProxyProperty.getValue();
                            if (reverseProxyConfig.hasDefined(HOST)) {
                                for (Property hostProperty : reverseProxyConfig.get(HOST).asPropertyList()) {
                                    final String hostName = hostProperty.getName();
                                    final ModelNode hostConfig = hostProperty.getValue();
                                    if (hostConfig.hasDefined(SECURITY_REALM)) {
                                        logger.debugf("Undertow subsystem's reverse-proxy handler configuration resource using a legacy security-realm found.");
                                        final PathAddress pathAddress = undertowSubsystemResource.getResourcePathAddress().append(CONFIGURATION, HANDLER).append(REVERSE_PROXY, reverseProxyName).append(HOST, hostName);
                                        compositeOperationBuilder.addStep(getUndefineAttributeOperation(pathAddress, SECURITY_REALM));
                                        compositeOperationBuilder.addStep(getWriteAttributeOperation(pathAddress, SSL_CONTEXT, legacySecurityConfiguration.getDefaultElytronTLSServerSSLContextName()));
                                        logger.warnf("Migrated Undertow subsystem's reverse-proxy handler configuration resource %s using a legacy security-realm, to Elytron's default TLS ServerSSLContext %s. Please note that further manual Elytron configuration may be needed if the legacy security realm being used was not the source server's default Application Realm configuration!", pathAddress.toPathStyleString(), legacySecurityConfiguration.getDefaultElytronTLSServerSSLContextName());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        protected void migrateIiopSubsystem(LegacySecurityConfiguration legacySecurityConfiguration, SubsystemResource subsystemResource, Operations.CompositeOperationBuilder compositeOperationBuilder, TaskContext taskContext) {
            final Logger logger = taskContext.getLogger();
            // replace any iiop-openjdk usage of the legacy security realm, with the created elytron ssl context
            logger.debugf("Looking for iiop-openjdk subsystem resources using a legacy realm...");
            final SubsystemResource iiopSubsystemResource = subsystemResource.getParentResource().getSubsystemResource(JBossSubsystemNames.IIOP_OPENJDK);
            if (iiopSubsystemResource != null) {
                if (iiopSubsystemResource.getResourceConfiguration().hasDefined(REALM)) {
                    final String realm = iiopSubsystemResource.getResourceConfiguration().get(REALM).asString();
                    logger.debugf("The iiop-openjdk subsystem resource using a legacy realm %s found.", realm);
                    compositeOperationBuilder.addStep(getUndefineAttributeOperation(iiopSubsystemResource.getResourcePathAddress(), REALM));
                    compositeOperationBuilder.addStep(getWriteAttributeOperation(iiopSubsystemResource.getResourcePathAddress(), SERVER_SSL_CONTEXT, legacySecurityConfiguration.getDefaultElytronTLSServerSSLContextName()));
                    compositeOperationBuilder.addStep(getWriteAttributeOperation(iiopSubsystemResource.getResourcePathAddress(), SERVER_REQUIRES_SSL, ModelNode.TRUE));
                    compositeOperationBuilder.addStep(getWriteAttributeOperation(iiopSubsystemResource.getResourcePathAddress(), SSL_SOCKET_BINDING, "iiop-ssl"));  // FIXME check if there is any iiop-ssl socket binding
                    logger.warnf("Migrated iiop-openjdk subsystem resource using a legacy realm %s, to Elytron's default TLS ServerSSLContext %s. Please note that further manual Elytron configuration may be needed if the legacy security realm being used was not the source server's default Application Realm configuration!", realm, legacySecurityConfiguration.getDefaultElytronTLSServerSSLContextName());
                }
            }
        }
    }

    public static class UpdateManagementInterfaces<S> extends ManageableServerConfigurationLeafTask.Builder<S> {

        private static final String SUBTASK_NAME = TASK_NAME + ".update-management-interfaces";

        protected UpdateManagementInterfaces(final LegacySecurityConfigurations legacySecurityConfigurations) {
            name(SUBTASK_NAME);
            skipPolicy(TaskSkipPolicy.skipIfDefaultTaskSkipPropertyIsSet());
            final ManageableResourceTaskRunnableBuilder<S, ManagementInterfaceResource.Parent> runnableBuilder = params -> context -> {
                final ManagementInterfaceResource.Parent resource = params.getResource();
                final LegacySecurityConfiguration legacySecurityConfiguration = legacySecurityConfigurations.getSecurityConfiguration(resource.getServerConfiguration().getConfigurationPath().getPath());
                final Operations.CompositeOperationBuilder compositeOperationBuilder = Operations.CompositeOperationBuilder.create();
                ServerMigrationTaskResult taskResult = ServerMigrationTaskResult.SKIPPED;
                if (legacySecurityConfiguration != null) {
                    final Logger logger = context.getLogger();
                    for (LegacySecuredManagementInterface managementInterface : legacySecurityConfiguration.getSecuredManagementInterfaces()) {
                        final ManagementInterfaceResource managementInterfaceResource = resource.getManagementInterfaceResource(managementInterface.getName());
                        if (managementInterface.getSecurityRealm() != null) {
                            logger.debugf("Migrating management interface %s secured by a legacy security-realm.", managementInterface.getName());
                            if (managementInterface.getName().equals(HTTP_INTERFACE)) {
                                // set http authentication factory
                                final ModelNode writeHttpAuthFactoryAttrOp = Util.createEmptyOperation(WRITE_ATTRIBUTE_OPERATION, managementInterfaceResource.getResourcePathAddress());
                                writeHttpAuthFactoryAttrOp.get(NAME).set(HTTP_AUTHENTICATION_FACTORY);
                                writeHttpAuthFactoryAttrOp.get(VALUE).set(legacySecurityConfiguration.getDefaultElytronManagementHttpAuthenticationFactoryName());
                                compositeOperationBuilder.addStep(writeHttpAuthFactoryAttrOp);
                                if (managementInterfaceResource.getResourceConfiguration().hasDefined(HTTP_UPGRADE)) {
                                    // set sasl auth factory
                                    final ModelNode writeSaslAuthFactoryAttrOp = Util.createEmptyOperation(WRITE_ATTRIBUTE_OPERATION, managementInterfaceResource.getResourcePathAddress());
                                    writeSaslAuthFactoryAttrOp.get(NAME).set(HTTP_UPGRADE + "." + SASL_AUTHENTICATION_FACTORY);
                                    writeSaslAuthFactoryAttrOp.get(VALUE).set(legacySecurityConfiguration.getDefaultElytronManagementSaslAuthenticationFactoryName());
                                    compositeOperationBuilder.addStep(writeSaslAuthFactoryAttrOp);
                                }
                                logger.warnf("Migrated management interface %s secured by a legacy security-realm, to Elytron's default Management HTTP AuthenticationFactory %s. Please note that further manual Elytron configuration may be needed if the legacy security realm being used was not the source server's default Application Realm configuration!", managementInterface.getName(), legacySecurityConfiguration.getDefaultElytronManagementHttpAuthenticationFactoryName());
                                taskResult = ServerMigrationTaskResult.SUCCESS;
                            } else if (managementInterface.getName().equals(NATIVE_INTERFACE)) {
                                // set sasl auth factory
                                final ModelNode writeSaslAuthFactoryAttrOp = Util.createEmptyOperation(WRITE_ATTRIBUTE_OPERATION, managementInterfaceResource.getResourcePathAddress());
                                writeSaslAuthFactoryAttrOp.get(NAME).set(SASL_AUTHENTICATION_FACTORY);
                                writeSaslAuthFactoryAttrOp.get(VALUE).set(legacySecurityConfiguration.getDefaultElytronManagementSaslAuthenticationFactoryName());
                                compositeOperationBuilder.addStep(writeSaslAuthFactoryAttrOp);
                                logger.warnf("Migrated management interface %s secured by a legacy security-realm, to Elytron's default Management SASL AuthenticationFactory %s. Please note that further manual Elytron configuration may be needed if the legacy security realm being used was not the source server's default Application Realm configuration!", managementInterface.getName(), legacySecurityConfiguration.getDefaultElytronManagementSaslAuthenticationFactoryName());

                                taskResult = ServerMigrationTaskResult.SUCCESS;
                            } else {
                                context.getLogger().debugf("Skipping Management interface %s.", managementInterface.getName());
                            }
                        }
                    }
                }
                if (taskResult == ServerMigrationTaskResult.SUCCESS) {
                    context.getLogger().debugf("Legacy secure management interfaces op to migrate to Elytron: %s", compositeOperationBuilder.build().getOperation());
                    resource.getServerConfiguration().executeManagementOperation(compositeOperationBuilder.build().getOperation());
                    context.getLogger().debugf("Management interfaces secured by legacy security realms migrated to Elytron.");
                }
                return taskResult;
            };
            runBuilder(ManagementInterfaceResource.Parent.class, runnableBuilder);
        }
    }

    public static class UpdateDomainController<S> extends ManageableServerConfigurationLeafTask.Builder<S> {

        private static final String SUBTASK_NAME = TASK_NAME + ".update-domain-controller";

        protected UpdateDomainController(final LegacySecurityConfigurations legacySecurityConfigurations) {
            name(SUBTASK_NAME);
            skipPolicy(TaskSkipPolicy.skipIfDefaultTaskSkipPropertyIsSet());
            final ManageableResourceTaskRunnableBuilder<S, ManageableServerConfiguration> runnableBuilder = params -> context -> {
                if (params.getResource().getConfigurationType() != HostConfiguration.RESOURCE_TYPE) {
                    return ServerMigrationTaskResult.SKIPPED;
                }
                final LegacySecurityConfiguration legacySecurityConfiguration = legacySecurityConfigurations.getSecurityConfigurations().get(params.getServerConfiguration().getConfigurationPath().getPath().toString());
                if (legacySecurityConfiguration == null) {
                    return ServerMigrationTaskResult.SKIPPED;
                }
                final String domainControllerRemoteSecurityRealm = legacySecurityConfiguration.getDomainControllerRemoteSecurityRealm();
                if (domainControllerRemoteSecurityRealm == null) {
                    return ServerMigrationTaskResult.SKIPPED;
                }
                context.getLogger().debug("Migrating Domain Controller configuration to Elytron...");
                if (!params.getResource().getResourceConfiguration().hasDefined(DOMAIN_CONTROLLER, REMOTE)) {
                    return ServerMigrationTaskResult.SKIPPED;
                }

                // TODO code below is wrong, attr should point to a new Elytron auth context related with server identity on the legacy security realm (e.g. secret), but this is not yet used on default configs so for now we do nothing
                //final ModelNode domainControllerConfig = params.getResource().getResourceConfiguration().get(DOMAIN_CONTROLLER).clone();
                // domainControllerConfig.get(REMOTE).get(AUTHENTICATION_CONTEXT).set(LegacySecurityRealm.getElytronSaslAuthenticationFactoryName(domainControllerRemoteSecurityRealm));
                //final ModelNode op = getWriteAttributeOperation(params.getResource().getResourcePathAddress(), DOMAIN_CONTROLLER, domainControllerConfig);
                //params.getServerConfiguration().executeManagementOperation(op);

                context.getLogger().warnf("Domain Controller configuration usage of legacy security realms removed.  Please note that further manual Elytron configuration may be needed to accomplish the source's configuration functionality!");
                return ServerMigrationTaskResult.SUCCESS;
            };
            runBuilder(ManageableServerConfiguration.class, runnableBuilder);
        }
    }

}