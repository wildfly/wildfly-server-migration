/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.migration.wfly10;

import org.jboss.migration.core.ProductInfo;
import org.jboss.migration.core.Server;
import org.jboss.migration.core.env.MigrationEnvironment;
import org.jboss.migration.core.jboss.Extensions;
import org.jboss.migration.core.jboss.JBossServer;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.TaskContext;

import java.nio.file.Path;

/**
 * @author emmartins
 */
public abstract class WildFlyServer10 extends JBossServer<WildFlyServer10> {

    public WildFlyServer10(String migrationName, ProductInfo productInfo, Path baseDir, MigrationEnvironment migrationEnvironment) {
        super(migrationName, productInfo, baseDir, migrationEnvironment);
    }

    @Override
    public ServerMigrationTaskResult migrate(Server source, TaskContext context) throws IllegalArgumentException {
        final WildFlyServerMigration10 migration = getMigration(source);
        if (migration != null) {
            beforeMigration(source, context);
            return migration.run(source, this, context);
        } else {
            return super.migrate(source, context);
        }
    }

    protected void beforeMigration(Server source, TaskContext context) {
    }

    public abstract Extensions getExtensions();

    protected WildFlyServerMigrations10 getMigrations() {
        return null;
    }

    protected WildFlyServerMigration10 getMigration(Server source) {
        final WildFlyServerMigrations10 serverMigrations = getMigrations();
        return serverMigrations != null ? serverMigrations.getMigrationFrom(source) : null;
    }
}
