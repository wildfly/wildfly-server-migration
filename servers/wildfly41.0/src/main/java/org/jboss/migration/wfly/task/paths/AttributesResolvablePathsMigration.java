/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.wfly.task.paths;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Migration of resolvable paths specified in XML elements using 'path' and 'relative-to' attributes.
 * @author emmartins
 */
public class AttributesResolvablePathsMigration implements XmlConfigurationMigration.Component {

    /**
     *
     */
    public static class Factory implements XmlConfigurationMigration.ComponentFactory {
        @Override
        public XmlConfigurationMigration.Component newComponent() {
            return new AttributesResolvablePathsMigration();
        }
    }

    private static final String ATTR_NAME_PATH = "path";
    private static final String ATTR_NAME_RELATIVE_TO = "relative-to";

    protected final Map<String,Set<ResolvablePath>> resolvablePaths;

    public AttributesResolvablePathsMigration() {
        this.resolvablePaths = new HashMap<>();
    }

    @Override
    public Set<String> getElementLocalNames() {
        return Set.of(XmlConfigurationMigration.ANY_ELEMENT_NAME);
    }

    @Override
    public void processElement(XMLStreamReader reader, JBossServerConfiguration sourceConfiguration, JBossServerConfiguration targetConfiguration, TaskContext context) throws IOException {
        final String path = reader.getAttributeValue(null, ATTR_NAME_PATH);
        if (path == null) {
            return;
        }

        // ignore paths in content
        if (reader.getAttributeValue(null, "content") != null && "content".equals(reader.getLocalName())) {
            return;
        }
        // ignore deployment scanner paths
        if ("deployment-scanner".equals(reader.getLocalName())) {
            return;
        }
        // ignore welcome-content
        if ("file".equals(reader.getLocalName()) && "welcome-content".equals(reader.getAttributeValue(null, "name"))) {
            return;
        }

        final String relativeTo = reader.getAttributeValue(null, ATTR_NAME_RELATIVE_TO);
        Set<ResolvablePath> elementPaths = resolvablePaths.computeIfAbsent(reader.getLocalName(), k -> new HashSet<>());
        elementPaths.add(relativeTo != null ? new ResolvablePath(path, relativeTo) : ResolvablePath.fromPathExpression(path));
    }

    @Override
    public void afterProcessingElements(JBossServerConfiguration sourceConfiguration, JBossServerConfiguration targetConfiguration, TaskContext taskContext) {
        taskContext.execute(new SimpleComponentTask.Builder()
                .name(taskContext.getTaskName().getName())
                .skipPolicy(TaskSkipPolicy.skipIfDefaultTaskSkipPropertyIsSet())
                .runnable(context -> {
                    for (Map.Entry<String, Set<ResolvablePath>> resolvablePathsEntry : resolvablePaths.entrySet()) {
                        final String subtaskNamePrefix = context.getTaskName()+"."+resolvablePathsEntry.getKey();
                        for (ResolvablePath resolvablePath : resolvablePathsEntry.getValue()) {
                            context.execute(new MigrateResolvablePathTaskBuilder()
                                    .name(subtaskNamePrefix)
                                    .path(resolvablePath)
                                    .source(sourceConfiguration)
                                    .target(targetConfiguration)
                                    .skipIfSourcePathDoesNotExists(true)
                                    .build());
                        }
                    }
                    return context.hasSucessfulSubtasks() ? ServerMigrationTaskResult.SUCCESS : ServerMigrationTaskResult.SKIPPED;})
                .build());
    }
}
