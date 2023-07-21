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
package org.jboss.migration.wfly10.config.task.subsystem.jgroups;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;
import org.jboss.migration.core.env.TaskEnvironment;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.wfly10.config.management.ManageableServerConfiguration;
import org.jboss.migration.wfly10.config.management.SubsystemResource;
import org.jboss.migration.wfly10.config.task.management.subsystem.UpdateSubsystemResourceSubtaskBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * A task which updates jgroup subsystem configuration's protocols.
 * @author emmartins
 */
public class UpdateProtocols<S> extends UpdateSubsystemResourceSubtaskBuilder<S> {

    private final List<Operation> operations;

    public UpdateProtocols(Operations operations) {
        subtaskName("update-protocols");
        this.operations = Collections.unmodifiableList(operations.operations);
    }

    private static final String STACK = "stack";
    private static final String PROTOCOL = "protocol";

    @Override
    protected ServerMigrationTaskResult updateConfiguration(ModelNode config, S source, SubsystemResource subsystemResource, TaskContext context, TaskEnvironment taskEnvironment) {
        final PathAddress subsystemPathAddress = subsystemResource.getResourcePathAddress();
        final ManageableServerConfiguration serverConfiguration = subsystemResource.getServerConfiguration();

        final ModelNode stacks = config.get(STACK);
        if (!stacks.isDefined()) {
            context.getLogger().debugf("No stacks defined.");
            return ServerMigrationTaskResult.SKIPPED;
        }
        final Set<String> protocolsRemoved = new HashSet<>();
        final Set<String> protocolsAdded = new HashSet<>();

        final org.jboss.as.controller.client.helpers.Operations.CompositeOperationBuilder compositeOperationBuilder = org.jboss.as.controller.client.helpers.Operations.CompositeOperationBuilder.create();
        for (String stackName : stacks.keys()) {
            final ProtocolStack protocolStack = new ProtocolStack(stackName, config.get(STACK, stackName), context);
            for (Operation operation : operations) {
                operation.execute(protocolStack, context);
            }
            final Set<String> protocolsRemovedFromStack = protocolStack.getProtocolsRemoved();
            final Set<String> protocolsAddedToStack = protocolStack.getProtocolsAdded();
            if (!protocolsRemovedFromStack.isEmpty() || !protocolsAddedToStack.isEmpty()) {
                // protocols order matters so...
                // first remove the old set
                for (Property protocol : protocolStack.sourceProtocols) {
                    compositeOperationBuilder.addStep(Util.createRemoveOperation(subsystemPathAddress.append(STACK, stackName).append(PROTOCOL, protocol.getName())));
                }
                // then add the new set
                for (Property protocol : protocolStack.targetProtocols) {
                    final ModelNode addOp = protocol.getValue().clone();
                    addOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
                    addOp.get(ModelDescriptionConstants.ADDRESS).set(subsystemPathAddress.append(STACK, stackName).append(PROTOCOL, protocol.getName()).toModelNode());
                    compositeOperationBuilder.addStep(addOp);
                }
                protocolsRemoved.addAll(protocolsRemovedFromStack);
                protocolsAdded.addAll(protocolsAddedToStack);
            }
        }

        if (protocolsRemoved.isEmpty() && protocolsAdded.isEmpty()) {
            context.getLogger().debugf("No protocols removed or added.");
            return ServerMigrationTaskResult.SKIPPED;
        }

        serverConfiguration.executeManagementOperation(compositeOperationBuilder.build().getOperation());

        final Logger logger = context.getLogger();
        logger.warnf("Configuration of JGroups protocols has been changed to match the default protocols of the target server. Please note that further manual configuration may be needed if the legacy configuration being used was not the source server's default configuration!");

        return new ServerMigrationTaskResult.Builder()
                .success()
                .addAttribute("protocols-removed", protocolsRemoved)
                .addAttribute("protocols-added", protocolsAdded)
                .build();
    }

    public static class ProtocolStack {

        final List<Property> sourceProtocols;
        final List<Property> targetProtocols;
        final String name;
        final TaskContext taskContext;

        ProtocolStack(String name, ModelNode stackModelNode, TaskContext taskContext) {
            this.name = name;
            final ModelNode protocolsModelNode = stackModelNode.get(PROTOCOL);
            this.sourceProtocols = protocolsModelNode.clone().asPropertyList();
            this.targetProtocols = protocolsModelNode.clone().asPropertyList();
            this.taskContext = taskContext;
        }

        public void add(String protocol) {
            add(protocol, new ModelNode());
        }

        public void add(String protocol, final ModelNode protocolValue) {
            taskContext.getLogger().debug("Adding protocol "+protocol+" with value: "+protocolValue);
            targetProtocols.add(new Property(protocol, protocolValue));
        }

        public ModelNode get(String protocol) {
            final ListIterator<Property> li = targetProtocols.listIterator();
            while (li.hasNext()) {
                Property p = li.next();
                if (p.getName().equals(protocol)) {
                    return p.getValue().clone();
                }
            }
            return null;
        }

        public void update(String protocolName, ModelNode protocolValue) {
            taskContext.getLogger().debug("Updating protocol "+protocolName+": "+protocolValue);
            final ListIterator<Property> li = targetProtocols.listIterator();
            while (li.hasNext()) {
                if (li.next().getName().equals(protocolName)) {
                    li.set(new Property(protocolName, protocolValue.clone()));
                }
            }
        }

        public void replace(String oldProtocol, String newProtocol) {
            taskContext.getLogger().debug("Replacing protocol "+oldProtocol+" with "+newProtocol);
            final ListIterator<Property> li = targetProtocols.listIterator();
            while (li.hasNext()) {
                if (li.next().getName().equals(oldProtocol)) {
                    li.set(new Property(newProtocol, new ModelNode()));
                }
            }
        }

        public boolean remove(String protocol) {
            taskContext.getLogger().debug("Removing protocol "+protocol);
            final ListIterator<Property> li = targetProtocols.listIterator();
            while (li.hasNext()) {
                if (li.next().getName().equals(protocol)) {
                    li.remove();
                    return true;
                }
            }
            return false;
        }

        public String getName() {
            return name;
        }

        Set<String> getProtocolsAdded() {
            final Set<String> result = new HashSet<>();
            for (Property protocol : targetProtocols) {
                result.add(protocol.getName());
            }
            for (Property protocol : sourceProtocols) {
                result.remove(protocol.getName());
            }
            return result;
        }

        Set<String> getProtocolsRemoved() {
            final Set<String> result = new HashSet<>();
            for (Property protocol : sourceProtocols) {
                result.add(protocol.getName());
            }
            for (Property protocol : targetProtocols) {
                result.remove(protocol.getName());
            }
            return result;
        }
    }

    public interface Operation {
        void execute(ProtocolStack protocolStack, TaskContext context);
    }

    public static class Operations {

        final List<Operation> operations = new ArrayList<>();

        public Operations add(String protocol) {
            operations.add((protocolStack, context) -> protocolStack.add(protocol));
            return this;
        }

        public Operations replace(String oldProtocol, String newProtocol) {
            operations.add((protocolStack, context) -> protocolStack.replace(oldProtocol, newProtocol));
            return this;
        }

        public Operations remove(String protocol) {
            operations.add((protocolStack, context) -> protocolStack.remove(protocol));
            return this;
        }

        public Operations custom(Operation operation) {
            operations.add(operation);
            return this;
        }
    }
}