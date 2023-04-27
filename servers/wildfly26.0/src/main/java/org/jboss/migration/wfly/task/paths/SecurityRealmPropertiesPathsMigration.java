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

package org.jboss.migration.wfly.task.paths;

import org.jboss.migration.core.jboss.XmlConfigurationMigration;
import org.jboss.migration.wfly10.config.task.paths.ResolvablePathsMigration;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Migration of properties files referenced by Security Realm XML configurations.
 * @author emmartins
 */
public class SecurityRealmPropertiesPathsMigration extends ResolvablePathsMigration {

    /**
     *
     */
    public static class Factory implements XmlConfigurationMigration.ComponentFactory {
        @Override
        public XmlConfigurationMigration.Component newComponent() {
            return new SecurityRealmPropertiesPathsMigration();
        }
    }

    public static final Set<String> ELEMENT_LOCAL_NAMES = Stream.of("properties").collect(Collectors.toSet());

    protected SecurityRealmPropertiesPathsMigration() {
        super("security-realm.properties", ELEMENT_LOCAL_NAMES, "urn:jboss:domain:");
    }
}
