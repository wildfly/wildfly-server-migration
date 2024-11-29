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

package org.jboss.migration.wfly10.config.task.paths;

import org.jboss.migration.core.jboss.XmlConfigurationMigration;

import java.util.Collections;
import java.util.Set;

/**
 * Migration of secret key credetial store files referenced by Elytron subsystem XML configurations.
 * @author istudens
 */
public class ElytronSubsystemSecretKeyCredentialStorePathsMigration extends ResolvablePathsMigration {

    /**
     *
     */
    public static class Factory implements XmlConfigurationMigration.ComponentFactory {
        @Override
        public XmlConfigurationMigration.Component newComponent() {
            return new ElytronSubsystemSecretKeyCredentialStorePathsMigration();
        }
    }

    public static final Set<String> ELEMENT_LOCAL_NAMES = Collections.singleton("secret-key-credential-store");

    protected ElytronSubsystemSecretKeyCredentialStorePathsMigration() {
        super("subsystem.elytron.secret-key-credential-store", ELEMENT_LOCAL_NAMES, "urn:wildfly:elytron:", true);
    }
}
