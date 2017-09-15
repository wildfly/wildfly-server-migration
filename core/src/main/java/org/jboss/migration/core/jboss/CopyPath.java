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

import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.core.task.component.TaskRunnable;

import java.nio.file.Path;

/**
 * @author emmartins
 */
public class CopyPath implements TaskRunnable {

    private final Path sourcePath;
    private final Path targetPath;

    public CopyPath(Path sourcePath, Path targetPath) {
        this.sourcePath = sourcePath;
        this.targetPath = targetPath;
    }

    @Override
    public ServerMigrationTaskResult run(TaskContext context) {
        context.getLogger().debugf("Source's path: %s", sourcePath);
        context.getLogger().debugf("Target's path: %s", targetPath);
        if (!sourcePath.equals(targetPath)) {
            context.getMigrationFiles().copy(sourcePath, targetPath);
            context.getLogger().infof("Resource with path %s migrated.", sourcePath, targetPath);
            return new ServerMigrationTaskResult.Builder()
                    .success()
                    .addAttribute("sourcePath", sourcePath)
                    .addAttribute("targetPath", targetPath)
                    .build();
        } else {
            return new ServerMigrationTaskResult.Builder()
                    .skipped()
                    .addAttribute("sourcePath", sourcePath)
                    .addAttribute("targetPath", targetPath)
                    .build();
        }
    }
}
