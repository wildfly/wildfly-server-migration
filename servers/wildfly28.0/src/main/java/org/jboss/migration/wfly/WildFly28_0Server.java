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
package org.jboss.migration.wfly;

import org.jboss.migration.core.ProductInfo;
import org.jboss.migration.core.env.MigrationEnvironment;
import org.jboss.migration.core.jboss.Extensions;
import org.jboss.migration.core.jboss.JBossExtensionNames;
import org.jboss.migration.core.jboss.JBossExtensions;
import org.jboss.migration.wfly10.ServiceLoaderWildFlyServerMigrations10;
import org.jboss.migration.wfly10.WildFlyServer10;
import org.jboss.migration.wfly10.WildFlyServerMigrations10;

import java.nio.file.Path;
import java.util.ServiceLoader;

/**
 * The WildFly 28.0 {@link org.jboss.migration.core.Server}.
 * @author emmartins
 */
public class WildFly28_0Server extends WildFlyServer10 {

    public static final Extensions EXTENSIONS = Extensions.builder()
            .extensionsExcept(WildFly27_0Server.EXTENSIONS,
                    JBossExtensionNames.MICROPROFILE_OPENTRACING_SMALLRYE,
                    JBossExtensionNames.MICROPROFILE_METRICS_SMALLRYE)
            .extension(JBossExtensions.MICROMETER)
            .extension(JBossExtensions.MICROPROFILE_LRA_COORDINATOR)
            .extension(JBossExtensions.MICROPROFILE_LRA_PARTICIPANT)
            .extension(JBossExtensions.MICROPROFILE_TELEMETRY)
            .build();

    private static final WildFlyServerMigrations10 SERVER_MIGRATIONS = new ServiceLoaderWildFlyServerMigrations10<>(ServiceLoader.load(WildFly28_0ServerMigrationProvider.class));

    public WildFly28_0Server(String migrationName, ProductInfo productInfo, Path baseDir, MigrationEnvironment migrationEnvironment) {
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
