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

package org.jboss.migration.wfly10.config.task.update;

import org.jboss.migration.core.ServerMigrationFailureException;
import org.jboss.migration.core.jboss.JBossServer;
import org.jboss.migration.core.jboss.CopyPath;
import org.jboss.migration.core.task.ServerMigrationTaskName;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.component.SimpleComponentTask;
import org.jboss.migration.core.task.component.TaskSkipPolicy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * @author emmartins
 */
public class MigrateContentDir<S extends JBossServer<S>> extends SimpleComponentTask.Builder {
    protected MigrateContentDir(String contentsName, Path sourceContentDir, Path targetContentDir) {
        name("contents."+contentsName+".migrate-content-dir");
        skipPolicies(TaskSkipPolicy.skipIfDefaultTaskSkipPropertyIsSet(), context -> !Files.isDirectory(sourceContentDir));
        beforeRun(context -> context.getLogger().debugf(" Searching for %s content at '%s'...", contentsName, sourceContentDir));
        runnable(context -> {
            final List<Path> contents;
            try {
                contents = Files.find(sourceContentDir, 3, (path, basicFileAttributes) -> Files.isRegularFile(path) && path.getFileName().toString().equals("content"))
                        .map(path -> sourceContentDir.relativize(path))
                        //.filter(path -> path.getNameCount() == 3)
                //path.getParent().getParent().getFileName().toString().length() == 2
                        .collect(toList());
            } catch (IOException e) {
                throw new ServerMigrationFailureException("Failed to read content from "+sourceContentDir, e);
            }
            if (contents.isEmpty()) {
                context.getLogger().debugf("No %s content found to migrate.", contentsName, sourceContentDir);
                return ServerMigrationTaskResult.SKIPPED;
            } else {
                context.getLogger().infof("Migrating %s content found: %s", contentsName, contents);
                // execute subtasks
                for (Path content : contents) {
                    final ServerMigrationTaskName subtaskName = new ServerMigrationTaskName.Builder("contents."+contentsName+".migrate-content").addAttribute("path", content.toString()).build();
                    context.execute(subtaskName, subtaskContext -> new CopyPath(sourceContentDir.resolve(content), targetContentDir.resolve(content)).run(subtaskContext));
                }
                return context.hasSucessfulSubtasks() ? ServerMigrationTaskResult.SUCCESS : ServerMigrationTaskResult.SKIPPED;
            }
        });
    }
}
