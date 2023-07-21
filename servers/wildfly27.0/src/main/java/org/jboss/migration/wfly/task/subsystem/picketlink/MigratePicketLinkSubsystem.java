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

package org.jboss.migration.wfly.task.subsystem.picketlink;

import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.migration.core.ServerMigrationFailureException;
import org.jboss.migration.core.jboss.JBossExtensionNames;
import org.jboss.migration.core.jboss.JBossSubsystemNames;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.wfly10.config.management.ManageableServerConfiguration;
import org.jboss.migration.wfly10.config.management.ManagementOperationException;
import org.jboss.migration.wfly10.config.management.SubsystemResource;
import org.jboss.migration.wfly10.config.task.management.subsystem.MigrateSubsystemResourceSubtaskBuilder;
import org.jboss.migration.wfly10.config.task.management.subsystem.MigrateSubsystemResources;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODULE;

/**
 * @author istudens
 */
public class MigratePicketLinkSubsystem<S> extends MigrateSubsystemResources<S> {

    public MigratePicketLinkSubsystem() {
        super(JBossExtensionNames.PICKETLINK, new MigratePicketLinkSubsystemSubtaskBuilder());
    }

    protected static class MigratePicketLinkSubsystemSubtaskBuilder<S> extends MigrateSubsystemResourceSubtaskBuilder<S> {

        public MigratePicketLinkSubsystemSubtaskBuilder() {
            super(JBossSubsystemNames.PICKETLINK_FEDERATION);
        }

        @Override
        protected ServerMigrationTaskResult migrateConfiguration(SubsystemResource picketlinkSubsystemResource, TaskContext taskContext) {
            // check if the keycloak saml adapter module is present
            // /extension=org.keycloak.keycloak-saml-adapter-subsystem:add(module=org.keycloak.keycloak-saml-adapter-subsystem)
            final ManageableServerConfiguration serverConfiguration = picketlinkSubsystemResource.getServerConfiguration();
            try {
                final ModelNode op = Util.createAddOperation(serverConfiguration.getExtensionResourcePathAddress(JBossExtensionNames.KEYCLOAK_SAML));
                op.get(MODULE).set(JBossExtensionNames.KEYCLOAK_SAML);
                serverConfiguration.executeManagementOperation(op);
            } catch (ManagementOperationException e) {
                if (e.getMessage().contains("WFLYCTL0310")) {
                    throw new ServerMigrationFailureException("The legacy picketlink-federation subsystem cannot be migrated to the new keycloak-adapter subsystem due to missing Keycloak client SAML adapter on the target server.", e);
                }
            }

            // do standard subsystem config migration
            return super.migrateConfiguration(picketlinkSubsystemResource, taskContext);
        }
    }
}
