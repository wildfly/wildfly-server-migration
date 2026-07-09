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

package org.jboss.migration.wfly10.config.task.update;

import org.jboss.migration.core.env.MigrationEnvironment;
import org.jboss.migration.core.jboss.JBossServer;
import org.jboss.migration.core.jboss.JBossServerConfiguration;
import org.jboss.migration.core.task.ServerMigrationTask;
import org.jboss.migration.core.task.ServerMigrationTaskName;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.core.task.component.SimpleComponentTask;
import org.jboss.migration.core.util.xml.XMLFileFilter;
import org.jboss.migration.core.util.xml.XMLFiles;
import org.jboss.migration.wfly10.WildFlyServer10;
import org.jboss.migration.wfly10.config.task.ServerConfigurationMigration;

import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.jboss.migration.core.task.component.TaskSkipPolicy.skipIfDefaultTaskSkipPropertyIsSet;

/**
 * @author emmartins
 */
public class RemoveUnsupportedExtensions<S extends JBossServer<S>> implements ServerConfigurationMigration.XMLConfigurationSubtaskFactory<JBossServerConfiguration<S>> {

    public interface EnvironmentProperties {
        /**
         * the prefix for the name of extensions related properties
         */
        String PREFIX = "extensions.";

        /**
         * a list with extensions to include, i.e. not remove from config
         */
        String INCLUDES = PREFIX + "includes";

        /**
         * a list with extensions to exclude, i.e. remove from config
         */
        String EXCLUDES = PREFIX + "excludes";
    }

    public static final String TASK_NAME = "extensions.remove-unsupported-extensions";
    public static final String SUBTASK_NAME = TASK_NAME +".remove-unsupported-extension";
    public static final String SUBTASK_NAME_ATTRIBUTE_MODULE = "module";

    @Override
    public ServerMigrationTask getTask(final JBossServerConfiguration<S> source, final JBossServerConfiguration targetConfigurationPath) {
        return new SimpleComponentTask.Builder()
                .name(TASK_NAME)
                .skipPolicy(skipIfDefaultTaskSkipPropertyIsSet())
                .runnable(context -> {
                    context.getLogger().debugf("Searching for extensions not supported by the target server...");
                    removeExtensions(source, targetConfigurationPath, context);
                    if (!context.hasSucessfulSubtasks()) {
                        context.getLogger().debugf("No unsupported extensions found.");
                    }
                    return ServerMigrationTaskResult.SUCCESS;
                })
                .build();
    }

    protected void removeExtensions(final JBossServerConfiguration<S> source, final JBossServerConfiguration targetConfigurationPath, final TaskContext context) {
        // gather the module names of extensions to accept (env property includes + target server extensions - env property excludes)
        // TODO rework to don't need cast
        final Set<String> accepted = new HashSet<>(((WildFlyServer10)targetConfigurationPath.getServer()).getExtensions().getExtensionModuleNames());
        final MigrationEnvironment environment = context.getMigrationEnvironment();
        accepted.addAll(environment.getPropertyAsList(EnvironmentProperties.INCLUDES, Collections.emptyList()));
        accepted.removeAll(environment.getPropertyAsList(EnvironmentProperties.EXCLUDES, Collections.emptyList()));
        // setup and run the xml filter to remove not accepted extensions
        final Set<String> removed = new HashSet<>();
        final XMLFileFilter extensionsFilter = (startElement, xmlEventReader, xmlEventWriter, xmlEventFactory) -> {
            if (startElement.getName().getLocalPart().equals("extension")) {
                Attribute moduleAttr = startElement.getAttributeByName(new QName("module"));
                final String moduleName = moduleAttr.getValue();
                if (accepted.contains(moduleName)) {
                    return XMLFileFilter.Result.ADD;
                } else {
                    // TODO if interactive mode, extension not excluded, and not a source server extension, then confirm with user its removal (feature to provide configless custom extension migration)
                    final ServerMigrationTask subtask = new SimpleComponentTask.Builder()
                            .name(new ServerMigrationTaskName.Builder(SUBTASK_NAME)
                                    .addAttribute(SUBTASK_NAME_ATTRIBUTE_MODULE, moduleName)
                                    .build())
                            .runnable(subtaskContext -> {
                                subtaskContext.getLogger().debugf("Extension with module '%s' removed.", moduleName);
                                removed.add(moduleName);
                                return ServerMigrationTaskResult.SUCCESS;
                            })
                            .build();
                    context.execute(subtask);
                    return XMLFileFilter.Result.REMOVE;
                }
            } else {
                if (startElement.getName().getLocalPart().equals("excluded-extensions")) {
                    // this element also has extension child elements, yet those should not be filtered
                    return XMLFileFilter.Result.ADD_ALL;
                } else {
                    return XMLFileFilter.Result.NOT_APPLICABLE;
                }
            }
        };
        XMLFiles.filter(targetConfigurationPath.getPath(), extensionsFilter);
        if (!removed.isEmpty()) {
            context.getLogger().infof("Unsupported extensions removed: %s", removed);
        }
    }
}