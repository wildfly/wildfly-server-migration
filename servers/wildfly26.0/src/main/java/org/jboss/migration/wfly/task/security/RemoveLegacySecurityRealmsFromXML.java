/*
 * Copyright 2022 Red Hat, Inc.
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

package org.jboss.migration.wfly.task.security;

import org.jboss.migration.core.ServerMigrationFailureException;
import org.jboss.migration.core.jboss.JBossServer;
import org.jboss.migration.core.jboss.JBossServerConfiguration;
import org.jboss.migration.core.task.ServerMigrationTask;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.core.task.component.SimpleComponentTask;
import org.jboss.migration.core.util.xml.XMLFileFilter;
import org.jboss.migration.core.util.xml.XMLFiles;
import org.jboss.migration.wfly10.config.task.ServerConfigurationMigration;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.jboss.migration.core.task.component.TaskSkipPolicy.skipIfDefaultTaskSkipPropertyIsSet;

/**
 * @author emmartins
 */
public class RemoveLegacySecurityRealmsFromXML<S extends JBossServer<S>> implements ServerConfigurationMigration.XMLConfigurationSubtaskFactory<JBossServerConfiguration<S>> {

    public static final String TASK_NAME = "security.remove-legacy-security-realms";

    private static final String SECURITY_REALMS = "security-realms";
    private static final String SECURITY_REALM = "security-realm";
    private static final String HTTP_INTERFACE = "http-interface";
    private static final String NATIVE_INTERFACE = "native-interface";
    private static final String REMOTE = "remote";
    private static final String ELYTRON_SUBSYSTEM_XMLNS_PREFIX = "urn:wildfly:elytron:";

    @Override
    public ServerMigrationTask getTask(final JBossServerConfiguration<S> source, final JBossServerConfiguration targetConfigurationPath) {
        return new SimpleComponentTask.Builder()
                .name(TASK_NAME)
                .skipPolicy(skipIfDefaultTaskSkipPropertyIsSet())
                .runnable(context -> {
                    context.getLogger().debug("Removing legacy security realms from the XML configuration...");
                    final ServerMigrationTaskResult taskResult = processXMLConfiguration(source, targetConfigurationPath, context);
                    if (taskResult.getStatus() == ServerMigrationTaskResult.Status.SKIPPED) {
                        context.getLogger().debugf("No legacy security realms found.");
                    } else {
                        context.getLogger().infof("Legacy security realms removed from XML configuration.");
                    }
                    return taskResult;
                })
                .build();
    }

    protected ServerMigrationTaskResult processXMLConfiguration(final JBossServerConfiguration<S> source, final JBossServerConfiguration targetConfigurationPath, final TaskContext context) {
        // setup and run the xml filter
        ServerMigrationTaskResult.Builder taskResultBuilder = new ServerMigrationTaskResult.Builder().skipped();
        final XMLFileFilter extensionsFilter = (startElement, xmlEventReader, xmlEventWriter, xmlEventFactory) -> {
            final String elementLocalName = startElement.getName().getLocalPart();
            if (elementLocalName.equals(SECURITY_REALMS)) {
                if (!startElement.getName().getNamespaceURI().startsWith(ELYTRON_SUBSYSTEM_XMLNS_PREFIX)) {
                    taskResultBuilder.success();
                    return XMLFileFilter.Result.REMOVE;
                } else {
                    return XMLFileFilter.Result.ADD_ALL;
                }
            } else if (elementLocalName.equals(HTTP_INTERFACE) || elementLocalName.equals(NATIVE_INTERFACE) || elementLocalName.equals(REMOTE)) {
                try {
                    if (removeSecurityRealmAttribute(startElement, xmlEventReader, xmlEventWriter, xmlEventFactory)) {
                        taskResultBuilder.success();
                    }
                } catch (Throwable e) {
                    throw new ServerMigrationFailureException(e);
                }
                return XMLFileFilter.Result.CONTINUE;
            } else {
                return XMLFileFilter.Result.NOT_APPLICABLE;
            }
        };
        XMLFiles.filter(targetConfigurationPath.getPath(), extensionsFilter);
        return taskResultBuilder.build();
    }

    protected boolean removeSecurityRealmAttribute(final StartElement startElement, XMLEventReader xmlEventReader, XMLEventWriter xmlEventWriter, XMLEventFactory xmlEventFactory) throws XMLStreamException {
        boolean update = false;
        final List<Attribute> attributes = new ArrayList<>();
        final Iterator<Attribute> iterator = startElement.getAttributes();
        while(iterator.hasNext()) {
            Attribute attribute = iterator.next();
            if (attribute.getName().getLocalPart().equals(SECURITY_REALM)) {
                update = true;
            } else {
                attributes.add(attribute);
            }
        }
        final StartElement startElementToWrite;
        if (update) {
            // write element with updated attributes
            startElementToWrite = xmlEventFactory.createStartElement(startElement.getName(), attributes.iterator(), startElement.getNamespaces());
        } else {
            // write original
            startElementToWrite = startElement;
        }
        writeElement(startElementToWrite, xmlEventReader, xmlEventWriter);
        return update;
    }

    protected void writeElement(StartElement startElement, XMLEventReader xmlEventReader, XMLEventWriter xmlEventWriter) throws XMLStreamException {
        xmlEventWriter.add(startElement);
        int endElementsLeft = 1;
        do {
            XMLEvent xmlEvent = xmlEventReader.nextEvent();
            if (xmlEvent.isStartElement()) {
                endElementsLeft++;
            }
            else if(xmlEvent.isEndElement()){
                endElementsLeft--;
            }
            xmlEventWriter.add(xmlEvent);
        } while (xmlEventReader.hasNext() && endElementsLeft > 0);
    }
}