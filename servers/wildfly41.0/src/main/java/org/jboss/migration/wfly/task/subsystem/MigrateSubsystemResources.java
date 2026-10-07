/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.wfly.task.subsystem;

import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.migration.core.ServerMigrationFailureException;
import org.jboss.migration.core.task.ServerMigrationTaskName;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.core.task.component.TaskSkipPolicy;
import org.jboss.migration.wfly10.config.management.ManageableResource;
import org.jboss.migration.wfly10.config.management.SubsystemResource;
import org.jboss.migration.wfly10.config.task.management.resource.ManageableResourceLeafTask;
import org.jboss.migration.wfly10.config.task.management.resources.ManageableResourcesCompositeSubtasks;
import org.jboss.migration.wfly10.config.task.management.resources.ManageableResourcesCompositeTask;

import java.io.IOException;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

/**
 * Migrates subsystem resources for a legacy extension: invokes the subsystem {@code migrate()} operation if
 * it exists, then enforces removal of the subsystem configuration. Unlike the original
 * {@link org.jboss.migration.wfly10.config.task.management.subsystem.MigrateSubsystemResources}, this task
 * does <em>not</em> remove the extension.
 *
 * @author emmartins
 */
public class MigrateSubsystemResources<S> extends ManageableResourcesCompositeTask.Builder<S, ManageableResource> {

    public MigrateSubsystemResources(String subsystem) {
        final ServerMigrationTaskName taskName = new ServerMigrationTaskName.Builder("subsystem." + subsystem + ".migrate").build();
        name(taskName);
        skipPolicy(TaskSkipPolicy.skipIfDefaultTaskSkipPropertyIsSet());
        beforeRun(context -> context.getLogger().debugf("Migrating subsystem %s...", subsystem));
        final ManageableResourceLeafTask.Builder<S, SubsystemResource> migrateConfigSubtask =
                new ManageableResourceLeafTask.Builder<S, SubsystemResource>()
                        .nameBuilder(parameters -> new ServerMigrationTaskName.Builder(taskName.getName() + ".migrate-config")
                                .addAttribute("name", parameters.getResource().getResourceAbsoluteName()).build())
                        .runBuilder(params -> context -> migrateSubsystem(params.getResource(), context));
        subtasks(new ManageableResourcesCompositeSubtasks.Builder<S, ManageableResource>()
                .subtask(SubsystemResource.class, subsystem, migrateConfigSubtask));
        afterRun(context -> {
            if (context.hasSucessfulSubtasks()) {
                context.getLogger().debugf("Legacy subsystem %s migrated.", subsystem);
            }
        });
    }

    private static ServerMigrationTaskResult migrateSubsystem(SubsystemResource subsystemResource, TaskContext taskContext) {
        final String configName = subsystemResource.getResourceAbsoluteName();
        taskContext.getLogger().debugf("Migrating legacy subsystem config %s...", configName);
        try {
            if (hasMigrateOperation(subsystemResource)) {
                invokeMigrateOperation(subsystemResource, configName, taskContext);
            } else {
                taskContext.getLogger().debugf("Legacy subsystem config %s has no migrate() operation, skipping.", configName);
            }
        } finally {
            // always remove the subsystem configuration regardless of whether migrate() was invoked
            if (subsystemResource.getResourceConfiguration() != null) {
                subsystemResource.getParentResource().removeChildResource(SubsystemResource.RESOURCE_TYPE, subsystemResource.getResourceName());
                taskContext.getLogger().infof("Legacy subsystem config %s removed.", configName);
            }
        }
        return ServerMigrationTaskResult.SUCCESS;
    }

    /**
     * Returns {@code true} if the subsystem resource exposes a {@code migrate} operation.
     */
    private static boolean hasMigrateOperation(SubsystemResource subsystemResource) {
        try {
            final ModelNode readOp = Util.createEmptyOperation(READ_OPERATION_NAMES_OPERATION, subsystemResource.getResourcePathAddress());
            final ModelNode result = subsystemResource.getServerConfiguration().getModelControllerClient().execute(readOp);
            if (SUCCESS.equals(result.get(OUTCOME).asString()) && result.hasDefined(RESULT)) {
                for (ModelNode name : result.get(RESULT).asList()) {
                    if ("migrate".equals(name.asString())) {
                        return true;
                    }
                }
            }
            return false;
        } catch (IOException e) {
            throw new ServerMigrationFailureException("Failed to read operation names for subsystem " + subsystemResource.getResourceAbsoluteName(), e);
        }
    }

    /**
     * Invokes the {@code migrate} operation on the subsystem and logs any warnings.
     * Throws {@link ServerMigrationFailureException} if the operation fails.
     */
    private static void invokeMigrateOperation(SubsystemResource subsystemResource, String configName, TaskContext taskContext) {
        final ModelNode op = Util.createEmptyOperation("migrate", subsystemResource.getResourcePathAddress());
        final ModelNode result;
        try {
            result = subsystemResource.getServerConfiguration().getModelControllerClient().execute(op);
        } catch (IOException e) {
            throw new ServerMigrationFailureException("Legacy subsystem config " + configName + " migration failed", e);
        }
        taskContext.getLogger().debugf("Migration op result: %s", result.asString());
        if (!SUCCESS.equals(result.get(OUTCOME).asString())) {
            final String description = result.hasDefined("migration-error") ? result.get("migration-error").asString()
                    : result.get("failure-description").asString();
            throw new ServerMigrationFailureException("Subsystem config " + configName + " migration failed: " + description);
        }
        if (result.get(RESULT).hasDefined("migration-warnings")) {
            for (ModelNode warning : result.get(RESULT).get("migration-warnings").asList()) {
                taskContext.getLogger().warnf("Subsystem config %s migration warning: %s", configName, warning.asString());
            }
        }
        taskContext.getLogger().infof("Legacy subsystem config %s migrated.", configName);
    }
}
