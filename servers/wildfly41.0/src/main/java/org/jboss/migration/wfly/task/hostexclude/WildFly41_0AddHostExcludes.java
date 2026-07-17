/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.wfly.task.hostexclude;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXCLUDED_EXTENSIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST_RELEASE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MAJOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MICRO_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MINOR_VERSION;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.migration.core.jboss.HostExclude;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.component.TaskSkipPolicy;
import org.jboss.migration.wfly.WildFly41_0Server;
import org.jboss.migration.wfly10.config.management.HostExcludeResource;
import org.jboss.migration.wfly10.config.task.management.configuration.ManageableServerConfigurationCompositeSubtasks;
import org.jboss.migration.wfly10.config.task.management.configuration.ManageableServerConfigurationCompositeTask;
import org.jboss.migration.wfly10.config.task.management.resource.ManageableResourceLeafTask;

import java.util.List;
import java.util.Set;

/**
 * @author emmartins
 */
public class WildFly41_0AddHostExcludes<S> extends ManageableServerConfigurationCompositeTask.Builder<S> {

    public WildFly41_0AddHostExcludes() {
        name("host-excludes.add");
        skipPolicies(TaskSkipPolicy.skipIfDefaultTaskSkipPropertyIsSet());
        beforeRun(context -> context.getLogger().debug("Adding host-excludes configuration..."));
        final ManageableServerConfigurationCompositeSubtasks.Builder<S> subtasksBuilder = new ManageableServerConfigurationCompositeSubtasks.Builder<>();
        // first we remove all existent
        subtasksBuilder.subtask(HostExcludeResource.Parent.class, new RemoveAllHostExclude<>());
        // then add all discovered from target server
        subtasksBuilder.subtask(HostExcludeResource.Parent.class, new AddAllHostExclude<>());
        subtasks(subtasksBuilder);
        afterRun(context -> {
            if (context.hasSucessfulSubtasks()) {
                context.getLogger().info("Host-excludes configuration added.");
            } else {
                context.getLogger().debug("Host-excludes configuration not added.");
            }
        });
    }

    private static class RemoveAllHostExclude<S> extends ManageableResourceLeafTask.Builder<S, HostExcludeResource.Parent> {
        public RemoveAllHostExclude() {
            name("host-exclude.removeAll");
            skipPolicy(TaskSkipPolicy.skipIfDefaultTaskSkipPropertyIsSet());
            beforeRun(context -> context.getLogger().debugf("Removing all legacy host-exclude configs..."));
            runBuilder(params -> taskContext -> {
                final HostExcludeResource.Parent parent = params.getResource();
                final Set<String> legacyHostExcludeNames = parent.getHostExcludeResourceNames();
                if (legacyHostExcludeNames.isEmpty()) {
                    taskContext.getLogger().debugf("No legacy host-exclude configs found.");
                    return ServerMigrationTaskResult.SKIPPED;
                }
                for (String hostExcludeName : legacyHostExcludeNames) {
                    parent.removeHostExcludeResource(hostExcludeName);
                    taskContext.getLogger().debugf("Legacy host-exclude %s found and removed.", hostExcludeName);
                }
                taskContext.getLogger().debugf("All legacy host-exclude configs removed.");
                return ServerMigrationTaskResult.SUCCESS;
            });
        }
    }

    private static class AddAllHostExclude<S> extends ManageableResourceLeafTask.Builder<S, HostExcludeResource.Parent> {
        public AddAllHostExclude() {
            name("host-exclude.addAll");
            skipPolicy(TaskSkipPolicy.skipIfDefaultTaskSkipPropertyIsSet());
            beforeRun(context -> context.getLogger().debugf("Adding all target host-exclude configs..."));
            runBuilder(params -> taskContext -> {
                final HostExcludeResource.Parent parent = params.getResource();
                final Operations.CompositeOperationBuilder compositeOperationBuilder = Operations.CompositeOperationBuilder.create();
                boolean update = false;
                for (HostExclude hostExclude : ((WildFly41_0Server) parent.getServerConfiguration().getServer()).getHostExcludes().getHostExcludes()) {
                    final String hostExcludeName = hostExclude.getName();
                    // if migrated config has an host-exclude with same name, remove it
                    if (parent.hasHostExcludeResource(hostExcludeName)) {
                        parent.removeHostExcludeResource(hostExcludeName);
                        taskContext.getLogger().debugf("Legacy host-exclude %s found and removed...", hostExclude.getName());
                    }
                    // add the host-exclude
                    final PathAddress hostExcludePathAddress = parent.getHostExcludeResourcePathAddress(hostExclude.getName());
                    final ModelNode addOp = Util.createAddOperation(hostExcludePathAddress);
                    final HostExclude.ApiVersion apiVersion = hostExclude.getApiVersion();
                    if (apiVersion != null) {
                        addOp.get(MANAGEMENT_MAJOR_VERSION).set(apiVersion.getMajorVersion());
                        addOp.get(MANAGEMENT_MINOR_VERSION).set(apiVersion.getMinorVersion());
                        if (apiVersion.getMicroVersion() != null) {
                            addOp.get(MANAGEMENT_MICRO_VERSION).set(apiVersion.getMicroVersion());
                        }
                    }
                    final HostExclude.Release hostRelease = hostExclude.getRelease();
                    if (hostRelease != null) {
                        addOp.get(HOST_RELEASE).set(hostRelease.getId());
                    }
                    final List<HostExclude.ExcludedExtension> excludedExtensions = hostExclude.getExcludedExtensions();
                    if (excludedExtensions != null && !excludedExtensions.isEmpty()) {
                        final ModelNode modelNode = addOp.get(EXCLUDED_EXTENSIONS).setEmptyList();
                        for (HostExclude.ExcludedExtension excludedExtension : excludedExtensions) {
                            modelNode.add(excludedExtension.getModule());
                        }
                    }
                    compositeOperationBuilder.addStep(addOp);
                    update = true;
                    taskContext.getLogger().debugf("Host-exclude %s added.", hostExcludeName);
                }
                if (update) {
                    params.getServerConfiguration().executeManagementOperation(compositeOperationBuilder.build().getOperation());
                }
                return ServerMigrationTaskResult.SUCCESS;
            });
        }
    }
}