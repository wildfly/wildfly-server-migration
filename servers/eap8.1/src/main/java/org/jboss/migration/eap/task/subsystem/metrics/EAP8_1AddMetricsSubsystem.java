/*
 * Copyright 2025 Red Hat, Inc.
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

package org.jboss.migration.eap.task.subsystem.metrics;

import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ValueExpression;
import org.jboss.migration.core.jboss.JBossExtensionNames;
import org.jboss.migration.core.jboss.JBossSubsystemNames;
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.wfly10.config.management.ProfileResource;
import org.jboss.migration.wfly10.config.management.SubsystemResource;
import org.jboss.migration.wfly10.config.task.management.resource.ManageableResourceBuildParameters;
import org.jboss.migration.wfly10.config.task.management.subsystem.AddSubsystemResourceSubtaskBuilder;
import org.jboss.migration.wfly10.config.task.management.subsystem.AddSubsystemResources;

/**
 * @author emmartins
 */
public class EAP8_1AddMetricsSubsystem<S> extends AddSubsystemResources<S> {

    public EAP8_1AddMetricsSubsystem() {
        super(JBossExtensionNames.METRICS, new SubtaskBuilder<>());
        // do not add subsystem config to "standalone-load-balancer.xml" config
        skipPolicyBuilders(getSkipPolicyBuilder(),
                buildParameters -> context -> buildParameters.getServerConfiguration().getConfigurationPath().getPath().endsWith("standalone-load-balancer.xml"));
    }

    static class SubtaskBuilder<S> extends AddSubsystemResourceSubtaskBuilder<S> {
        SubtaskBuilder() {
            super(JBossSubsystemNames.METRICS);
            // do not add subsystem config to profile "load-balancer"
            skipPolicyBuilder(buildParameters -> context -> buildParameters.getResource().getResourceType() == ProfileResource.RESOURCE_TYPE && buildParameters.getResource().getResourceName().equals("load-balancer"));
        }
        @Override
        protected void addConfiguration(ManageableResourceBuildParameters<S, SubsystemResource.Parent> params, TaskContext taskContext) {
            final ModelNode op = Util.createAddOperation(params.getResource().getSubsystemResourcePathAddress(getSubsystem()));
            op.get("security-enabled").set(false);
            op.get("exposed-subsystems").setEmptyList().add("*");
            op.get("prefix").set(new ValueExpression("${wildfly.metrics.prefix:jboss}"));
            params.getServerConfiguration().executeManagementOperation(op);
        }
    }
}