/*
 * Copyright 2025 Red Hat, Inc.
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

package org.jboss.migration.eap.task.subsystem.opentracing;

import org.jboss.migration.core.jboss.JBossExtensionNames;
import org.jboss.migration.core.jboss.JBossSubsystemNames;
import org.jboss.migration.wfly10.config.task.management.subsystem.MigrateSubsystemResources;

/**
 * @author emmartins
 */
public class MigrateMicroprofileOpentracingSmallryeSubsystem<S> extends MigrateSubsystemResources<S> {
    public MigrateMicroprofileOpentracingSmallryeSubsystem() {
        super(JBossExtensionNames.MICROPROFILE_OPENTRACING_SMALLRYE, JBossSubsystemNames.MICROPROFILE_OPENTRACING_SMALLRYE);
    }
}
