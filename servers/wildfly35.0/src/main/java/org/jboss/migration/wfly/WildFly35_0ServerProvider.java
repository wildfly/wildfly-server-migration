/*
 * Copyright 2024 Red Hat, Inc.
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
import org.jboss.migration.core.Server;
import org.jboss.migration.core.env.MigrationEnvironment;

import java.nio.file.Path;

/**
 * The WildFly 35.x {@link org.jboss.migration.core.ServerProvider}.
 *  @author emmartins
 */
public class WildFly35_0ServerProvider extends WildFly34_0ServerProvider {

    @Override
    protected String getProductNameRegex() {
        return "WildFly( EE)?";
    }

    @Override
    protected String getProductVersionRegex() {
        return "35\\..*";
    }

    @Override
    protected Server constructServer(String migrationName, ProductInfo productInfo, Path baseDir, MigrationEnvironment migrationEnvironment) {
        return new WildFly35_0Server(migrationName, productInfo, baseDir, migrationEnvironment);
    }

    @Override
    public String getName() {
        return "WildFly 35.x";
    }
}
