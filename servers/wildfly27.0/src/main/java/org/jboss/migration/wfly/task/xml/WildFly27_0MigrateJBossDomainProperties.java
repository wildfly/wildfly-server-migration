/*
 * Copyright 2022 Red Hat, Inc.
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

package org.jboss.migration.wfly.task.xml;

import org.jboss.migration.core.ServerMigrationFailureException;
import org.jboss.migration.core.jboss.JBossServer;
import org.jboss.migration.core.jboss.JBossServerConfiguration;
import org.jboss.migration.core.task.ServerMigrationTask;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.core.task.component.SimpleComponentTask;
import org.jboss.migration.core.util.TextFiles;
import org.jboss.migration.wfly10.config.task.ServerConfigurationMigration;

import static org.jboss.migration.core.task.component.TaskSkipPolicy.skipIfDefaultTaskSkipPropertyIsSet;

/**
 * @author emmartins
 */
public class WildFly27_0MigrateJBossDomainProperties<S extends JBossServer<S>> implements ServerConfigurationMigration.XMLConfigurationSubtaskFactory<JBossServerConfiguration<S>> {

    public static final String TASK_NAME = "properties.migrate-jboss-domain-properties";

    @Override
    public ServerMigrationTask getTask(final JBossServerConfiguration<S> source, final JBossServerConfiguration targetConfigurationPath) {
        return new SimpleComponentTask.Builder()
                .name(TASK_NAME)
                .skipPolicy(skipIfDefaultTaskSkipPropertyIsSet())
                .runnable(context -> {
                    context.getLogger().debugf("Migrating JBoss domain properties...");
                    try {
                        final String originalFileAsString = TextFiles.read(targetConfigurationPath.getPath());
                        String updatedFileAsString = renameProperty(originalFileAsString,"jboss.domain.master.address", "jboss.domain.primary.address", context);
                        updatedFileAsString = renameProperty(updatedFileAsString,"jboss.domain.slave.address", "jboss.domain.secondary.address", context);
                        updatedFileAsString = renameProperty(updatedFileAsString,"jboss.domain.master.port", "jboss.domain.primary.port", context);
                        updatedFileAsString = renameProperty(updatedFileAsString,"jboss.domain.slave.port", "jboss.domain.secondary.port", context);
                        updatedFileAsString = renameProperty(updatedFileAsString,"jboss.domain.master.protocol", "jboss.domain.primary.protocol", context);
                        updatedFileAsString = renameProperty(updatedFileAsString,"jboss.domain.slave.protocol", "+jboss.domain.secondary.protocol", context);
                        if (!originalFileAsString.equals(updatedFileAsString)) {
                            TextFiles.write(targetConfigurationPath.getPath(), updatedFileAsString);
                            context.getLogger().infof("JBoss domain properties migrated.");
                            return ServerMigrationTaskResult.SUCCESS;
                        } else {
                            context.getLogger().debugf("No JBoss domain properties found to migrate.");
                            return ServerMigrationTaskResult.SKIPPED;
                        }
                    } catch (Exception e) {
                        throw new ServerMigrationFailureException(e);
                    }
                }).build();
    }

    private static String renameProperty(final String fileAsString, final String sourcePropertyName, final String targetPropertyName, TaskContext context) {
        final String updatedFileAsString = fileAsString.replaceAll(sourcePropertyName, targetPropertyName);
        if (!fileAsString.equals(updatedFileAsString)) {
            context.getLogger().infof("JBoss domain property %s migrated to %s", sourcePropertyName, targetPropertyName);
        }
        return updatedFileAsString;
    }
}