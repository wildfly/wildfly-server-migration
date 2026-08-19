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
import java.util.HashSet;
import java.util.Set;

/**
 * Migration of paths referenced by the Elytron OIDC Client subsystem XML configuration.
 * Handles {@code client-keystore} and {@code truststore} elements (via the {@code file} attribute),
 * the {@code client-keystore-file} attribute on {@code credential} elements, and the
 * {@code request-object-signing-keystore-file} element (via the {@code file} attribute).
 * @author emmartins
 */
public class ElytronOidcClientSubsystemPathsMigration implements XmlConfigurationMigration.Component {

    /**
     *
     */
    public static class Factory implements XmlConfigurationMigration.ComponentFactory {
        @Override
        public XmlConfigurationMigration.Component newComponent() {
            return new ElytronOidcClientSubsystemPathsMigration();
        }
    }

    private static final String NAMESPACE_URI_PREFIX = "urn:wildfly:elytron-oidc-client:";

    public static final Set<String> ELEMENT_LOCAL_NAMES = Set.of(
            "client-keystore",
            "truststore",
            "credential",
            "request-object-signing-keystore-file"
    );

    private static final String ATTR_FILE = "file";
    private static final String ATTR_CLIENT_KEYSTORE_FILE = "client-keystore-file";

    protected final Set<String> files;

    protected ElytronOidcClientSubsystemPathsMigration() {
        files = new HashSet<>();
    }

    @Override
    public Set<String> getElementLocalNames() {
        return ELEMENT_LOCAL_NAMES;
    }

    @Override
    public void processElement(XMLStreamReader reader, JBossServerConfiguration sourceConfiguration, JBossServerConfiguration targetConfiguration, TaskContext context) throws IOException {
        final String namespaceURI = reader.getNamespaceURI();
        if (namespaceURI == null || !namespaceURI.startsWith(NAMESPACE_URI_PREFIX)) {
            return;
        }
        final String localName = reader.getLocalName();
        if ("credential".equals(localName)) {
            // credential element uses a dedicated attribute name for the keystore file path
            final String clientKeystoreFile = reader.getAttributeValue(null, ATTR_CLIENT_KEYSTORE_FILE);
            if (clientKeystoreFile != null) {
                files.add(clientKeystoreFile);
            }
        } else {
            // client-keystore, truststore, request-object-signing-keystore-file all use a 'file' attribute
            final String file = reader.getAttributeValue(null, ATTR_FILE);
            if (file != null) {
                files.add(file);
            }
        }
    }

    @Override
    public void afterProcessingElements(JBossServerConfiguration sourceConfiguration, JBossServerConfiguration targetConfiguration, TaskContext taskContext) {
        taskContext.execute(new SimpleComponentTask.Builder()
                .name(taskContext.getTaskName().getName() + ".subsystem.elytron-oidc-client")
                .skipPolicy(TaskSkipPolicy.skipIfDefaultTaskSkipPropertyIsSet())
                .runnable(context -> {
                    final String subtaskNamePrefix = context.getTaskName() + ".file";
                    for (String file : files) {
                        context.execute(new MigrateResolvablePathTaskBuilder()
                                .name(subtaskNamePrefix)
                                .path(ResolvablePath.fromPathExpression(file))
                                .source(sourceConfiguration)
                                .target(targetConfiguration)
                                .skipIfSourcePathDoesNotExists(true)
                                .build());
                    }
                    return context.hasSucessfulSubtasks() ? ServerMigrationTaskResult.SUCCESS : ServerMigrationTaskResult.SKIPPED;
                })
                .build());
    }
}
