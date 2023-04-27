/*
 * Copyright 2022 Red Hat, Inc.
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

package org.jboss.migration.wfly.task.security;

import org.jboss.migration.core.jboss.JBossServer;
import org.jboss.migration.core.jboss.JBossServerConfiguration;

/**
 * @author emmartins
 */
public class LegacySecurityConfigurationMigration<S extends JBossServer<S>> {

    private final LegacySecurityConfigurations legacySecurityConfigurations = new LegacySecurityConfigurations();

    public ReadLegacySecurityConfigurationFromXML<S> getReadLegacySecurityConfiguration() {
        return new ReadLegacySecurityConfigurationFromXML<>(legacySecurityConfigurations);
    }

    public RemoveLegacySecurityRealmsFromXML<S> getRemoveLegacySecurityRealms() {
        return new RemoveLegacySecurityRealmsFromXML<>();
    }

    public EnsureBasicElytronSubsystem<JBossServerConfiguration<S>> getEnsureBasicElytronSubsystem() {
        return new EnsureBasicElytronSubsystem<>(legacySecurityConfigurations);
    }

    public MigrateLegacySecurityRealmsToElytron<JBossServerConfiguration<S>> getMigrateLegacySecurityRealmsToElytron() {
        return new MigrateLegacySecurityRealmsToElytron<>(legacySecurityConfigurations);
    }

    public MigrateLegacySecurityDomainsToElytron<JBossServerConfiguration<S>> getMigrateLegacySecurityDomainsToElytron() {
        return new MigrateLegacySecurityDomainsToElytron<>(legacySecurityConfigurations);
    }


}