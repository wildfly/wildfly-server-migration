/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.wfly10.config.task.paths;

import org.jboss.migration.core.jboss.XmlConfigurationMigration;

import java.util.List;

/**
 * Unit test for {@link WebSubsystemPathsMigration}.
 * @author emmartins
 */
public class WebSubsystemPathsMigrationTest extends AbstractXmlConfigurationMigrationComponentTest {

    @Override
    protected String getXmlConfig() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<server xmlns=\"urn:jboss:domain:1.7\">\n"
                + "  <subsystem xmlns=\"urn:jboss:domain:web:2.2\">\n"
                + "    <connector name=\"https\" protocol=\"HTTP/1.1\" scheme=\"https\" socket-binding=\"https\" secure=\"true\">\n"
                + "      <ssl name=\"ssl\""
                + "           certificate-key-file=\"${jboss.home.dir}/standalone/data/server.keystore\""
                + "           ca-certificate-file=\"${jboss.home.dir}/standalone/data/ca.crt\"/>\n"
                + "    </connector>\n"
                + "  </subsystem>\n"
                + "</server>\n";
    }

    @Override
    protected XmlConfigurationMigration.ComponentFactory getComponentFactory() {
        return new WebSubsystemPathsMigration.Factory();
    }

    @Override
    protected List<String> getExpectedCopiedPaths() {
        return List.of(
                "standalone/data/server.keystore",
                "standalone/data/ca.crt"
        );
    }
}
