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
import org.jboss.migration.core.jboss.DeploymentOverlayLinkMatcher;
import org.jboss.migration.core.jboss.JBossServer;
import org.jboss.migration.core.jboss.JBossServerConfiguration;
import org.jboss.migration.core.task.ServerMigrationTaskName;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.component.TaskRunnable;
import org.jboss.migration.wfly10.config.management.DeploymentOverlayResource;
import org.jboss.migration.wfly10.config.management.DeploymentResource;
import org.jboss.migration.wfly10.config.management.HostControllerConfiguration;
import org.jboss.migration.wfly10.config.management.ServerGroupResource;
import org.jboss.migration.wfly10.config.task.management.configuration.ManageableServerConfigurationBuildParameters;
import org.jboss.migration.wfly10.config.task.management.configuration.ManageableServerConfigurationCompositeTask;
import org.jboss.migration.wfly10.config.task.management.configuration.ManageableServerConfigurationLeafTask;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.migration.wfly10.config.management.ManageableResourceSelectors.selectResources;

/**
 * Task which handles the migration/removal of a server configuration's deployment overlays.  When used this task can't be skipped, since any deployments found must either be migrated, or removed.
 * @author emmartins
 */
public class MigrateDeploymentOverlays<S extends JBossServer<S>> extends ManageableServerConfigurationCompositeTask.Builder<JBossServerConfiguration<S>> {

    public MigrateDeploymentOverlays() {
        name("deployments.overlays.migrate");
        runBuilder(params -> context -> {
            context.getLogger().debugf("Retrieving the configuration's deployment overlays...");
            // only deployment overlay resources which are direct children of the server config, so it doesn't end up handling here domain server groups' deployment overlays
            final List<DeploymentOverlayResource> overlays = params.getServerConfiguration().getChildResources(DeploymentOverlayResource.RESOURCE_TYPE);
            if (overlays.isEmpty()) {
                context.getLogger().debugf("No deployment overlays found.");
                return ServerMigrationTaskResult.SKIPPED;
            } else {
                context.getLogger().infof("Deployment overlays found: %s", overlays.stream().map(overlay -> overlay.getResourceName()).collect(toList()));
                // migrate only overlays linked to migrated deployments
                final DeploymentOverlayLinkMatcher matcher = new DeploymentOverlayLinkMatcher();
                for (DeploymentOverlayResource overlay : overlays) {
                    boolean migrateResource = false;
                    final String[] deploymentLinks = overlay.getDeploymentLinks();
                    final List<DeploymentResource> deployments = params.getServerConfiguration().getChildResources(DeploymentResource.RESOURCE_TYPE);
                    for (DeploymentResource deployment : deployments) {
                        if (matcher.matches(deployment.getResourceName(), deploymentLinks)) {
                            context.getLogger().infof("Migrating deployment overlay '%s', it's linked to deployment %s", overlay.getResourceName(), deployment.getResourceName());
                            migrateResource = true;
                            break;
                        }
                    }
                    // until deployment overlays missing content don't fail to boot server we first copy all content, and then filter
                    //final ManageableServerConfigurationLeafTask.Builder<JBossServerConfigurationPath<S>> subtaskBuilder = migrateResource ? new MigrateResourceSubtask<>(overlay) : new RemoveResourceSubtask<>(overlay);
                    if (!migrateResource) {
                        final ManageableServerConfigurationLeafTask.Builder<JBossServerConfiguration<S>> subtaskBuilder = new RemoveResourceSubtask<>(overlay);
                        context.execute(subtaskBuilder.build(params));
                    }
                }
                return context.hasSucessfulSubtasks() ? ServerMigrationTaskResult.SUCCESS : ServerMigrationTaskResult.SKIPPED;
            }
        });
    }

    public static class MigrateResourceSubtask<S extends JBossServer<S>> extends ManageableServerConfigurationLeafTask.Builder<JBossServerConfiguration<S>> {
        protected MigrateResourceSubtask(DeploymentOverlayResource resource) {
            nameBuilder(parameters -> new ServerMigrationTaskName.Builder("deployments.overlay."+resource.getResourceName()+".migrate").addAttribute("resource", resource.getResourceAbsoluteName()).build());
            final TaskRunnable.Builder<ManageableServerConfigurationBuildParameters<JBossServerConfiguration<S>>> runnableBuilder = params -> context -> {
                final ModelNode resourceConfig = resource.getResourceConfiguration();
                if (!resourceConfig.hasDefined(CONTENT)) {
                    throw new ServerMigrationFailureException("Unexpected deployment overlay "+resource.getResourceName()+" configuration: "+resourceConfig.asString());
                }
                for (ModelNode content : resourceConfig.get(CONTENT).asList()) {
                    /* FIXME needs to be reworked
                    if (content.hasDefined(CONTENT)) {
                        // some servers ha
                        new MigrateContent(content.get(CONTENT).asBytes(), params.getSource(), params.getServerConfiguration()).run(context);
                    } else if (content.hasDefined(HASH)) {
                        new MigrateContent(content.get(HASH).asBytes(), params.getSource(), params.getServerConfiguration()).run(context);
                    } else if (content.hasDefined(PATH)) {
                        new MigrateResolvablePath(new ResolvablePath(content), params.getSource(), params.getServerConfiguration()).run(context);
                    } else {
                        throw new ServerMigrationFailureException("Unexpected deployment overlay "+resource.getResourceName()+" content: "+content.asString());
                    }
                    */
                }
                return ServerMigrationTaskResult.SUCCESS;
            };
            runBuilder(runnableBuilder);
        }
    }

    public static class RemoveResourceSubtask<S extends JBossServer<S>> extends ManageableServerConfigurationLeafTask.Builder<JBossServerConfiguration<S>> {
        protected RemoveResourceSubtask(DeploymentOverlayResource resource) {
            nameBuilder(parameters -> new ServerMigrationTaskName.Builder("deployments.overlay."+resource.getResourceName()+".remove").addAttribute("resource", resource.getResourceAbsoluteName()).build());
            final TaskRunnable.Builder<ManageableServerConfigurationBuildParameters<JBossServerConfiguration<S>>> runnableBuilder = params -> context -> {
                if (params.getServerConfiguration() instanceof HostControllerConfiguration) {
                    // the resource may be referenced in server groups, remove these first
                    for (DeploymentOverlayResource serverGroupDeploymentResource : selectResources(ServerGroupResource.class).andThen(selectResources(DeploymentOverlayResource.class, resource.getResourceName())).fromResources(params.getServerConfiguration())) {
                        serverGroupDeploymentResource.removeResource();
                        context.getLogger().infof("Removed deployment overlay from server group %s", resource.getResourceAbsoluteName());
                    }
                }
                resource.removeResource();
                context.getLogger().infof("Removed deployment overlay from configuration %s", resource.getResourceAbsoluteName());
                return ServerMigrationTaskResult.SUCCESS;
            };
            runBuilder(runnableBuilder);
        }
    }
}