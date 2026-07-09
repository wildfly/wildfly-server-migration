/*
 * Copyright 2020 Red Hat, Inc.
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
package org.jboss.migration.eap;

import org.jboss.migration.core.ProductInfo;
import org.jboss.migration.core.Server;
import org.jboss.migration.core.env.MigrationEnvironment;
import org.jboss.migration.core.jboss.Extensions;
import org.jboss.migration.core.jboss.JBossExtensions;
import org.jboss.migration.wfly10.ServiceLoaderWildFlyServerMigrations10;
import org.jboss.migration.wfly10.WildFlyServer10;
import org.jboss.migration.wfly10.WildFlyServerMigrations10;
import org.jboss.migration.wfly13.WildFly13_0Server;

import java.nio.file.Path;
import java.util.ServiceLoader;

/**
 * The JBoss EAP 7.4 {@link Server}
 * @author emmartins
 */
public class EAPServer7_4 extends WildFlyServer10 {

    private static final WildFlyServerMigrations10 SERVER_MIGRATIONS = new ServiceLoaderWildFlyServerMigrations10<>(ServiceLoader.load(EAPServerMigrationProvider7_4.class));

    public static final Extensions EXTENSIONS = Extensions.builder()
            .extensions(WildFly13_0Server.EXTENSIONS)
            .extension(JBossExtensions.DATASOURCES_AGROAL)
            .extension(JBossExtensions.CLUSTERING_WEB)
            .extension(JBossExtensions.HEALTH)
            .extension(JBossExtensions.METRICS)
            .build();

    public EAPServer7_4(String migrationName, ProductInfo productInfo, Path baseDir, MigrationEnvironment migrationEnvironment) {
        super(migrationName, productInfo, baseDir, migrationEnvironment);
    }

    @Override
    public Extensions getExtensions() {
        return EXTENSIONS;
    }

    @Override
    protected WildFlyServerMigrations10 getMigrations() {
        return SERVER_MIGRATIONS;
    }
}
