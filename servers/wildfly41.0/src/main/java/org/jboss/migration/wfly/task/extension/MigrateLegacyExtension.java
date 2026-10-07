/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.wfly.task.extension;

import org.jboss.migration.core.jboss.Extension;
import org.jboss.migration.core.task.ServerMigrationTask;
import org.jboss.migration.core.task.ServerMigrationTaskName;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.component.SimpleComponentTask;
import org.jboss.migration.core.task.component.TaskSkipPolicy;
import org.jboss.migration.wfly.task.subsystem.MigrateSubsystemResources;
import org.jboss.migration.wfly10.config.management.ManageableServerConfiguration;
import org.jboss.migration.wfly10.config.task.factory.ManageableServerConfigurationTaskFactory;
import org.jboss.migration.wfly10.config.task.management.configuration.ManageableServerConfigurationBuildParametersImpl;
import org.jboss.migration.wfly10.config.task.management.configuration.ManageableServerConfigurationLeafTask;
import org.jboss.migration.wfly10.config.task.management.extension.RemoveExtensionTaskBuilder;
import org.jboss.migration.wfly10.config.task.management.resources.ManageableResourcesBuildParametersImpl;

import java.util.Collections;

/**
 * A task factory that migrates a single legacy extension: for each of its subsystems a
 * {@link MigrateSubsystemResources} subtask is added (which invokes
 * {@code migrate()} if available and then removes the subsystem config), and
 * finally a remove-extension subtask is added.
 *
 * @author emmartins
 */
public class MigrateLegacyExtension<S> implements ManageableServerConfigurationTaskFactory<S, ManageableServerConfiguration> {

    private final Extension legacyExtension;

    public MigrateLegacyExtension(Extension legacyExtension) {
        this.legacyExtension = legacyExtension;
    }

    @Override
    public ServerMigrationTask getTask(S source, ManageableServerConfiguration configuration) {
        final String extensionModule = legacyExtension.getModule();
        final ServerMigrationTaskName taskName = new ServerMigrationTaskName.Builder("extension." + extensionModule + ".migrate-legacy").build();
        return new SimpleComponentTask.Builder()
                .name(taskName)
                .skipPolicy(TaskSkipPolicy.skipIfDefaultTaskSkipPropertyIsSet())
                .runnable(context -> {
                    context.getLogger().debugf("Migrating legacy extension %s...", extensionModule);
                    // add a migrate-subsystem subtask for each subsystem of the legacy extension
                    for (String subsystemName : legacyExtension.getSubsystemNames()) {
                        final MigrateSubsystemResources<S> migrateSubsystemTask = new MigrateSubsystemResources<>(subsystemName);
                        context.execute(migrateSubsystemTask.build(
                                new ManageableResourcesBuildParametersImpl<>(source, configuration, Collections.singleton(configuration))));
                    }
                    // remove the extension after all subsystems have been migrated
                    final ManageableServerConfigurationLeafTask.Builder<S> removeExtensionTask =
                            new RemoveExtensionTaskBuilder<S>(extensionModule)
                                    .name(new ServerMigrationTaskName.Builder(taskName.getName() + ".remove-extension")
                                            .addAttribute("module", extensionModule).build());
                    context.execute(removeExtensionTask.build(
                            new ManageableServerConfigurationBuildParametersImpl<>(source, configuration)));
                    if (context.hasSucessfulSubtasks()) {
                        context.getLogger().debugf("Legacy extension %s migrated.", extensionModule);
                        return ServerMigrationTaskResult.SUCCESS;
                    } else {
                        return ServerMigrationTaskResult.SKIPPED;
                    }
                })
                .build();
    }
}
