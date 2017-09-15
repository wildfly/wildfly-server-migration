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

package org.jboss.migration.wfly10.config.task.module;

import org.jboss.migration.core.jboss.JBossServer;

/**
 * @author emmartins
 */
public class MigrateReferencedModules<S extends JBossServer<S>> extends ConfigurationModulesMigrationTaskFactory<S> {
    public MigrateReferencedModules() {
        super(new ConfigurationModulesMigrationTaskFactory.Builder<S>()
                .modulesFinder(new DatasourcesJdbcDriversModulesFinder())
                .modulesFinder(new DefaultJsfImplModulesFinder())
                .modulesFinder(new EEGlobalModulesFinder())
                .modulesFinder(new JMSBridgesModulesFinder())
                .modulesFinder(new NamingObjectFactoriesModulesFinder())
                .modulesFinder(new SecurityRealmsPluginModulesFinder())
                .modulesFinder(new VaultModulesFinder()));
    }
}
