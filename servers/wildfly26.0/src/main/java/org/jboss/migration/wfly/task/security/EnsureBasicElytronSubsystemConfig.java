/*
 * Copyright 2017 Red Hat, Inc.
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
import org.jboss.migration.wfly10.config.management.ManageableServerConfiguration;
import org.jboss.migration.wfly11.task.subsystem.elytron.ConfigurableSaslServerFactoryAddOperation;
import org.jboss.migration.wfly11.task.subsystem.elytron.IdentityRealmAddOperation;
import org.jboss.migration.wfly11.task.subsystem.elytron.MechanismProviderFilteringSaslServerFactoryAddOperation;
import org.jboss.migration.wfly11.task.subsystem.elytron.ProviderHttpServerMechanismFactoryAddOperation;
import org.jboss.migration.wfly11.task.subsystem.elytron.ProviderSaslServerFactoryAddOperation;
import org.jboss.migration.wfly13.task.subsystem.elytron.WildFly13_0AddElytronSubsystemConfig;

/**
 * A task which adds a basic Elytron config, if missing.
 * @author emmartins
 */
public class EnsureBasicElytronSubsystemConfig<S> extends WildFly13_0AddElytronSubsystemConfig<S> {

    private final LegacySecurityConfigurations legacySecurityConfigurations;

    public EnsureBasicElytronSubsystemConfig(LegacySecurityConfigurations legacySecurityConfigurations) {
        this.legacySecurityConfigurations = legacySecurityConfigurations;
    }

    @Override
    protected void addSecurityDomains(final ManageableServerConfiguration configuration, final PathAddress subsystemPathAddress, final Operations.CompositeOperationBuilder compositeOperationBuilder) {
        // no default secuity domain components needed by migrated legacy default realm/domain configs
    }

    @Override
    protected void addSecurityRealms(ManageableServerConfiguration configuration, PathAddress subsystemPathAddress, Operations.CompositeOperationBuilder compositeOperationBuilder) {
        // only add default security realm components needed by migrated legacy default realm/domain configs
        compositeOperationBuilder.addStep(new IdentityRealmAddOperation(subsystemPathAddress, "local")
                .identity("$local")
                .toModelNode());
    }

    @Override
    protected void addHttp(ManageableServerConfiguration configuration, PathAddress subsystemPathAddress, Operations.CompositeOperationBuilder compositeOperationBuilder) {
        // only add default http components needed by migrated legacy default realm/domain configs
        compositeOperationBuilder.addStep(new ProviderHttpServerMechanismFactoryAddOperation(subsystemPathAddress, "global")
                .toModelNode());
    }

    @Override
    protected void addSasl(ManageableServerConfiguration configuration, PathAddress subsystemPathAddress, Operations.CompositeOperationBuilder compositeOperationBuilder) {
        // only add default sasl components needed by migrated legacy default realm/domain configs
        compositeOperationBuilder.addStep(new ProviderSaslServerFactoryAddOperation(subsystemPathAddress, "global")
                .toModelNode());
        compositeOperationBuilder.addStep(new MechanismProviderFilteringSaslServerFactoryAddOperation(subsystemPathAddress, "elytron")
                .saslServerFactory("global")
                .addFilter("WildFlyElytron")
                .toModelNode());
        compositeOperationBuilder.addStep(new ConfigurableSaslServerFactoryAddOperation(subsystemPathAddress, "configured")
                .saslServerFactory("elytron")
                .addProperty("wildfly.sasl.local-user.default-user", "$local")
                .toModelNode());
    }

    @Override
    protected void addAuditLogging(ManageableServerConfiguration configuration, PathAddress subsystemPathAddress, Operations.CompositeOperationBuilder compositeOperationBuilder) {
        super.addAuditLogging(configuration, subsystemPathAddress, compositeOperationBuilder);
    }
}
