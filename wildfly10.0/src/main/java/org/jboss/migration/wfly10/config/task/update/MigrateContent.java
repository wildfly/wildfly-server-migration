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

import org.jboss.migration.core.jboss.ContentHashToPathMapper;
import org.jboss.migration.core.jboss.JBossServerConfiguration;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.core.task.component.TaskRunnable;

import java.nio.file.Path;

/**
 * @author emmartins
 */
public class MigrateContent implements TaskRunnable {

    private final byte[] contentHash;
    private final JBossServerConfiguration sourceConfiguration;
    private final JBossServerConfiguration targetConfiguration;

    public MigrateContent(byte[] contentHash, JBossServerConfiguration sourceConfiguration, JBossServerConfiguration targetConfiguration) {
        this.contentHash = contentHash;
        this.sourceConfiguration = sourceConfiguration;
        this.targetConfiguration = targetConfiguration;
    }

    @Override
    public ServerMigrationTaskResult run(TaskContext context) {
        final Path contentPath = new ContentHashToPathMapper().apply(contentHash);
        final Path contentSource = sourceConfiguration.getContentDir().resolve(contentPath);
        context.getLogger().infof("Source content's path: %s", contentSource);
        final Path contentTarget = targetConfiguration.getContentDir().resolve(contentPath);
        context.getLogger().infof("Target content's path: %s", contentTarget);
        if (!contentSource.equals(contentTarget)) {
            context.getMigrationFiles().copy(contentSource, contentTarget);
            context.getLogger().infof("Source's content %s migrated to %s.", contentSource, contentTarget);
            return ServerMigrationTaskResult.SUCCESS;
        } else {
            context.getLogger().infof("Source equals target content path, skipping content migration");
            return  ServerMigrationTaskResult.SKIPPED;
        }
    }
}
