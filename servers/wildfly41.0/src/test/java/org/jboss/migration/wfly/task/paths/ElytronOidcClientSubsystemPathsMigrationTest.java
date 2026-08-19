/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.wfly.task.paths;

import org.jboss.migration.core.jboss.XmlConfigurationMigration;
import org.jboss.migration.wfly10.config.task.paths.AbstractXmlConfigurationMigrationComponentTest;

import java.util.List;

/**
 * Unit test for {@link ElytronOidcClientSubsystemPathsMigration}.
 * @author emmartins
 */
public class ElytronOidcClientSubsystemPathsMigrationTest extends AbstractXmlConfigurationMigrationComponentTest {

    @Override
    protected String getXmlConfig() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<server xmlns=\"urn:jboss:domain:1.7\">\n"
                + "  <subsystem xmlns=\"urn:wildfly:elytron-oidc-client:3.0\">\n"
                + "    <realm name=\"myrealm\">\n"
                + "      <client-keystore file=\"${jboss.home.dir}/standalone/data/realm-client.jks\"/>\n"
                + "      <truststore file=\"${jboss.home.dir}/standalone/data/realm-trust.jks\"/>\n"
                + "    </realm>\n"
                + "    <provider name=\"myprovider\">\n"
                + "      <client-keystore file=\"${jboss.home.dir}/standalone/data/provider-client.jks\"/>\n"
                + "      <truststore file=\"${jboss.home.dir}/standalone/data/provider-trust.jks\"/>\n"
                + "    </provider>\n"
                + "    <secure-deployment name=\"myapp.war\">\n"
                + "      <client-keystore file=\"${jboss.home.dir}/standalone/data/deployment-client.jks\"/>\n"
                + "      <truststore file=\"${jboss.home.dir}/standalone/data/deployment-trust.jks\"/>\n"
                + "      <credential client-keystore-file=\"${jboss.home.dir}/standalone/data/credential-client.jks\"/>\n"
                + "    </secure-deployment>\n"
                + "    <secure-server name=\"myserver\">\n"
                + "      <client-keystore file=\"${jboss.home.dir}/standalone/data/server-client.jks\"/>\n"
                + "      <truststore file=\"${jboss.home.dir}/standalone/data/server-trust.jks\"/>\n"
                + "    </secure-server>\n"
                + "  </subsystem>\n"
                + "  <subsystem xmlns=\"urn:wildfly:elytron-oidc-client:preview:4.0\">\n"
                + "    <realm name=\"preview-realm\">\n"
                + "      <request-object-signing-keystore-file file=\"${jboss.home.dir}/standalone/data/preview-signing.jks\"/>\n"
                + "    </realm>\n"
                + "  </subsystem>\n"
                + "</server>\n";
    }

    @Override
    protected XmlConfigurationMigration.ComponentFactory getComponentFactory() {
        return new ElytronOidcClientSubsystemPathsMigration.Factory();
    }

    @Override
    protected List<String> getExpectedCopiedPaths() {
        return List.of(
                "standalone/data/realm-client.jks",
                "standalone/data/realm-trust.jks",
                "standalone/data/provider-client.jks",
                "standalone/data/provider-trust.jks",
                "standalone/data/deployment-client.jks",
                "standalone/data/deployment-trust.jks",
                "standalone/data/credential-client.jks",
                "standalone/data/server-client.jks",
                "standalone/data/server-trust.jks",
                "standalone/data/preview-signing.jks"
        );
    }
}
