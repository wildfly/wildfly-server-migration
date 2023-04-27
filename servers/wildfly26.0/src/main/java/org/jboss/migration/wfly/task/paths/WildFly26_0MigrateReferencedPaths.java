/*
 * Copyright 2021 Red Hat, Inc.
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

package org.jboss.migration.wfly.task.paths;

import org.jboss.migration.core.jboss.JBossServer;
import org.jboss.migration.core.jboss.XmlConfigurationMigration;
import org.jboss.migration.wfly10.config.task.paths.ConfigurationPathsMigrationTaskFactory;
import org.jboss.migration.wfly10.config.task.paths.ElytronSubsystemKeystorePathsMigration;
import org.jboss.migration.wfly10.config.task.paths.ElytronSubsystemPropertiesPathsMigration;
import org.jboss.migration.wfly10.config.task.paths.VaultPathsMigration;
import org.jboss.migration.wfly10.config.task.paths.WebSubsystemPathsMigration;

/**
 * @author emmartins
 */
public class WildFly26_0MigrateReferencedPaths<S extends JBossServer<S>> extends ConfigurationPathsMigrationTaskFactory<S> {
    public WildFly26_0MigrateReferencedPaths() {
        super(new XmlConfigurationMigration.Builder<S>()
                .componentFactory(new WebSubsystemPathsMigration.Factory())
                .componentFactory(new VaultPathsMigration.Factory())
                .componentFactory(new SecurityRealmKeystorePathsMigration.Factory())
                .componentFactory(new SecurityRealmPropertiesPathsMigration.Factory())
                .componentFactory(new ElytronSubsystemPropertiesPathsMigration.Factory())
                .componentFactory(new ElytronSubsystemKeystorePathsMigration.Factory())
        );
    }
}
