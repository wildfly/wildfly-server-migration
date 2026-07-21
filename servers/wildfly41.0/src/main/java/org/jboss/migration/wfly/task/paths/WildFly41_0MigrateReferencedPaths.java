/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.wfly.task.paths;

import org.jboss.migration.core.jboss.JBossServer;
import org.jboss.migration.core.jboss.XmlConfigurationMigration;
import org.jboss.migration.wfly10.config.task.paths.ConfigurationPathsMigrationTaskFactory;
import org.jboss.migration.wfly10.config.task.paths.VaultPathsMigration;
import org.jboss.migration.wfly10.config.task.paths.WebSubsystemPathsMigration;

/**
 * @author emmartins
 */
public class WildFly41_0MigrateReferencedPaths<S extends JBossServer<S>> extends ConfigurationPathsMigrationTaskFactory<S> {
    public WildFly41_0MigrateReferencedPaths() {
        super(new XmlConfigurationMigration.Builder<S>()
                .componentFactory(new VaultPathsMigration.Factory())
                .componentFactory(new WebSubsystemPathsMigration.Factory())
                .componentFactory(new AttributesResolvablePathsMigration.Factory())
        );
    }
}
