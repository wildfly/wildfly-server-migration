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

import org.jboss.migration.wfly10.WildFlyServerMigration10;

/**
 * Server migration, from EAP XP 8.1 to EAP XP 8.1.
 * @author emmartins
 */
public class EAPXP8_1ToEAPXP8_1ServerMigrationProvider implements EAPXPServerMigrationProvider8_1 {

    public WildFlyServerMigration10 getServerMigration() {
        // nothing more than the base migration
        return new EAP8_1ToEAP8_1ServerMigrationProvider().getServerMigration();
    }

    @Override
    public Class<EAPXPServer8_1> getSourceType() {
        return EAPXPServer8_1.class;
    }
}
