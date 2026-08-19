/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.wfly.task.paths;

import org.jboss.migration.core.jboss.XmlConfigurationMigration;
import org.jboss.migration.wfly10.config.task.paths.AbstractXmlConfigurationMigrationComponentTest;

import java.util.List;

/**
 * Unit test for {@link ModclusterSubsystemPathsMigration}.
 * @author emmartins
 */
public class ModclusterSubsystemPathsMigrationTest extends AbstractXmlConfigurationMigrationComponentTest {

    @Override
    protected String getXmlConfig() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<server xmlns=\"urn:jboss:domain:1.7\">\n"
                + "  <subsystem xmlns=\"urn:jboss:domain:modcluster:5.0\">\n"
                + "    <proxy name=\"default\">\n"
                + "      <ssl"
                + "          certificate-key-file=\"${jboss.home.dir}/standalone/data/server.keystore\""
                + "          ca-certificate-file=\"${jboss.home.dir}/standalone/data/ca.crt\""
                + "          ca-revocation-url=\"${jboss.home.dir}/standalone/data/ca.crl\"/>\n"
                + "    </proxy>\n"
                + "  </subsystem>\n"
                + "</server>\n";
    }

    @Override
    protected XmlConfigurationMigration.ComponentFactory getComponentFactory() {
        return new ModclusterSubsystemPathsMigration.Factory();
    }

    @Override
    protected List<String> getExpectedCopiedPaths() {
        return List.of(
                "standalone/data/server.keystore",
                "standalone/data/ca.crt",
                "standalone/data/ca.crl"
        );
    }
}
