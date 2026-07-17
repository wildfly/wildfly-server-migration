/*
 * Copyright 2017 Red Hat, Inc.
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

package org.jboss.migration.wfly10.config.task.module;

import org.jboss.migration.core.env.MigrationEnvironment;
import org.jboss.migration.core.jboss.ModulesMigrationTask;
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.wfly10.config.task.update.RemoveUnsupportedExtensions;

import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Finds modules referenced by Extensions.
 * @author emmartins
 */
public class ExtensionModulesFinder implements ConfigurationModulesMigrationTaskFactory.ModulesFinder {
    @Override
    public String getElementLocalName() {
        return "extension";
    }

    @Override
    public void processElement(XMLStreamReader reader, ModulesMigrationTask.ModuleMigrator moduleMigrator, TaskContext context) throws IOException {
        final String namespaceURI = reader.getNamespaceURI();
        if (namespaceURI == null || !namespaceURI.startsWith("urn:jboss:domain:")) {
            return;
        }
        final String moduleId = reader.getAttributeValue(null, "module");
        if (moduleId != null) {
            // gather the module names of extensions to accept (env property includes + target server extensions - env property excludes)
            final Set<String> accepted = new HashSet<>();
            final MigrationEnvironment environment = context.getMigrationEnvironment();
            accepted.addAll(environment.getPropertyAsList(RemoveUnsupportedExtensions.EnvironmentProperties.INCLUDES, Collections.emptyList()));
            accepted.removeAll(environment.getPropertyAsList(RemoveUnsupportedExtensions.EnvironmentProperties.EXCLUDES, Collections.emptyList()));
            if (accepted.contains(moduleId)) {
                moduleMigrator.migrateModule(moduleId, "Required by Extension", context);
            }
        }
    }
}
