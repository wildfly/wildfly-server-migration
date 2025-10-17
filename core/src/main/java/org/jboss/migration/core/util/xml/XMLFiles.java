/*
 * Copyright 2016 Red Hat, Inc.
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

package org.jboss.migration.core.util.xml;

import org.jboss.migration.core.ServerMigrationFailureException;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Convenience operations related to XML Files.
 * @author emmartins
 */
public class XMLFiles {

    /**
     * Scans a path for XML files.
     * @param start the starting directory path
     * @param recursive if the scan should include sub directories
     * @param matcher the xml file matcher
     * @return the paths of all files matched
     * @throws ServerMigrationFailureException if there was a failure in the scanning process
     */
    public static Collection<Path> scan(final Path start, final boolean recursive, final XMLFileMatcher matcher) throws ServerMigrationFailureException {
        final SortedSet<Path> result = new TreeSet<>();
        try {
            Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (matcher.matches(file)) {
                        result.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (recursive || dir.equals(start)) {
                        return FileVisitResult.CONTINUE;
                    } else {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                    if (e == null) {
                        return recursive ? FileVisitResult.CONTINUE : FileVisitResult.TERMINATE;
                    } else {
                        // directory iteration failed
                        throw e;
                    }
                }
            });
        } catch (IOException e) {
            throw new ServerMigrationFailureException("XML file scan failed!", e);
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Copy a XML file.
     * @param source the source XML file
     * @param target the target XML file
     * @param filters the xml file content filters
     * @throws ServerMigrationFailureException if there was a failure in the copy process
     */
    public static void copy(Path source, Path target, XMLFileFilter... filters) throws ServerMigrationFailureException {
        try (InputStream inputStream = Files.newInputStream(source); OutputStream outputStream = Files.newOutputStream(target)) {
            filter(inputStream, outputStream, filters);
        } catch (IOException e) {
            throw new ServerMigrationFailureException("xml file copy failed", e);
        }
    }

    /**
     * Filters the specified XML file.
     * @param xmlFile the xml file to filter
     * @param filters the xml file content filters
     * @throws ServerMigrationFailureException
     */
    public static void filter(Path xmlFile, XMLFileFilter... filters) throws ServerMigrationFailureException {
        try {
            byte[] xmlFileBytes = Files.readAllBytes(xmlFile);
            try (InputStream inputStream = new ByteArrayInputStream(xmlFileBytes); OutputStream outputStream = Files.newOutputStream(xmlFile)) {
                filter(inputStream, outputStream, filters);
            }
        } catch (IOException e) {
            throw new ServerMigrationFailureException("XML file filter failed.", e);
        }
    }

    private static void filter(final InputStream inputStream, final OutputStream outputStream, XMLFileFilter... filters) throws ServerMigrationFailureException {
        XMLEventReader xmlEventReader = null;
        XMLEventWriter xmlEventWriter = null;
        final XMLEventFactory xmlEventFactory = XMLEventFactory.newInstance();
        try {
            xmlEventReader = XMLInputFactory.newInstance().createXMLEventReader(inputStream, "UTF-8");
            xmlEventWriter = XMLOutputFactory.newInstance().createXMLEventWriter(outputStream, "UTF-8");
            while (xmlEventReader.hasNext()) {
                XMLEvent xmlEvent = xmlEventReader.nextEvent();
                if (xmlEvent.isStartElement()) {
                    final StartElement startElement = xmlEvent.asStartElement();
                    XMLFileFilter.Result filterResult = XMLFileFilter.Result.NOT_APPLICABLE;
                    if (filters != null) {
                        for (XMLFileFilter filter : filters) {
                            filterResult = filter.filter(startElement, xmlEventReader, xmlEventWriter, xmlEventFactory);
                            if (filterResult != XMLFileFilter.Result.NOT_APPLICABLE) {
                                break;
                            }
                        }
                    }
                    switch (filterResult) {
                        case REMOVE:
                            skipTillEndElement(xmlEventReader);
                            break;
                        case CONTINUE:
                            break;
                        case ADD_ALL:
                            xmlEventWriter.add(xmlEvent);
                            addTillEndElement(xmlEventReader, xmlEventWriter);
                            break;
                        case ADD:
                        case NOT_APPLICABLE:
                        default:
                            xmlEventWriter.add(xmlEvent);
                            break;
                    }
                } else {
                    xmlEventWriter.add(xmlEvent);
                }
            }
        } catch (XMLStreamException e) {
            throw new ServerMigrationFailureException("XML file filtering failed", e);
        } finally {
            if (xmlEventReader != null) {
                try {
                    xmlEventReader.close();
                } catch (XMLStreamException e) {
                    // ignore
                }
            }
            if (xmlEventWriter != null) {
                try {
                    xmlEventWriter.close();
                } catch (XMLStreamException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Process the specified XML file.
     * @param xmlFile the xml file to process
     * @param processor the xml file content processor
     * @throws ServerMigrationFailureException
     */
    public static void process(Path xmlFile, XMLFileProcessor processor) throws ServerMigrationFailureException {
        try {
            byte[] xmlFileBytes = Files.readAllBytes(xmlFile);
            try (InputStream inputStream = new ByteArrayInputStream(xmlFileBytes)) {
                process(inputStream, processor);
            }
        } catch (IOException e) {
            throw new ServerMigrationFailureException("XML file processor failed.", e);
        }
    }

    private static void process(final InputStream inputStream, XMLFileProcessor processor) throws ServerMigrationFailureException {
        XMLEventReader xmlEventReader = null;
        try {
            xmlEventReader = XMLInputFactory.newInstance().createXMLEventReader(inputStream);
            while (xmlEventReader.hasNext()) {
                XMLEvent xmlEvent = xmlEventReader.nextEvent();
                if (xmlEvent.isStartElement()) {
                    processor.process(xmlEvent.asStartElement(), xmlEventReader);
                }
            }
        } catch (XMLStreamException e) {
            throw new ServerMigrationFailureException("XML file processing failed", e);
        } finally {
            if (xmlEventReader != null) {
                try {
                    xmlEventReader.close();
                } catch (XMLStreamException e) {
                    // ignore
                }
            }
        }
    }

    private static void skipTillEndElement(XMLEventReader xmlEventReader) throws XMLStreamException {
        int endElementsLeft = 1;
        do {
            XMLEvent xmlEvent = xmlEventReader.nextEvent();
            if (xmlEvent.isStartElement()) {
                endElementsLeft++;
            }
            else if(xmlEvent.isEndElement()){
                endElementsLeft--;
            }
        } while (xmlEventReader.hasNext() && endElementsLeft > 0);
    }

    private static void addTillEndElement(XMLEventReader xmlEventReader, XMLEventWriter xmlEventWriter) throws XMLStreamException {
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
