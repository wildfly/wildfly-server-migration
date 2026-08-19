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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Migration of paths referenced in the modcluster subsystem SSL configuration.
 * Handles the {@code ca-certificate-file}, {@code certificate-key-file}, and
 * {@code ca-revocation-url} attributes on the {@code ssl} element of
 * {@code urn:jboss:domain:modcluster:5.0}.
 * @author emmartins
 */
public class ModclusterSubsystemPathsMigration implements XmlConfigurationMigration.Component {

    /**
     *
     */
    public static class Factory implements XmlConfigurationMigration.ComponentFactory {
        @Override
        public XmlConfigurationMigration.Component newComponent() {
            return new ModclusterSubsystemPathsMigration();
        }
    }

    public static final Set<String> ELEMENT_LOCAL_NAMES = Collections.singleton("ssl");

    public static final String CERTIFICATE_KEY_FILE = "certificate-key-file";
    public static final String CA_CERTIFICATE_FILE = "ca-certificate-file";
    public static final String CA_REVOCATION_URL = "ca-revocation-url";

    protected final Set<String> certificateKeyFiles;
    protected final Set<String> caCertificateFiles;
    protected final Set<String> caRevocationUrls;

    protected ModclusterSubsystemPathsMigration() {
        certificateKeyFiles = new HashSet<>();
        caCertificateFiles = new HashSet<>();
        caRevocationUrls = new HashSet<>();
    }

    @Override
    public Set<String> getElementLocalNames() {
        return ELEMENT_LOCAL_NAMES;
    }

    @Override
    public void processElement(XMLStreamReader reader, JBossServerConfiguration sourceConfiguration, JBossServerConfiguration targetConfiguration, TaskContext context) throws IOException {
        final String namespaceURI = reader.getNamespaceURI();
        if (namespaceURI == null || !namespaceURI.startsWith("urn:jboss:domain:modcluster:")) {
            return;
        }
        final String certificateKeyFile = reader.getAttributeValue(null, CERTIFICATE_KEY_FILE);
        if (certificateKeyFile != null) {
            certificateKeyFiles.add(certificateKeyFile);
        }
        final String caCertificateFile = reader.getAttributeValue(null, CA_CERTIFICATE_FILE);
        if (caCertificateFile != null) {
            caCertificateFiles.add(caCertificateFile);
        }
        final String caRevocationUrl = reader.getAttributeValue(null, CA_REVOCATION_URL);
        if (caRevocationUrl != null) {
            caRevocationUrls.add(caRevocationUrl);
        }
    }

    @Override
    public void afterProcessingElements(JBossServerConfiguration sourceConfiguration, JBossServerConfiguration targetConfiguration, TaskContext taskContext) {
        taskContext.execute(new SimpleComponentTask.Builder()
                .name(taskContext.getTaskName().getName()+".subsystem.modcluster.ssl")
                .skipPolicy(TaskSkipPolicy.skipIfDefaultTaskSkipPropertyIsSet())
                .runnable(context -> {
                    final String subtaskNamePrefix = context.getTaskName()+".";
                    for (String certificateKeyFile : certificateKeyFiles) {
                        context.execute(new MigrateResolvablePathTaskBuilder()
                                .name(subtaskNamePrefix+CERTIFICATE_KEY_FILE)
                                .path(ResolvablePath.fromPathExpression(certificateKeyFile))
                                .source(sourceConfiguration)
                                .target(targetConfiguration)
                                .build());
                    }
                    for (String caCertificateFile : caCertificateFiles) {
                        context.execute(new MigrateResolvablePathTaskBuilder()
                                .name(subtaskNamePrefix+CA_CERTIFICATE_FILE)
                                .path(ResolvablePath.fromPathExpression(caCertificateFile))
                                .source(sourceConfiguration)
                                .target(targetConfiguration)
                                .build());
                    }
                    for (String caRevocationUrl : caRevocationUrls) {
                        context.execute(new MigrateResolvablePathTaskBuilder()
                                .name(subtaskNamePrefix+CA_REVOCATION_URL)
                                .path(ResolvablePath.fromPathExpression(caRevocationUrl))
                                .source(sourceConfiguration)
                                .target(targetConfiguration)
                                .build());
                    }
                    return context.hasSucessfulSubtasks() ? ServerMigrationTaskResult.SUCCESS : ServerMigrationTaskResult.SKIPPED;})
                .build());
    }
}
