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

package org.jboss.migration.wfly10.config.task.management.subsystem;

import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.migration.core.task.ServerMigrationTaskName;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.wfly10.config.management.ProfileResource;
import org.jboss.migration.wfly10.config.management.SubsystemResource;
import org.jboss.migration.wfly10.config.task.management.resource.ManageableResourceBuildParameters;
import org.jboss.migration.wfly10.config.task.management.resource.ManageableResourceLeafTask;

/**
 * The builder for leaf tasks, which add subsystem configs.
 * @author emmartins
 */
public class AddSubsystemResourceSubtaskBuilder<S> extends ManageableResourceLeafTask.Builder<S, SubsystemResource.Parent> {

    private String subsystem;

    public AddSubsystemResourceSubtaskBuilder(String subsystem) {
        this.subsystem = subsystem;
        nameBuilder(parameters -> new ServerMigrationTaskName.Builder("subsystem."+subsystem+".add-config").addAttribute("name", parameters.getResource().getResourceAbsoluteName()).build());
        runBuilder(params -> taskContext -> {
            SubsystemResource.Parent parent = params.getResource();
            // skip if subsystem already exists in current "profile"
            if (parent.hasSubsystemResource(subsystem)) {
                taskContext.getLogger().debugf("Skipped adding subsystem config %s, already exists.", parent.getSubsystemResourceAbsoluteName(subsystem));
                return ServerMigrationTaskResult.SKIPPED;
            }
            // skip if parent is a profile that includes another profile, we only add configurations to leaf profiles (prevents CMTOOL-368)
            if (parent.getResourceType() == ProfileResource.RESOURCE_TYPE) {
                final String includes = parent.getResourceConfiguration().get("includes").asStringOrNull();
                if (includes != null)  {
                    taskContext.getLogger().warnf("Subsystem to be added on profile %s, which includes profile(s) %s, skipping...", parent.getResourceName(), includes);
                    return ServerMigrationTaskResult.SKIPPED;
                }
            }
            final String configName = parent.getSubsystemResourceAbsoluteName(subsystem);
            taskContext.getLogger().debugf("Adding subsystem config %s...", configName);
            addConfiguration(params, taskContext);
            taskContext.getLogger().debugf("Subsystem config %s added.", configName);
            return ServerMigrationTaskResult.SUCCESS;
        });
    }

    protected void addConfiguration(ManageableResourceBuildParameters<S, SubsystemResource.Parent> params, TaskContext taskContext) {
        final ModelNode op = Util.createAddOperation(params.getResource().getSubsystemResourcePathAddress(subsystem));
        params.getServerConfiguration().executeManagementOperation(op);
    }

    public String getSubsystem() {
        return subsystem;
    }
}
