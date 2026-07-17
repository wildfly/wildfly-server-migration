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
package org.jboss.migration.eap;

import org.jboss.migration.core.ProductInfo;
import org.jboss.migration.core.Server;
import org.jboss.migration.core.env.MigrationEnvironment;
import org.jboss.migration.core.jboss.Extensions;
import org.jboss.migration.core.jboss.JBossExtensions;
import org.jboss.migration.wfly10.ServiceLoaderWildFlyServerMigrations10;
import org.jboss.migration.wfly10.WildFlyServer10;
import org.jboss.migration.wfly10.WildFlyServerMigrations10;

import java.nio.file.Path;
import java.util.ServiceLoader;

/**
 * The JBoss EAP XP 7.4 {@link Server}
 * @author emmartins
 */
public class EAPXPServer7_4 extends WildFlyServer10 {

    private static final WildFlyServerMigrations10 SERVER_MIGRATIONS = new ServiceLoaderWildFlyServerMigrations10<>(ServiceLoader.load(EAPXPServerMigrationProvider7_4.class));

    public static final Extensions EXTENSIONS = Extensions.builder()
            .extensions(EAPServer7_4.EXTENSIONS)
            .extension(JBossExtensions.MICROPROFILE_CONFIG_SMALLRYE)
            .extension(JBossExtensions.MICROPROFILE_FAULT_TOLERANCE_SMALLRYE)
            .extension(JBossExtensions.MICROPROFILE_HEALTH_SMALLRYE)
            .extension(JBossExtensions.MICROPROFILE_JWT_SMALLRYE)
            .extension(JBossExtensions.MICROPROFILE_METRICS_SMALLRYE)
            .extension(JBossExtensions.MICROPROFILE_OPENAPI_SMALLRYE)
            .extension(JBossExtensions.MICROPROFILE_OPENTRACING_SMALLRYE)
            .extension(JBossExtensions.MICROPROFILE_REACTIVE_MESSAGING_SMALLRYE)
            .extension(JBossExtensions.MICROPROFILE_REACTIVE_STREAMS_OPERATORS_SMALLRYE)
            .build();

    public EAPXPServer7_4(String migrationName, ProductInfo productInfo, Path baseDir, MigrationEnvironment migrationEnvironment) {
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