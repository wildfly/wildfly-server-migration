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

package org.jboss.migration.wfly10.config.task.paths;

import org.jboss.migration.core.jboss.JBossServer;
import org.jboss.migration.core.jboss.JBossServerConfiguration;
import org.jboss.migration.core.jboss.XmlConfigurationMigration;
import org.jboss.migration.core.task.ServerMigrationTask;
import org.jboss.migration.core.task.ServerMigrationTaskName;
import org.jboss.migration.core.task.component.SimpleComponentTask;
import org.jboss.migration.wfly10.config.task.ServerConfigurationMigration;

/**
 * @author emmartins
 */
public class ConfigurationPathsMigrationTaskFactory<S extends JBossServer<S>> implements ServerConfigurationMigration.XMLConfigurationSubtaskFactory<JBossServerConfiguration<S>> {

    private final XmlConfigurationMigration.Builder<S> runnableBuilder;

    protected ConfigurationPathsMigrationTaskFactory(XmlConfigurationMigration.Builder<S> runnableBuilder) {
        this.runnableBuilder = runnableBuilder;
    }

    @Override
    public ServerMigrationTask getTask(final JBossServerConfiguration<S> sourceConfiguration, final JBossServerConfiguration targetConfiguration) {
        return new SimpleComponentTask.Builder()
                .name(new ServerMigrationTaskName.Builder("paths.migrate-paths-requested-by-configuration").addAttribute("path", targetConfiguration.getPath().toString()).build())
                .beforeRun(context -> context.getLogger().debugf("Migrating paths requested by configuration..."))
                .runnable(runnableBuilder.build(sourceConfiguration, targetConfiguration))
                .afterRun(context -> context.getLogger().debugf("Migration of paths requested by configuration complete."))
                .build();
    }
}
