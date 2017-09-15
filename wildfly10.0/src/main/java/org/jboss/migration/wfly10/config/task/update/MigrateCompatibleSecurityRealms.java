/*
 * Copyright 2016 Red Hat, Inc.
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
import org.jboss.migration.core.jboss.JBossServer;
import org.jboss.migration.core.jboss.JBossServerConfiguration;
import org.jboss.migration.core.jboss.MigrateResolvablePathTaskRunnable;
import org.jboss.migration.core.jboss.ResolvablePath;
import org.jboss.migration.core.task.ServerMigrationTaskName;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.core.task.component.TaskSkipPolicy;
import org.jboss.migration.wfly10.config.management.SecurityRealmResource;
import org.jboss.migration.wfly10.config.task.management.configuration.ManageableServerConfigurationCompositeTask;
import org.jboss.migration.wfly10.config.task.management.resource.ManageableResourceCompositeSubtasks;
import org.jboss.migration.wfly10.config.task.management.resource.ManageableResourceLeafTask;
import org.jboss.migration.wfly10.config.task.management.resource.ManageableResourceTaskRunnableBuilder;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * Migration of security realms fully compatible with WildFly 10.
 * @author emmartins
 */
public class MigrateCompatibleSecurityRealms<S extends JBossServer<S>> extends ManageableServerConfigurationCompositeTask.Builder<JBossServerConfiguration<S>> {

    public MigrateCompatibleSecurityRealms() {
        name("security-realms.migrate-properties");
        skipPolicy(TaskSkipPolicy.skipIfDefaultTaskSkipPropertyIsSet());
        beforeRun(context -> context.getLogger().infof("Migrating security realms..."));
        subtasks(SecurityRealmResource.class, ManageableResourceCompositeSubtasks.of(new Subtask<>()));
        afterRun(context -> context.getLogger().debugf("Security realms migration done."));

    }

    protected static class Subtask<S extends JBossServer<S>> extends ManageableResourceLeafTask.Builder<JBossServerConfiguration<S>, SecurityRealmResource> {
        protected Subtask() {
            nameBuilder(parameters -> new ServerMigrationTaskName.Builder("security-realm."+parameters.getResource().getResourceName()+".migrate-properties").build());
            final ManageableResourceTaskRunnableBuilder<JBossServerConfiguration<S>, SecurityRealmResource> runnableBuilder = params -> context -> {
                final SecurityRealmResource securityRealmResource = params.getResource();
                final String securityRealmConfigName = securityRealmResource.getResourceAbsoluteName();
                context.getLogger().debugf("Security realm %s migration starting...", securityRealmConfigName);
                final ModelNode securityRealmConfig = securityRealmResource.getResourceConfiguration();
                if (securityRealmConfig.hasDefined(AUTHENTICATION, PROPERTIES)) {
                    copyPropertiesFile(AUTHENTICATION, securityRealmConfig, params.getSource(), securityRealmResource, context);
                }
                if (securityRealmConfig.hasDefined(AUTHORIZATION, PROPERTIES)) {
                    copyPropertiesFile(AUTHORIZATION, securityRealmConfig, params.getSource(), securityRealmResource, context);
                }
                context.getLogger().infof("Security realm %s migrated.", securityRealmConfigName);
                return ServerMigrationTaskResult.SUCCESS;
            };
            runBuilder(runnableBuilder);
        }

        private void copyPropertiesFile(String propertiesName, ModelNode securityRealmConfig, JBossServerConfiguration<S> source, SecurityRealmResource securityRealmResource, TaskContext context) throws ServerMigrationFailureException {
            final ModelNode properties = securityRealmConfig.get(propertiesName, PROPERTIES);
            if (properties.hasDefined(PATH)) {
                new MigrateResolvablePathTaskRunnable(new ResolvablePath(properties), source, securityRealmResource.getServerConfiguration().getConfigurationPath()).run(context);
            }
        }
    }
}