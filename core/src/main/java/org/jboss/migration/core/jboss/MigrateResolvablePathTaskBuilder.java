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

import org.jboss.migration.core.task.ServerMigrationTask;
import org.jboss.migration.core.task.ServerMigrationTaskName;
import org.jboss.migration.core.task.component.SimpleComponentTask;
import org.jboss.migration.core.task.component.TaskSkipPolicy;

import java.util.Objects;

/**
 * @author emmartins
 */
public class MigrateResolvablePathTaskBuilder {

    private String name;
    private ResolvablePath path;
    private JBossServerConfiguration source;
    private JBossServerConfiguration target;

    public MigrateResolvablePathTaskBuilder name(String name) {
        this.name = name;
        return this;
    }

    public MigrateResolvablePathTaskBuilder path(ResolvablePath path) {
        this.path = path;
        return this;
    }

    public MigrateResolvablePathTaskBuilder source(JBossServerConfiguration source) {
        this.source = source;
        return this;
    }

    public MigrateResolvablePathTaskBuilder target(JBossServerConfiguration target) {
        this.target = target;
        return this;
    }

    public ServerMigrationTask build() {
        return new SimpleComponentTask.Builder()
                .name(new ServerMigrationTaskName.Builder(Objects.requireNonNull(name)).addAttribute("path", Objects.requireNonNull(path.toString())).build())
                .skipPolicy(TaskSkipPolicy.skipIfDefaultTaskSkipPropertyIsSet())
                .runnable(new MigrateResolvablePathTaskRunnable(Objects.requireNonNull(path), Objects.requireNonNull(source), Objects.requireNonNull(target)))
                .build();
    }
}
