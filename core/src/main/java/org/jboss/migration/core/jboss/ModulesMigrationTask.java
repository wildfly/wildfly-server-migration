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

package org.jboss.migration.core.jboss;

import org.jboss.migration.core.env.MigrationEnvironment;
import org.jboss.migration.core.env.TaskEnvironment;
import org.jboss.migration.core.task.ServerMigrationTask;
import org.jboss.migration.core.task.ServerMigrationTaskName;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.TaskContext;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author emmartins
 */
public class ModulesMigrationTask implements ServerMigrationTask {

    public static final String ENVIRONMENT_PROPERTY_INCLUDES = "modules.includes";
    public static final String ENVIRONMENT_PROPERTY_EXCLUDES = "modules.excludes";

    private static final ServerMigrationTaskName TASK_NAME = new ServerMigrationTaskName.Builder("modules.migrate-modules-requested-by-user").build();

    private final JBossServer source;
    private final JBossServer target;
    private final String requestedBy;

    public ModulesMigrationTask(JBossServer source, JBossServer target) {
        this(source, target, "environment");
    }

    protected ModulesMigrationTask(JBossServer source, JBossServer target, String requestedBy) {
        this.source = source;
        this.target = target;
        this.requestedBy = requestedBy;
    }

    @Override
    public ServerMigrationTaskName getName() {
        return TASK_NAME;
    }

    @Override
    public ServerMigrationTaskResult run(TaskContext context) {
        final TaskEnvironment taskEnvironment = new TaskEnvironment(context.getMigrationEnvironment(), getName());
        if (taskEnvironment.isSkippedByEnvironment()) {
            return ServerMigrationTaskResult.SKIPPED;
        }
        final ModuleMigrator moduleMigrator = new ModuleMigrator(source, target, context.getMigrationEnvironment());
        migrateModules(moduleMigrator, context);
        if (context.hasSucessfulSubtasks()) {
            return ServerMigrationTaskResult.SUCCESS;
        } else {
            context.getLogger().debugf("No modules required migration.", requestedBy);
            return ServerMigrationTaskResult.SKIPPED;
        }
    }

    protected void migrateModules(ModuleMigrator moduleMigrator, TaskContext context) {
        context.getConsoleWrapper().println();
        context.getLogger().infof("--- Migrating modules requested by environment...");
        context.getConsoleWrapper().println();
        final List<String> includedModules = context.getMigrationEnvironment().getPropertyAsList(ENVIRONMENT_PROPERTY_INCLUDES, Collections.emptyList());
        if (includedModules.isEmpty()) {
            context.getLogger().infof("No modules to migrate.", requestedBy);
            return;
        }
        for (String module : includedModules) {
            moduleMigrator.migrateModule(module, "requested by environment", context);
        }
    }

    public static class ModuleMigrator {

        private final JBossServer sourceServer;
        private final JBossServer targetServer;
        private final JBossServer.Modules sourceModules;
        private final JBossServer.Modules targetModules;
        private final Set<ModuleIdentifier> excludedByEnvironment;

        protected ModuleMigrator(JBossServer source, JBossServer target, MigrationEnvironment environment) {
            this.sourceServer = source;
            this.targetServer = target;
            this.sourceModules = source.getModules();
            this.targetModules = target.getModules();
            this.excludedByEnvironment = new HashSet<>();
            for (String excludedModule : environment.getPropertyAsList(ENVIRONMENT_PROPERTY_EXCLUDES, Collections.emptyList())) {
                this.excludedByEnvironment.add(ModuleIdentifier.fromString(excludedModule));
            }
        }

        public JBossServer getSourceServer() {
            return sourceServer;
        }

        public JBossServer getTargetServer() {
            return targetServer;
        }

        public void migrateModule(String moduleId, String reason, final TaskContext context) {
            migrateModule(ModuleIdentifier.fromString(moduleId), reason, context);
        }

        public void migrateModule(final ModuleIdentifier moduleIdentifier, final String reason, final TaskContext context) throws IllegalStateException {
            if (excludedByEnvironment.contains(moduleIdentifier)) {
                context.getLogger().debugf("Skipping module %s migration, it's excluded by environment.", moduleIdentifier);
                return;
            }
            if (targetModules.isInternalModule(moduleIdentifier)) {
                context.getLogger().debugf("Skipping module %s migration, it's an internal target server's module.", moduleIdentifier);
                return;
            }
            if (targetModules.getModule(moduleIdentifier) != null) {
                context.getLogger().debugf("Skipping module %s migration, already exists in target.", moduleIdentifier, reason);
                return;
            }
            final JBossServer.Module sourceModule = sourceModules.getModule(moduleIdentifier);
            if (sourceModule == null) {
                throw new IllegalStateException("Migration of module "+moduleIdentifier+" required, but module not found in source server.");
            }
            final ServerMigrationTaskName taskName = new ServerMigrationTaskName.Builder(context.getTaskName().getName()+".migrate-module").addAttribute("id", moduleIdentifier.toString()).build();
            final ServerMigrationTask subtask = new ServerMigrationTask() {
                @Override
                public ServerMigrationTaskName getName() {
                    return taskName;
                }

                @Override
                public ServerMigrationTaskResult run(TaskContext context) {
                    context.getMigrationFiles().copy(sourceModule.getModuleDir(), targetModules.getModuleDir(moduleIdentifier));
                    context.getLogger().infof("Module %s migrated.", moduleIdentifier);
                    return new ServerMigrationTaskResult.Builder()
                            .success()
                            .addAttribute("reason", reason)
                            .build();
                }
            };
            context.execute(subtask);
            for (ModuleSpecification.Dependency dependency : sourceModule.getModuleSpecification().getDependencies()) {
                migrateModule(dependency.getId(), "migrated module " + moduleIdentifier + " depends on it", context);
            }
        }
    }
}
