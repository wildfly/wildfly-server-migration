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

package org.jboss.migration.core.jboss;

import org.jboss.migration.core.ServerMigrationFailureException;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.core.task.component.TaskRunnable;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static javax.xml.stream.XMLStreamConstants.START_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 * A {@link TaskRunnable} to migrate {@link JBossServerConfiguration}s at XML level, delegating all the XML processing to a set of components, each responsible for processing specific XML Element(s).
 * @author emmartins
 */
public class XmlConfigurationMigration<S extends JBossServer<S>> implements TaskRunnable {

    private final Set<ComponentFactory> componentFactories;
    private final JBossServerConfiguration<S> sourceConfiguration;
    private final JBossServerConfiguration<S> targetConfiguration;

    protected XmlConfigurationMigration(JBossServerConfiguration<S> sourceConfiguration, JBossServerConfiguration<S> targetConfiguration, Set<ComponentFactory> componentFactories) {
        this.sourceConfiguration = sourceConfiguration;
        this.targetConfiguration = targetConfiguration;
        this.componentFactories = Collections.unmodifiableSet(componentFactories);
    }

    @Override
    public ServerMigrationTaskResult run(TaskContext context) {
        // retrieve all components from factories and map these to related element name
        final Map<String, List<Component>> componentsMap = new HashMap<>();
        for (ComponentFactory componentFactory : componentFactories) {
            final Component component = componentFactory.newComponent();
            component.beforeProcessingElements(sourceConfiguration, targetConfiguration, context);
            for (String elementLocalName : component.getElementLocalNames()) {
                List<Component> elementComponents = componentsMap.get(elementLocalName);
                if (elementComponents == null) {
                    elementComponents = new ArrayList<>();
                    componentsMap.put(elementLocalName, elementComponents);
                }
                elementComponents.add(component);
            }
        }
        // parse config
        try (InputStream in = new BufferedInputStream(new FileInputStream(targetConfiguration.getPath().toFile()))) {
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(in);
            reader.require(START_DOCUMENT, null, null);
            while (reader.hasNext()) {
                if (reader.next() == START_ELEMENT) {
                    final List<Component> elementComponents = componentsMap.get(reader.getLocalName());
                    if (elementComponents != null) {
                        for (Component elementComponent : elementComponents) {
                            elementComponent.processElement(reader, sourceConfiguration, targetConfiguration, context);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new ServerMigrationFailureException(e);
        }
        // signal components that element processing is done
        for (List<Component> components : componentsMap.values()) {
            for (Component component : components) {
                component.afterProcessingElements(sourceConfiguration, targetConfiguration, context);
            }
        }
        return context.hasSucessfulSubtasks() ? ServerMigrationTaskResult.SUCCESS : ServerMigrationTaskResult.SKIPPED;
    }

    /**
     * The component interface, responsible for processing specific XML elements.
     * @author emmartins
     */
    public interface Component {

        /**
         * The XML Element's local names the component should process.
         * @return
         */
        Set<String> getElementLocalNames();

        /**
         * A component callback invoked before any element is processed
         * @param sourceConfiguration
         * @param targetConfiguration
         * @param taskContext
         */
        default void beforeProcessingElements(JBossServerConfiguration sourceConfiguration, JBossServerConfiguration targetConfiguration, TaskContext taskContext) {
        }

        /**
         * {@link XmlConfigurationMigration} component callback, invoked when a component's element is found.
         * @param reader the XML stream reader, positioned at the start of an element of interest
         */
        void processElement(XMLStreamReader reader, JBossServerConfiguration sourceConfiguration, JBossServerConfiguration targetConfiguration, TaskContext context) throws IOException;

        /**
         * A component callback invoked after all elements were processed
         * @param sourceConfiguration
         * @param targetConfiguration
         * @param taskContext
         */
        default void afterProcessingElements(JBossServerConfiguration sourceConfiguration, JBossServerConfiguration targetConfiguration, TaskContext taskContext) {
        }
    }

    /**
     * Factory to create new instances of a component.
     */
    public interface ComponentFactory {
        /**
         *
         * @return a new instance of the factory's component.
         */
        Component newComponent();
    }

    /**
     *
     * @param <S>
     */
    public static class Builder<S extends JBossServer<S>> {

        private final Set<ComponentFactory> componentFactories = new HashSet<>();

        public synchronized Builder<S> componentFactory(ComponentFactory componentFactory) {
            componentFactories.add(componentFactory);
            return this;
        }

        public XmlConfigurationMigration<S> build(JBossServerConfiguration<S> sourceConfiguration, JBossServerConfiguration<S> targetConfiguration) {
            return new XmlConfigurationMigration<>(sourceConfiguration, targetConfiguration, componentFactories);
        }
    }
}
