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

package org.jboss.migration.wfly.task.subsystem.keycloak;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.migration.core.jboss.JBossExtensionNames;
import org.jboss.migration.core.jboss.JBossSubsystemNames;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.wfly.task.security.LegacySecurityConfiguration;
import org.jboss.migration.wfly10.config.management.ManageableServerConfiguration;
import org.jboss.migration.wfly10.config.management.SubsystemResource;
import org.jboss.migration.wfly10.config.task.management.subsystem.MigrateSubsystemResourceSubtaskBuilder;
import org.jboss.migration.wfly10.config.task.management.subsystem.MigrateSubsystemResources;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HTTP_AUTHENTICATION_FACTORY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODULE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SASL_AUTHENTICATION_FACTORY;
import static org.jboss.as.domain.management.ModelDescriptionConstants.SECURITY_DOMAIN;
import static org.jboss.migration.wfly.task.security.MigrateLegacySecurityDomainsToElytron.UpdateSubsystems.APPLICATION_SECURITY_DOMAIN;

/**
 * @author istudens
 */
public class MigrateKeycloakSubsystem<S> extends MigrateSubsystemResources<S> {

    public MigrateKeycloakSubsystem() {
        super(JBossExtensionNames.KEYCLOAK, new MigrateKeycloakSubsystemSubtaskBuilder<>());
    }

    protected static class MigrateKeycloakSubsystemSubtaskBuilder<S> extends MigrateSubsystemResourceSubtaskBuilder<S> {

        private static final String KEYCLOAK_ADAPTER_MODULE_NAME = "org.keycloak.keycloak-wildfly-elytron-oidc-adapter";

        private static final String AGGREGATE_HTTP_SERVER_MECHANISM_FACTORY = "aggregate-http-server-mechanism-factory";
        private static final String CONSTANT_REALM_MAPPER = "constant-realm-mapper";
        private static final String CUSTOM_REALM = "custom-realm";
        private static final String DEFAULT_MIGRATED_APPLICATION_HTTP_AUTHENTICATION_FACTORY = LegacySecurityConfiguration.DEFAULT_ELYTRON_APPLICATION_HTTP_AUTHENTICATION_FACTORY_NAME;
        private static final String DEFAULT_MIGRATED_APPLICATION_SASL_AUTHENTICATION_FACTORY = LegacySecurityConfiguration.DEFAULT_ELYTRON_APPLICATION_SASL_AUTHENTICATION_FACTORY_NAME;
        private static final String DEFAULT_MIGRATED_APPLICATION_SECURITY_DOMAIN = LegacySecurityConfiguration.DEFAULT_ELYTRON_APPLICATION_DOMAIN_NAME;
        private static final String HTTP_CONNECTOR = "http-connector";
        private static final String REALM_NAME = "realm-name";
        private static final String REALMS = "realms";
        private static final String SERVICE_LOADER_HTTP_SERVER_MECHANISM = "service-loader-http-server-mechanism-factory";


        public MigrateKeycloakSubsystemSubtaskBuilder() {
            super(JBossSubsystemNames.KEYCLOAK);
        }

        @Override
        protected ServerMigrationTaskResult migrateConfiguration(SubsystemResource keycloakSubsystemResource, TaskContext taskContext) {
            // do standard subsystem config migration
            final ServerMigrationTaskResult taskResult = super.migrateConfiguration(keycloakSubsystemResource, taskContext);

            // remove any keycloak resources
            if (taskResult.getStatus() == ServerMigrationTaskResult.Status.SUCCESS) {
                final SubsystemResource elytronSubsystemResource = keycloakSubsystemResource.getParentResource().getSubsystemResource(JBossSubsystemNames.ELYTRON);
                final ModelNode elytronSubsystemConfig = elytronSubsystemResource.getResourceConfiguration();
                final PathAddress elytronSubsystemAddress = elytronSubsystemResource.getResourcePathAddress();

                final Operations.CompositeOperationBuilder compositeOperationBuilder = Operations.CompositeOperationBuilder.create();

                // remove keycloak custom realm if any
                removeKeycloakCustomRealm(KEYCLOAK_ADAPTER_MODULE_NAME, keycloakSubsystemResource, elytronSubsystemConfig, elytronSubsystemAddress, compositeOperationBuilder);

                // remove keycloak service loader http server mechanism factory if any
                removeKeycloakServiceLoaderHttpServerMechanismFactory(KEYCLOAK_ADAPTER_MODULE_NAME, keycloakSubsystemResource, elytronSubsystemConfig, elytronSubsystemAddress, compositeOperationBuilder);

                // execute the composite operation
                final ManageableServerConfiguration serverConfiguration = elytronSubsystemResource.getServerConfiguration();
                serverConfiguration.executeManagementOperation(compositeOperationBuilder.build().getOperation());

            }
            return taskResult;
        }

        protected void removeKeycloakCustomRealm(String keycloakModule, SubsystemResource keycloakSubsystemResource, ModelNode elytronSubsystemConfig, PathAddress elytronSubsystemAddress, Operations.CompositeOperationBuilder compositeOperationBuilder) {
            // look for any custom realm based by the given keycloak module
            elytronSubsystemConfig.get(CUSTOM_REALM).asPropertyListOrEmpty()
                    .stream()
                    .filter(p -> p.getValue().hasDefined(MODULE))
                    .filter(p -> p.getValue().get(MODULE).asString().equals(keycloakModule))
                    .forEach(p -> {
                        final String customRealmName = p.getName();
                        // remove security domain configured to the keycloak realm if any
                        removeKeycloakSecurityDomain(customRealmName, keycloakSubsystemResource, elytronSubsystemConfig, elytronSubsystemAddress, compositeOperationBuilder);

                        // remove keycloak constant realm mapper if any
                        removeKeycloakConstantRealmMapper(customRealmName, elytronSubsystemConfig, elytronSubsystemAddress, compositeOperationBuilder);

                        // and remove the realm
                        compositeOperationBuilder.addStep(Util.createRemoveOperation(elytronSubsystemAddress.append(CUSTOM_REALM, customRealmName)));
                    });
        }

        protected void removeKeycloakServiceLoaderHttpServerMechanismFactory(String keycloakModule, SubsystemResource keycloakSubsystemResource, ModelNode elytronSubsystemConfig, PathAddress elytronSubsystemAddress, Operations.CompositeOperationBuilder compositeOperationBuilder) {
            // look for any service loader http server mechanism based by the given keycloak module
            elytronSubsystemConfig.get(SERVICE_LOADER_HTTP_SERVER_MECHANISM).asPropertyListOrEmpty()
                    .stream()
                    .filter(p -> p.getValue().hasDefined(MODULE))
                    .filter(p -> p.getValue().get(MODULE).asString().equals(keycloakModule))
                    .forEach(p -> {
                        final String serviceLoaderName = p.getName();
                        // remove keycloak service loader factory from the aggregate http server mechanism factory if any
                        removeKeycloakAggregateHttpServerMechanismFactory(serviceLoaderName, elytronSubsystemConfig, elytronSubsystemAddress, compositeOperationBuilder);

                        // and remove the keycloak service loader
                        compositeOperationBuilder.addStep(Util.createRemoveOperation(elytronSubsystemAddress.append(SERVICE_LOADER_HTTP_SERVER_MECHANISM, serviceLoaderName)));
                    });
        }

        protected void removeKeycloakAggregateHttpServerMechanismFactory(String keycloakServiceLoaderFactory, ModelNode elytronSubsystemConfig, PathAddress elytronSubsystemAddress, Operations.CompositeOperationBuilder compositeOperationBuilder) {
            // look for any aggregate http server mechanism based by the given keycloak service loader http server mechanism factory
            elytronSubsystemConfig.get(AGGREGATE_HTTP_SERVER_MECHANISM_FACTORY).asPropertyListOrEmpty()
                    .forEach(p -> p.getValue().asPropertyListOrEmpty()      // aggregate-http-server-mechanism-factory
                            .forEach(p2 -> p2.getValue().asListOrEmpty()    // http-server-mechanism-factory
                                    .stream()
                                    .filter(p3 -> p3.asString().equals(keycloakServiceLoaderFactory))   // name of http-server-mechanism-factory
                                    .forEach(p3 -> {
                                        final String aggregateHttpServerMechanismFactoryName = p.getName();
                                        // remove the aggregate http server mechanism factory
                                        compositeOperationBuilder.addStep(Util.createRemoveOperation(elytronSubsystemAddress.append(AGGREGATE_HTTP_SERVER_MECHANISM_FACTORY, aggregateHttpServerMechanismFactoryName)));
                                    }))
                    );
        }

        protected void removeKeycloakConstantRealmMapper(String keycloakRealmName, ModelNode elytronSubsystemConfig, PathAddress elytronSubsystemAddress, Operations.CompositeOperationBuilder compositeOperationBuilder) {
            // look for any constant realm mapper configured to the given keycloak realm
            elytronSubsystemConfig.get(CONSTANT_REALM_MAPPER).asPropertyListOrEmpty()
                    .stream()
                    .filter(p -> p.getValue().hasDefined(REALM_NAME))
                    .filter(p -> p.getValue().get(REALM_NAME).asString().equals(keycloakRealmName))
                    .forEach(p -> {
                        final String realmMapperName = p.getName();
                        // remove the constant realm mapper
                        compositeOperationBuilder.addStep(Util.createRemoveOperation(elytronSubsystemAddress.append(CONSTANT_REALM_MAPPER, realmMapperName)));
                    });
        }

        protected void removeKeycloakSecurityDomain(String keycloakRealmName, SubsystemResource keycloakSubsystemResource, ModelNode elytronSubsystemConfig, PathAddress elytronSubsystemAddress, Operations.CompositeOperationBuilder compositeOperationBuilder) {
            // look for any security domain configured to the given keycloak realm
            elytronSubsystemConfig.get(SECURITY_DOMAIN).asPropertyListOrEmpty()
                    .forEach(p -> p.getValue().get(REALMS).asListOrEmpty()
                            .stream()
                            .filter(node -> node.hasDefined(REALM))
                            .filter(node -> node.get(REALM).asString().equals(keycloakRealmName))
                            .forEach(node -> {
                                final String securityDomainName = p.getName();
                                // remove http authentication factory bounded to the keycloak security domain if any
                                removeKeycloakHttpAuthenticationFactory(securityDomainName, keycloakSubsystemResource, elytronSubsystemConfig, elytronSubsystemAddress, compositeOperationBuilder);

                                // remove sasl authentication factory bounded to the keycloak security domain if any
                                removeKeycloakSaslAuthenticationFactory(securityDomainName, keycloakSubsystemResource, elytronSubsystemConfig, elytronSubsystemAddress, compositeOperationBuilder);

                                // update ejb3's application security domain based by keycloak if any and point it to another one
                                updateKeycloakEJB3ApplicationSecurityDomain(securityDomainName, keycloakSubsystemResource, compositeOperationBuilder);

                                // remove the security domain
                                compositeOperationBuilder.addStep(Util.createRemoveOperation(elytronSubsystemAddress.append(SECURITY_DOMAIN, securityDomainName)));
                            })
                    );
        }

        protected void removeKeycloakHttpAuthenticationFactory(String keycloakSecurityDomainName, SubsystemResource keycloakSubsystemResource, ModelNode elytronSubsystemConfig, PathAddress elytronSubsystemAddress, Operations.CompositeOperationBuilder compositeOperationBuilder) {
            // look for any http authentication factory configured to the given keycloak security domain
            elytronSubsystemConfig.get(HTTP_AUTHENTICATION_FACTORY).asPropertyListOrEmpty()
                    .stream()
                    .filter(p -> p.getValue().hasDefined(SECURITY_DOMAIN))
                    .filter(p -> p.getValue().get(SECURITY_DOMAIN).asString().equals(keycloakSecurityDomainName))
                    .forEach(p -> {
                        final String httpAuthenticationFactoryName = p.getName();
                        // update Undertow's application security domain based by keycloak if any and point it to another security domain
                        updateKeycloakUndertowApplicationSecurityDomain(httpAuthenticationFactoryName, keycloakSubsystemResource, compositeOperationBuilder);

                        // remove the keycloak http authentication factory
                        compositeOperationBuilder.addStep(Util.createRemoveOperation(elytronSubsystemAddress.append(HTTP_AUTHENTICATION_FACTORY, httpAuthenticationFactoryName)));
                    });
        }

        protected void removeKeycloakSaslAuthenticationFactory(String keycloakSecurityDomainName, SubsystemResource keycloakSubsystemResource, ModelNode elytronSubsystemConfig, PathAddress elytronSubsystemAddress, Operations.CompositeOperationBuilder compositeOperationBuilder) {
            // look for any http authentication factory configured to the given keycloak security domain
            elytronSubsystemConfig.get(SASL_AUTHENTICATION_FACTORY).asPropertyListOrEmpty()
                    .stream()
                    .filter(p -> p.getValue().hasDefined(SECURITY_DOMAIN))
                    .filter(p -> p.getValue().get(SECURITY_DOMAIN).asString().equals(keycloakSecurityDomainName))
                    .forEach(p -> {
                        final String saslAuthenticationFactoryName = p.getName();
                        // update Remoting's http connector based by keycloak if any and point it to the default sasl authentication factory
                        updateKeycloakRemotingHttpConnectorSaslAuthenticationFactory(saslAuthenticationFactoryName, keycloakSubsystemResource, compositeOperationBuilder);

                        // remove the keycloak sasl authentication factory
                        compositeOperationBuilder.addStep(Util.createRemoveOperation(elytronSubsystemAddress.append(SASL_AUTHENTICATION_FACTORY, saslAuthenticationFactoryName)));
                    });
        }

        protected void updateKeycloakRemotingHttpConnectorSaslAuthenticationFactory(String keycloakSaslAuthenticationFactoryName, SubsystemResource keycloakSubsystemResource, Operations.CompositeOperationBuilder compositeOperationBuilder) {
            final SubsystemResource remotingSubsystemResource = keycloakSubsystemResource.getParentResource().getSubsystemResource(JBossSubsystemNames.REMOTING);
            final ModelNode remotingSubsystemConfig = remotingSubsystemResource.getResourceConfiguration();
            final PathAddress remotingSubsystemAddress = remotingSubsystemResource.getResourcePathAddress();
            // look for any Remoting's http-connector configured to the given keycloak sasl authentication factory
            remotingSubsystemConfig.get(HTTP_CONNECTOR).asPropertyListOrEmpty()
                    .stream()
                    .filter(p -> p.getValue().hasDefined(SASL_AUTHENTICATION_FACTORY))
                    .filter(p -> p.getValue().get(SASL_AUTHENTICATION_FACTORY).asString().equals(keycloakSaslAuthenticationFactoryName))
                    .forEach(p -> {
                        final String httpConnectorName = p.getName();
                        // update Remoting http connector's sasl authentication factory to the migrated default one
                        ModelNode updateOp = Util.getWriteAttributeOperation(
                                remotingSubsystemAddress.append(HTTP_CONNECTOR, httpConnectorName),
                                SASL_AUTHENTICATION_FACTORY,
                                DEFAULT_MIGRATED_APPLICATION_SASL_AUTHENTICATION_FACTORY);
                        //FIXME check if there is any such migrated factory
                        compositeOperationBuilder.addStep(updateOp);
                    });
        }

        protected void updateKeycloakUndertowApplicationSecurityDomain(String keycloakHttpAuthenticationFactoryName, SubsystemResource keycloakSubsystemResource, Operations.CompositeOperationBuilder compositeOperationBuilder) {
            final SubsystemResource undertowSubsystemResource = keycloakSubsystemResource.getParentResource().getSubsystemResource(JBossSubsystemNames.UNDERTOW);
            final ModelNode undertowSubsystemConfig = undertowSubsystemResource.getResourceConfiguration();
            final PathAddress undertowSubsystemAddress = undertowSubsystemResource.getResourcePathAddress();
            // look for any Undertow's application security domain configured to the given keycloak http authentication factory
            undertowSubsystemConfig.get(APPLICATION_SECURITY_DOMAIN).asPropertyListOrEmpty()
                    .stream()
                    .filter(p -> p.getValue().hasDefined(HTTP_AUTHENTICATION_FACTORY))
                    .filter(p -> p.getValue().get(HTTP_AUTHENTICATION_FACTORY).asString().equals(keycloakHttpAuthenticationFactoryName))
                    .forEach(p -> {
                        final String applicationSecurityDomainName = p.getName();
                        // update Undertow's application security domain to the migrated default one
                        ModelNode updateOp = Util.getWriteAttributeOperation(
                                undertowSubsystemAddress.append(APPLICATION_SECURITY_DOMAIN, applicationSecurityDomainName),
                                HTTP_AUTHENTICATION_FACTORY,
                                DEFAULT_MIGRATED_APPLICATION_HTTP_AUTHENTICATION_FACTORY);
                        //FIXME check if there is any such migrated factory
                        compositeOperationBuilder.addStep(updateOp);
                    });
        }

        protected void updateKeycloakEJB3ApplicationSecurityDomain(String keycloakSecurityDomain, SubsystemResource keycloakSubsystemResource, Operations.CompositeOperationBuilder compositeOperationBuilder) {
            final SubsystemResource ejb3SubsystemResource = keycloakSubsystemResource.getParentResource().getSubsystemResource(JBossSubsystemNames.EJB3);
            final ModelNode ejb3SubsystemConfig = ejb3SubsystemResource.getResourceConfiguration();
            final PathAddress ejb3SubsystemAddress = ejb3SubsystemResource.getResourcePathAddress();
            // look for any http authentication factory configured to the given keycloak security domain
            ejb3SubsystemConfig.get(APPLICATION_SECURITY_DOMAIN).asPropertyListOrEmpty()
                    .stream()
                    .filter(p -> p.getValue().hasDefined(SECURITY_DOMAIN))
                    .filter(p -> p.getValue().get(SECURITY_DOMAIN).asString().equals(keycloakSecurityDomain))
                    .forEach(p -> {
                        final String applicationSecurityDomainName = p.getName();
                        // update EJB3's application security domain to the migrated default one
                        ModelNode updateOp = Util.getWriteAttributeOperation(
                                ejb3SubsystemAddress.append(APPLICATION_SECURITY_DOMAIN, applicationSecurityDomainName),
                                SECURITY_DOMAIN,
                                DEFAULT_MIGRATED_APPLICATION_SECURITY_DOMAIN);
                        //FIXME check if there is any such migrated security domain
                        compositeOperationBuilder.addStep(updateOp);
                    });
        }
    }
}
