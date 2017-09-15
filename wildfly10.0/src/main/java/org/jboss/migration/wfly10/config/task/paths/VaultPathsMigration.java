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

package org.jboss.migration.wfly10.config.task.paths;

import org.jboss.migration.core.jboss.JBossServerConfiguration;
import org.jboss.migration.core.jboss.MigrateResolvablePathTaskBuilder;
import org.jboss.migration.core.jboss.ResolvablePath;
import org.jboss.migration.core.jboss.XmlConfigurationMigration;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.core.task.component.SimpleComponentTask;
import org.jboss.migration.core.task.component.TaskSkipPolicy;

import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

/**
 * @author emmartins
 */
public class VaultPathsMigration implements XmlConfigurationMigration.Component {

    /**
     *
     */
    public static class Factory implements XmlConfigurationMigration.ComponentFactory {
        @Override
        public XmlConfigurationMigration.Component newComponent() {
            return new VaultPathsMigration();
        }
    }

    private static final Set<String> ELEMENT_LOCAL_NAMES = Collections.singleton("vault-option");
    private String keystoreURL;
    private String encFileDir;

    protected VaultPathsMigration() {
    }

    @Override
    public Set<String> getElementLocalNames() {
        return ELEMENT_LOCAL_NAMES;
    }

    @Override
    public void processElement(XMLStreamReader reader, JBossServerConfiguration sourceConfiguration, JBossServerConfiguration targetConfiguration, TaskContext context) throws IOException {
        final String namespaceURI = reader.getNamespaceURI();
        if (namespaceURI == null || !namespaceURI.startsWith("urn:jboss:domain:")) {
            return;
        }
        final String optionName = reader.getAttributeValue(null, "name");
        if ("KEYSTORE_URL".equals(optionName)) {
            keystoreURL = reader.getAttributeValue(null, "value");
        } else if ("ENC_FILE_DIR".equals(optionName)) {
            encFileDir = reader.getAttributeValue(null, "value");
        }
    }

    @Override
    public void afterProcessingElements(JBossServerConfiguration sourceConfiguration, JBossServerConfiguration targetConfiguration, TaskContext taskContext) {
        taskContext.execute(new SimpleComponentTask.Builder()
                .name(taskContext.getTaskName().getName()+".vault")
                .skipPolicy(TaskSkipPolicy.skipIfDefaultTaskSkipPropertyIsSet())
                .runnable(context -> {
                    if (keystoreURL != null) {
                        context.execute(new MigrateResolvablePathTaskBuilder()
                                .name(context.getTaskName().getName()+".KEYSTORE_URL")
                                .path(ResolvablePath.fromPathExpression(keystoreURL))
                                .source(sourceConfiguration)
                                .target(targetConfiguration)
                                .build());
                    }
                    if (encFileDir != null) {
                        context.execute(new MigrateResolvablePathTaskBuilder()
                                .name(context.getTaskName().getName()+".ENC_FILE_DIR")
                                .path(ResolvablePath.fromPathExpression(encFileDir))
                                .source(sourceConfiguration)
                                .target(targetConfiguration)
                                .build());
                    }
                    return context.hasSucessfulSubtasks() ? ServerMigrationTaskResult.SUCCESS : ServerMigrationTaskResult.SKIPPED;
                })
                .build());
    }
}
