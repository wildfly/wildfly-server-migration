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

package org.jboss.migration.wfly.task.xml;

import org.jboss.migration.core.env.TaskEnvironment;
import org.jboss.migration.core.jboss.JBossServer;
import org.jboss.migration.core.jboss.JBossServerConfiguration;
import org.jboss.migration.core.task.ServerMigrationTask;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.core.task.component.SimpleComponentTask;
import org.jboss.migration.core.util.xml.XMLFileFilter;
import org.jboss.migration.core.util.xml.XMLFiles;
import org.jboss.migration.wfly10.config.task.ServerConfigurationMigration;

import static org.jboss.migration.core.task.component.TaskSkipPolicy.skipIfDefaultTaskSkipPropertyIsSet;

/**
 * @author emmartins
 */
public class WildFly25MigrateVault<S extends JBossServer<S>> implements ServerConfigurationMigration.XMLConfigurationSubtaskFactory<JBossServerConfiguration<S>> {

    public static final String TASK_NAME = "security.migrate-vault";

    private static final String VAULT = "vault";

    @Override
    public ServerMigrationTask getTask(final JBossServerConfiguration<S> source, final JBossServerConfiguration targetConfigurationPath) {
        return new SimpleComponentTask.Builder()
                .name(TASK_NAME)
                .skipPolicy(skipIfDefaultTaskSkipPropertyIsSet())
                .runnable(context -> {
                    context.getLogger().debugf("Searching for legacy vault XML configuration, not supported by the target server...");
                    final ServerMigrationTaskResult taskResult = processXMLConfiguration(source, targetConfigurationPath, context);
                    if (taskResult.getStatus() == ServerMigrationTaskResult.Status.SKIPPED) {
                        context.getLogger().debugf("No legacy vault XML configuration found.");
                    } else {
                        context.getLogger().infof("Legacy vault XML configuration migrated.");
                    }
                    return taskResult;
                })
                .build();
    }

    protected ServerMigrationTaskResult processXMLConfiguration(final JBossServerConfiguration<S> source, final JBossServerConfiguration targetConfigurationPath, final TaskContext context) {
        // setup and run the xml filter
        ServerMigrationTaskResult.Builder taskResultBuilder = new ServerMigrationTaskResult.Builder().skipped();
        final XMLFileFilter extensionsFilter = (startElement, xmlEventReader, xmlEventWriter, xmlEventFactory) -> {
            final String elementLocalName = startElement.getName().getLocalPart();
            if (elementLocalName.equals(VAULT) && startElement.getName().getNamespaceURI().startsWith("urn:jboss:domain:")) {
                if (new TaskEnvironment(context.getMigrationEnvironment(), context.getTaskName()).getPropertyAsBoolean("fail-if-vault-found",true)) {
                    throw new UnsupportedOperationException("The source configuration includes Vault, which migration is unsupported by the tool and needs to be done manually in advance. If you want to proceed with the migration please restart migration with environment property "+TASK_NAME+".fail-if-vault-found set as false.");
                } else {
                    taskResultBuilder.success();
                    return XMLFileFilter.Result.REMOVE;
                }
            } else {
                return XMLFileFilter.Result.NOT_APPLICABLE;
            }
        };
        XMLFiles.filter(targetConfigurationPath.getPath(), extensionsFilter);
        return taskResultBuilder.build();
    }
}