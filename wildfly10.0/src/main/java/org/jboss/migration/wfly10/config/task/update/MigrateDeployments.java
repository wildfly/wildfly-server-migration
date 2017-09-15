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

import org.jboss.migration.core.jboss.JBossServer;
import org.jboss.migration.core.jboss.JBossServerConfiguration;
import org.jboss.migration.wfly10.config.task.management.configuration.ManageableServerConfigurationCompositeSubtasks;
import org.jboss.migration.wfly10.config.task.management.configuration.ManageableServerConfigurationCompositeTask;

/**
 * Task which handles the migration/removal of a server configuration's deployments and overlays. When used this task can't be skipped, since any deployments found must either be migrated, or removed.
 * @author emmartins
 */
public class MigrateDeployments<S extends JBossServer<S>> extends ManageableServerConfigurationCompositeTask.Builder<JBossServerConfiguration<S>> {
    public MigrateDeployments() {
        name("deployments.migrate-deployments");
        beforeRun(context -> context.getLogger().debugf("Processing source configuration's deployments..."));
        subtasks(new ManageableServerConfigurationCompositeSubtasks.Builder<JBossServerConfiguration<S>>()
                .subtask(new MigratePersistentDeployments<>())
                .subtask(new MigrateScannerDeployments<>())
                .subtask(new MigrateDeploymentOverlays<>()));
    }
}