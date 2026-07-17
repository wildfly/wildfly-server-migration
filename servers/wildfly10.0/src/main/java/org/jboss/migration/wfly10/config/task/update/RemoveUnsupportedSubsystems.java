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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static org.jboss.migration.core.task.component.TaskSkipPolicy.skipIfDefaultTaskSkipPropertyIsSet;

/**
 * @author emmartins
 */
public class RemoveUnsupportedSubsystems<S extends JBossServer<S>> implements ServerConfigurationMigration.XMLConfigurationSubtaskFactory<JBossServerConfiguration<S>> {

    public interface EnvironmentProperties {
        /**
         * the prefix for the name of subsystems related properties
         */
        String PREFIX = "subsystems.";

        /**
         * a list with namespaces of subsystems to include, i.e. not remove from config
         */
        String INCLUDES = PREFIX + "includes";

        /**
         * a list with namespaces of subsystems to exclude, i.e. remove from config
         */
        String EXCLUDES = PREFIX + "excludes";
    }

    public static final String TASK_NAME = "subsystems.remove-unsupported-subsystems";
    public static final String SUBTASK_NAME = TASK_NAME +".remove-unsupported-subsystem";
    public static final String SUBTASK_NAME_ATTRIBUTE_NAMESPACE = "namespace";

    @Override
    public ServerMigrationTask getTask(final JBossServerConfiguration<S> source, final JBossServerConfiguration targetConfigurationPath) {
        return new SimpleComponentTask.Builder()
                .name(TASK_NAME)
                .skipPolicy(skipIfDefaultTaskSkipPropertyIsSet())
                .runnable(context -> {
                    context.getLogger().debugf("Searching for subsystems not supported by the target server...");
                    run(source, targetConfigurationPath, context);
                    if (!context.hasSucessfulSubtasks()) {
                        context.getLogger().debugf("No unsupported subsystems found.");
                    }
                    return ServerMigrationTaskResult.SUCCESS;
                })
                .build();
    }

    protected void run(final JBossServerConfiguration<S> source, final JBossServerConfiguration targetConfigurationPath, final TaskContext context) {
        // gather the namespaces of subsystems to accept (env property includes + target server subsystems - env property excludes)
        final Set<String> accepted = new HashSet<>(((WildFlyServer10)targetConfigurationPath.getServer()).getExtensions().getExtensions().stream()
                .flatMap(extension -> extension.getSubsystems().stream())
                .map(subsystem -> subsystem.getNamespaceWithoutVersion())
                .collect(toList()));
        final MigrationEnvironment environment = context.getMigrationEnvironment();
        accepted.addAll(environment.getPropertyAsList(EnvironmentProperties.INCLUDES, Collections.emptyList()));
        accepted.removeAll(environment.getPropertyAsList(EnvironmentProperties.EXCLUDES, Collections.emptyList()));
        // setup and run the xml filter to remove the ones not accepted
        final Set<String> removed = new HashSet<>();
        final XMLFileFilter extensionsFilter = (startElement, xmlEventReader, xmlEventWriter, xmlEventFactory) -> {
            if (startElement.getName().getLocalPart().equals("subsystem")) {
                final String namespaceURI = startElement.getName().getNamespaceURI();
                // keep if the namespace uri starts with a supported subsystem's namespace without version
                for (String namespaceWithoutVersion : accepted) {
                    if (namespaceURI != null && namespaceURI.startsWith(namespaceWithoutVersion+':')) {
                        return XMLFileFilter.Result.ADD;
                    }
                }
                // not supported, remove subsystem
                // TODO if interactive mode, subsystem not excluded, and not a source server subsystem, then confirm with user its removal (feature to provide configless "custom subsystem" migration)
                final ServerMigrationTask subtask = new SimpleComponentTask.Builder()
                        .name(new ServerMigrationTaskName.Builder(SUBTASK_NAME)
                                .addAttribute(SUBTASK_NAME_ATTRIBUTE_NAMESPACE, namespaceURI)
                                .build())
                        .runnable(subtaskContext -> {
                            subtaskContext.getLogger().debugf("Subsystem with namespace '%s' removed.", namespaceURI);
                            removed.add(namespaceURI);
                            return ServerMigrationTaskResult.SUCCESS;
                        })
                        .build();
                context.execute(subtask);
                return XMLFileFilter.Result.REMOVE;
            } else {
                return XMLFileFilter.Result.NOT_APPLICABLE;
            }
        };
        XMLFiles.filter(targetConfigurationPath.getPath(), extensionsFilter);
        if (!removed.isEmpty()) {
            context.getLogger().infof("Unsupported subsystems removed: %s", removed);
        }
    }
}