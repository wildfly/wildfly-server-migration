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

package org.jboss.migration.core.jboss;

import org.jboss.migration.core.ServerMigrationFailureException;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.core.task.component.TaskRunnable;

import java.nio.file.Path;

/**
 * @author emmartins
 */
public class MigrateResolvablePathTaskRunnable implements TaskRunnable {

    private final ResolvablePath path;
    private final JBossServerConfiguration sourceConfiguration;
    private final JBossServerConfiguration targetConfiguration;

    public MigrateResolvablePathTaskRunnable(ResolvablePath path, JBossServerConfiguration sourceConfiguration, JBossServerConfiguration targetConfiguration) {
        this.path = path;
        this.sourceConfiguration = sourceConfiguration;
        this.targetConfiguration = targetConfiguration;
    }

    @Override
    public ServerMigrationTaskResult run(TaskContext context) {
        // only migrate path to target server if it's relative
        if (path.getRelativeTo() != null) {
            Path sourcePath = sourceConfiguration.resolvePath(path);
            Path targetPath = targetConfiguration.resolvePath(path);
            if (sourcePath == null) {
                if (targetPath == null) {
                    throw new ServerMigrationFailureException("Failed to resolve path "+path);
                } else {
                    if (targetPath.startsWith(targetConfiguration.getServer().getBaseDir())) {
                        // relativize for source base dir
                        sourcePath = sourceConfiguration.getServer().getBaseDir().resolve(targetConfiguration.getServer().getBaseDir().relativize(targetPath));
                    } else {
                        sourcePath = targetPath;
                    }
                }
            } else {
                if (targetPath == null) {
                    if (sourcePath.startsWith(sourceConfiguration.getServer().getBaseDir())) {
                        // relativize for target base dir
                        targetPath = targetConfiguration.getServer().getBaseDir().resolve(sourceConfiguration.getServer().getBaseDir().relativize(sourcePath));
                    } else {
                        targetPath = sourcePath;
                    }
                }
            }
            return new CopyPath(sourcePath, targetPath).run(context);
        } else {
            context.getLogger().warnf("Skipping migration of path '%s', not a relative path!", path);
            return ServerMigrationTaskResult.SKIPPED;
        }
    }
}
