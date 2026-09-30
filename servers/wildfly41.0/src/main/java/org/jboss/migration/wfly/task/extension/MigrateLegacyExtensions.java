/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.wfly.task.extension;

import org.jboss.migration.core.jboss.Extension;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.component.TaskSkipPolicy;
import org.jboss.migration.wfly10.config.task.management.configuration.ManageableServerConfigurationLeafTask;

import java.util.Collection;

/**
 * A task that iterates over the target server's legacy extensions and adds a
 * {@link MigrateLegacyExtension} subtask for each one. This task should be registered as a
 * subtask (before {@code MigrateDeployments}) in both standalone and domain configuration
 * builders of every migration that auto-discovers supported extensions.
 *
 * @author emmartins
 */
public class MigrateLegacyExtensions<S> extends ManageableServerConfigurationLeafTask.Builder<S> {

    public static final String TASK_NAME = "extensions.migrate-legacy-extensions";

    public MigrateLegacyExtensions() {
        name(TASK_NAME);
        skipPolicy(TaskSkipPolicy.skipIfDefaultTaskSkipPropertyIsSet());
        runBuilder(params -> context -> {
            final S source = params.getSource();
            final var configuration = params.getServerConfiguration();
            final Collection<Extension> legacyExtensions =
                    configuration.getServer().getExtensions().getLegacyExtensions();
            if (legacyExtensions.isEmpty()) {
                context.getLogger().debug("No legacy extensions found.");
                return ServerMigrationTaskResult.SKIPPED;
            }
            context.getLogger().debug("Migrating legacy extensions...");
            for (Extension legacyExtension : legacyExtensions) {
                final MigrateLegacyExtension<S> migrateLegacyExtension = new MigrateLegacyExtension<>(legacyExtension);
                context.execute(migrateLegacyExtension.getTask(source, configuration));
            }
            if (context.hasSucessfulSubtasks()) {
                return ServerMigrationTaskResult.SUCCESS;
            } else {
                return ServerMigrationTaskResult.SKIPPED;
            }
        });
    }
}
