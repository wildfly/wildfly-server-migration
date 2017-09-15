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

package org.jboss.migration.wfly10.config.task.update;

import org.jboss.dmr.ModelNode;
import org.jboss.migration.core.ServerMigrationFailureException;
import org.jboss.migration.core.console.BasicResultHandlers;
import org.jboss.migration.core.console.UserConfirmation;
import org.jboss.migration.core.env.MigrationEnvironment;
import org.jboss.migration.core.env.TaskEnvironment;
import org.jboss.migration.core.jboss.JBossServer;
import org.jboss.migration.core.jboss.JBossServerConfiguration;
import org.jboss.migration.core.jboss.MigrateResolvablePathTaskRunnable;
import org.jboss.migration.core.jboss.ResolvablePath;
import org.jboss.migration.core.task.ServerMigrationTaskName;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.component.TaskRunnable;
import org.jboss.migration.wfly10.config.management.DeploymentResource;
import org.jboss.migration.wfly10.config.management.HostControllerConfiguration;
import org.jboss.migration.wfly10.config.management.ServerGroupResource;
import org.jboss.migration.wfly10.config.task.management.configuration.ManageableServerConfigurationBuildParameters;
import org.jboss.migration.wfly10.config.task.management.configuration.ManageableServerConfigurationCompositeTask;
import org.jboss.migration.wfly10.config.task.management.configuration.ManageableServerConfigurationLeafTask;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.migration.core.console.BasicResultHandlers.UserConfirmation.Result.NO;
import static org.jboss.migration.core.console.BasicResultHandlers.UserConfirmation.Result.YES;
import static org.jboss.migration.wfly10.config.management.ManageableResourceSelectors.selectResources;

/**
 * Task which handles the migration/removal of a server configuration's persisted deployments.  When used this task can't be skipped, since any deployments found must either be migrated, or removed.
 * @author emmartins
 */
public class MigratePersistentDeployments<S extends JBossServer<S>> extends ManageableServerConfigurationCompositeTask.Builder<JBossServerConfiguration<S>> {

    public MigratePersistentDeployments() {
        name("deployments.migrate-persistent-deployments");
        runBuilder(params -> context -> {
            context.getLogger().debugf("Retrieving the configuration's persistent deployments...");
            // FIXME only deployment resources which are direct children of the server config
            final List<DeploymentResource> deploymentResources = params.getServerConfiguration().getChildResources(DeploymentResource.RESOURCE_TYPE);
            if (deploymentResources.isEmpty()) {
                context.getLogger().debugf("No persistent deployments found.");
                return ServerMigrationTaskResult.SKIPPED;
            } else {
                context.getLogger().infof("Persistent deployments found: %s", deploymentResources.stream().map(resource -> resource.getResourceName()).collect(toList()));
                // deployments are migrated if this task's is not skipped on env, skip env property is set, or if parent's skip env property is set
                final MigrationEnvironment environment = context.getMigrationEnvironment();
                boolean migrateDeployments = !(new TaskEnvironment(environment, context.getTaskName()).isSkippedByEnvironment() || new TaskEnvironment(environment, context.getParentTask().getTaskName()).isSkippedByEnvironment());
                boolean confirmEachDeployment = false;
                // confirm deployments migration if environment does not skip it, and migration is interactive
                if (context.isInteractive()) {
                    final BasicResultHandlers.UserConfirmation migrateUserConfirmation = new BasicResultHandlers.UserConfirmation();
                    new UserConfirmation(context.getConsoleWrapper(), "This tool is not able to assert if persistent deployments found are compatible with the target server, skip persistent deployments migration?","yes/no?", migrateUserConfirmation).execute();
                    migrateDeployments = migrateUserConfirmation.getResult() == NO;
                    if (migrateDeployments && deploymentResources.size() > 1) {
                        final BasicResultHandlers.UserConfirmation userConfirmation = new BasicResultHandlers.UserConfirmation();
                        new UserConfirmation(context.getConsoleWrapper(), "Migrate all persistent deployments found?", "yes/no?", userConfirmation).execute();
                        confirmEachDeployment = userConfirmation.getResult() == NO;
                    }
                }
                // execute subtasks
                for (DeploymentResource deploymentResource : deploymentResources) {
                    final boolean migrateDeployment;
                    if (confirmEachDeployment) {
                        final BasicResultHandlers.UserConfirmation userConfirmation = new BasicResultHandlers.UserConfirmation();
                        new UserConfirmation(context.getConsoleWrapper(), "Migrate persistent deployment '"+deploymentResource.getResourceName()+"'?","yes/no?", userConfirmation).execute();
                        migrateDeployment = userConfirmation.getResult() == YES;
                    } else {
                        migrateDeployment = migrateDeployments;
                    }
                    // until deployment overlays missing content don't fail to boot server we first copy all content, and then filter
                    //final ManageableServerConfigurationLeafTask.Builder<JBossServerConfigurationPath<S>> subtaskBuilder = migrateDeployment ? new MigrateDeploymentSubtask<>(deploymentResource) : new RemoveDeploymentSubtask<>(deploymentResource);
                    if (!migrateDeployment) {
                        final ManageableServerConfigurationLeafTask.Builder<JBossServerConfiguration<S>> subtaskBuilder = new RemoveDeploymentSubtask<>(deploymentResource);
                        context.execute(subtaskBuilder.build(params));
                    }
                }
                return context.hasSucessfulSubtasks() ? ServerMigrationTaskResult.SUCCESS : ServerMigrationTaskResult.SKIPPED;
            }
        });
    }

    public static class MigrateDeploymentSubtask<S extends JBossServer<S>> extends ManageableServerConfigurationLeafTask.Builder<JBossServerConfiguration<S>> {
        protected MigrateDeploymentSubtask(DeploymentResource resource) {
            nameBuilder(parameters -> new ServerMigrationTaskName.Builder("deployments.migrate-persistent-deployment").addAttribute("resource", resource.getResourceAbsoluteName()).build());
            final TaskRunnable.Builder<ManageableServerConfigurationBuildParameters<JBossServerConfiguration<S>>> runnableBuilder = params -> context -> {
                final ModelNode deploymentConfig = resource.getResourceConfiguration();
                if (!deploymentConfig.hasDefined(CONTENT)) {
                    throw new ServerMigrationFailureException("Unexpected deployment "+resource.getResourceName()+" configuration: "+deploymentConfig.asString());
                }
                for (ModelNode content : deploymentConfig.get(CONTENT).asList()) {
                    if (content.hasDefined(HASH)) {
                        new MigrateContent(content.get(HASH).asBytes(), params.getSource(), params.getServerConfiguration().getConfigurationPath()).run(context);
                    } else if (content.hasDefined(PATH)) {
                        new MigrateResolvablePathTaskRunnable(new ResolvablePath(content), params.getSource(), params.getServerConfiguration().getConfigurationPath()).run(context);
                    } else {
                        throw new ServerMigrationFailureException("Unexpected deployment "+resource.getResourceName()+" content: "+content.asString());
                    }
                }
                return ServerMigrationTaskResult.SUCCESS;
            };
            runBuilder(runnableBuilder);
        }
    }

    public static class RemoveDeploymentSubtask<S extends JBossServer<S>> extends ManageableServerConfigurationLeafTask.Builder<JBossServerConfiguration<S>> {
        protected RemoveDeploymentSubtask(DeploymentResource resource) {
            nameBuilder(parameters -> new ServerMigrationTaskName.Builder("deployments.remove-persistent-deployment").addAttribute("resource", resource.getResourceAbsoluteName()).build());
            final TaskRunnable.Builder<ManageableServerConfigurationBuildParameters<JBossServerConfiguration<S>>> runnableBuilder = params -> context -> {
                if (params.getServerConfiguration() instanceof HostControllerConfiguration) {
                    // the deployment may be referenced in server groups, remove these first
                    for (DeploymentResource serverGroupDeploymentResource : selectResources(ServerGroupResource.class).andThen(selectResources(DeploymentResource.class, resource.getResourceName())).fromResources(params.getServerConfiguration())) {
                        serverGroupDeploymentResource.removeResource();
                        context.getLogger().infof("Removed persistent deployment from server group %s", resource.getResourceAbsoluteName());
                    }
                }
                resource.removeResource();
                context.getLogger().infof("Removed persistent deployment from configuration %s", resource.getResourceAbsoluteName());
                return ServerMigrationTaskResult.SUCCESS;
            };
            runBuilder(runnableBuilder);
        }
    }
}