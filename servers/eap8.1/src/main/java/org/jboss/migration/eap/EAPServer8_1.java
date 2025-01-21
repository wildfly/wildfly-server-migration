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
package org.jboss.migration.eap;

import org.jboss.migration.core.ProductInfo;
import org.jboss.migration.core.Server;
import org.jboss.migration.core.env.MigrationEnvironment;
import org.jboss.migration.core.jboss.JBossExtensionNames;
import org.jboss.migration.core.jboss.JBossServer;
import org.jboss.migration.wfly10.ServiceLoaderWildFlyServerMigrations10;
import org.jboss.migration.wfly10.WildFlyServer10;
import org.jboss.migration.wfly10.WildFlyServerMigrations10;

import java.nio.file.Path;
import java.util.ServiceLoader;

/**
 * The JBoss EAP 8.1 {@link Server}
 * @author emmartins
 */
public class EAPServer8_1 extends WildFlyServer10 {

    private static final WildFlyServerMigrations10 SERVER_MIGRATIONS = new ServiceLoaderWildFlyServerMigrations10<>(ServiceLoader.load(EAPServerMigrationProvider8_1.class));

    public static final JBossServer.Extensions EXTENSIONS = Extensions.builder()
            .extensionsExcept(EAPXPServer8_1.EXTENSIONS,
                    JBossExtensionNames.MICROMETER,
                    JBossExtensionNames.MICROPROFILE_CONFIG_SMALLRYE,
                    JBossExtensionNames.MICROPROFILE_FAULT_TOLERANCE_SMALLRYE,
                    JBossExtensionNames.MICROPROFILE_HEALTH_SMALLRYE,
                    JBossExtensionNames.MICROPROFILE_JWT_SMALLRYE,
                    JBossExtensionNames.MICROPROFILE_METRICS_SMALLRYE,
                    JBossExtensionNames.MICROPROFILE_LRA_COORDINATOR,
                    JBossExtensionNames.MICROPROFILE_LRA_PARTICIPANT,
                    JBossExtensionNames.MICROPROFILE_OPENAPI_SMALLRYE,
                    JBossExtensionNames.MICROPROFILE_OPENTRACING_SMALLRYE,
                    JBossExtensionNames.MICROPROFILE_REACTIVE_MESSAGING_SMALLRYE,
                    JBossExtensionNames.MICROPROFILE_REACTIVE_STREAMS_OPERATORS_SMALLRYE,
                    JBossExtensionNames.MICROPROFILE_TELEMETRY,
                    JBossExtensionNames.OPENTELEMETRY
            ).build();

    public EAPServer8_1(String migrationName, ProductInfo productInfo, Path baseDir, MigrationEnvironment migrationEnvironment) {
        super(migrationName, productInfo, baseDir, migrationEnvironment, EXTENSIONS);
    }

    @Override
    protected WildFlyServerMigrations10 getMigrations() {
        return SERVER_MIGRATIONS;
    }
}
