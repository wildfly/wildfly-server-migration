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
package org.jboss.migration.core.ts.jboss;

import org.jboss.migration.core.ProductInfo;
import org.jboss.migration.core.env.MigrationEnvironment;
import org.jboss.migration.core.jboss.JBossServer;

import java.nio.file.Path;

public class TestJBossServer extends JBossServer {
    private static final String SERVER_NAME = "TestServerName";
    private static final String SERVER_VERSION = "TestServerVersion";

    static final String MIGRATION_NAME = SERVER_NAME + "Migration";

    public TestJBossServer(Path baseDir, MigrationEnvironment migrationEnvironment) {
        super(MIGRATION_NAME, new ProductInfo(SERVER_NAME, SERVER_VERSION), baseDir, migrationEnvironment);
    }
}
