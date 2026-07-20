/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.wfly;

import org.jboss.migration.core.env.MigrationEnvironment;
import org.jboss.migration.core.jboss.HostExclude;
import org.jboss.migration.core.jboss.HostExcludes;
import org.jboss.migration.core.jboss.JBossServer;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 * Discovers host-excludes configuration from a WildFly server's domain.xml.
 *
 * @author emmartins
 */
public class HostExcludesDiscovery {

    public static final String PROPERTY_CONFIG_FILE = "discovery.host-excludes.configFile";
    public static final String DEFAULT_CONFIG_FILE = "domain.xml";

    /**
     * Discovers host-excludes from the server's domain configuration directory.
     * The domain config file name is read from the migration environment property
     * {@value #PROPERTY_CONFIG_FILE}, defaulting to {@value #DEFAULT_CONFIG_FILE}.
     *
     * @param server the server
     * @param migrationEnvironment the migration environment to obtain the path to the domain config xml
     * @return the discovered HostExcludes
     * @throws IllegalArgumentException if path to the domain config xml, obtained from environment, is not an existent file, or parsing of such file failed
     */
    public static HostExcludes discoverHostExcludes(WildFly41_0Server server, MigrationEnvironment migrationEnvironment) {
        final String configFileName = migrationEnvironment.getPropertyAsString(JBossServer.Environment.getFullEnvironmentPropertyName(server.getMigrationName(), PROPERTY_CONFIG_FILE), DEFAULT_CONFIG_FILE);
        final Path configFilePath = server.getDomainConfigurationDir().resolve(configFileName);
        return discoverHostExcludes(configFilePath);
    }

    /**
     * Discovers host-excludes from a domain config xml file.
     *
     * @param domainXml the path to the domain config xml file
     * @return the discovered HostExcludes
     * @throws IllegalArgumentException if the specified path to a domain config xml is not an existent file,or parsing of such file failed
     */
    public static HostExcludes discoverHostExcludes(Path domainXml) throws IllegalArgumentException {
        final HostExcludes.Builder hostExcludesBuilder = HostExcludes.builder();
        if (!Files.exists(domainXml)) {
            throw new IllegalArgumentException("Failed to discover target server host-excludes configuration, "+domainXml.toAbsolutePath()+" domain config file not found.");
        }
        try (InputStream in = new BufferedInputStream(new FileInputStream(domainXml.toFile()))) {
            final XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(in);
            boolean inHostExcludes = false;
            while (reader.hasNext()) {
                int type = reader.next();
                switch (type) {
                    case START_ELEMENT:
                        if ("host-excludes".equals(reader.getLocalName())) {
                            inHostExcludes = true;
                        } else if (inHostExcludes && "host-exclude".equals(reader.getLocalName())) {
                            hostExcludesBuilder.hostExclude(parseHostExclude(reader));
                        }
                        break;
                    case END_ELEMENT:
                        if ("host-excludes".equals(reader.getLocalName())) {
                            inHostExcludes = false;
                        }
                        break;
                }
            }
        } catch (IOException | XMLStreamException e) {
            throw new IllegalArgumentException("Failed to discover target server host-excludes configuration, "+domainXml.toAbsolutePath()+" domain config parsing failed.", e);
        }
        return hostExcludesBuilder.build();
    }

    private static HostExclude parseHostExclude(XMLStreamReader reader) throws XMLStreamException {
        final HostExclude.Builder builder = HostExclude.builder();
        builder.name(reader.getAttributeValue(null, "name"));
        while (reader.hasNext()) {
            int type = reader.next();
            switch (type) {
                case START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case "host-release":
                            final String releaseId = reader.getAttributeValue(null, "id");
                            if (releaseId != null) {
                                builder.release(releaseId);
                            }
                            break;
                        case "host-api-version":
                            final String majorVersion = reader.getAttributeValue(null, "major-version");
                            final String minorVersion = reader.getAttributeValue(null, "minor-version");
                            if (majorVersion != null && minorVersion != null) {
                                builder.apiVersion(majorVersion, minorVersion, reader.getAttributeValue(null, "micro-version"));
                            }
                            break;
                        case "excluded-extensions":
                            parseExcludedExtensions(reader, builder);
                            break;
                    }
                    break;
                case END_ELEMENT:
                    if ("host-exclude".equals(reader.getLocalName())) {
                        return builder.build();
                    }
                    break;
            }
        }
        return builder.build();
    }

    private static void parseExcludedExtensions(XMLStreamReader reader, HostExclude.Builder builder) throws XMLStreamException {
        while (reader.hasNext()) {
            int type = reader.next();
            switch (type) {
                case START_ELEMENT:
                    if ("extension".equals(reader.getLocalName())) {
                        final String module = reader.getAttributeValue(null, "module");
                        if (module != null) {
                            builder.excludedExtension(module);
                        }
                    }
                    break;
                case END_ELEMENT:
                    if ("excluded-extensions".equals(reader.getLocalName())) {
                        return;
                    }
                    break;
            }
        }
    }
}
