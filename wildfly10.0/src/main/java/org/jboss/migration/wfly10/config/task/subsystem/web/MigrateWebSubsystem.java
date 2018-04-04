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

package org.jboss.migration.wfly10.config.task.subsystem.web;

import org.jboss.migration.wfly10.config.task.management.subsystem.MigrateSubsystemResourceSubtaskBuilder;
import org.jboss.migration.wfly10.config.task.management.subsystem.MigrateSubsystemResources;
import org.jboss.migration.wfly10.config.task.subsystem.ExtensionNames;
import org.jboss.migration.wfly10.config.task.subsystem.SubsystemNames;

/**
 * @author emmartins
 */
public class MigrateWebSubsystem<S> extends MigrateSubsystemResources<S> {

    public MigrateWebSubsystem() {
        this(new MigrateWebSubsystemSubtaskBuilder<>());
    }

    protected MigrateWebSubsystem(MigrateWebSubsystemSubtaskBuilder<S> subtaskBuilder) {
        super(ExtensionNames.WEB, subtaskBuilder);
    }

    protected static class MigrateWebSubsystemSubtaskBuilder<S> extends MigrateSubsystemResourceSubtaskBuilder<S> {
        public MigrateWebSubsystemSubtaskBuilder() {
            super(SubsystemNames.WEB);
        }
    }
}
