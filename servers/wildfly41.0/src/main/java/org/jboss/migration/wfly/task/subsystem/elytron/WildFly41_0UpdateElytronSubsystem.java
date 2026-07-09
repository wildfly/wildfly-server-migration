/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.wfly.task.subsystem.elytron;

import static org.jboss.as.controller.operations.common.Util.createRemoveOperation;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;
import org.jboss.migration.core.env.TaskEnvironment;
import org.jboss.migration.core.jboss.JBossSubsystemNames;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.wfly.WildFly41_0Server;
import org.jboss.migration.wfly10.config.management.ManageableServerConfiguration;
import org.jboss.migration.wfly10.config.management.SubsystemResource;
import org.jboss.migration.wfly10.config.task.management.subsystem.UpdateSubsystemResourceSubtaskBuilder;
import org.jboss.migration.wfly10.config.task.management.subsystem.UpdateSubsystemResources;

/**
 * @author emartins
 */
public class WildFly41_0UpdateElytronSubsystem<S> extends UpdateSubsystemResources<S> {

    public WildFly41_0UpdateElytronSubsystem() {
        super(JBossSubsystemNames.ELYTRON, new RemovePoliciesSubtaskBuilder<>());
    }

    static class RemovePoliciesSubtaskBuilder<S> extends UpdateSubsystemResourceSubtaskBuilder<S> {

        private static final String POLICY = "policy";

        RemovePoliciesSubtaskBuilder() {
            subtaskName("remove-policies");
        }

        @Override
        protected ServerMigrationTaskResult updateConfiguration(ModelNode config, S source, SubsystemResource subsystemResource, TaskContext taskContext, TaskEnvironment taskEnvironment) {
            final ManageableServerConfiguration serverConfiguration = subsystemResource.getServerConfiguration();
            if (((WildFly41_0Server)serverConfiguration.getServer()).getTechnologyType() == WildFly41_0Server.TechnologyType.LEGACY) {
                return ServerMigrationTaskResult.SKIPPED;
            }
            final PathAddress subsystemPathAddress = subsystemResource.getResourcePathAddress();
            final Operations.CompositeOperationBuilder compositeOperationBuilder = Operations.CompositeOperationBuilder.create();
            // do update
            boolean update = false;
            final ModelNode subsystemConfig = subsystemResource.getResourceConfiguration();
            if (subsystemConfig.hasDefined(POLICY)) {
                for (String policyName : subsystemConfig.get(POLICY).keys()) {
                    taskContext.getLogger().debugf("Removing policy %s from elytron configuration %s!", policyName, subsystemResource.getResourceAbsoluteName());
                    compositeOperationBuilder.addStep(createRemoveOperation(subsystemPathAddress.append(POLICY, policyName)));
                    update = true;
                }
            }
            if (update) {
                serverConfiguration.executeManagementOperation(compositeOperationBuilder.build().getOperation());
                return ServerMigrationTaskResult.SUCCESS;
            } else {
                return ServerMigrationTaskResult.SKIPPED;
            }
        }
    }
}
