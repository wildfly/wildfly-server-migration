/*
 * Copyright 2023 Red Hat, Inc.
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

package org.jboss.migration.wfly10.config.task.update;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.migration.core.task.ServerMigrationTaskName;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.component.TaskSkipPolicy;
import org.jboss.migration.wfly10.config.management.ManageableServerConfiguration;
import org.jboss.migration.wfly10.config.management.SocketBindingGroupResource;
import org.jboss.migration.wfly10.config.management.SubsystemResource;
import org.jboss.migration.wfly10.config.task.management.configuration.ManageableServerConfigurationCompositeSubtasks;
import org.jboss.migration.wfly10.config.task.management.configuration.ManageableServerConfigurationCompositeTask;
import org.jboss.migration.wfly10.config.task.management.resource.ManageableResourceLeafTask;

import java.util.Arrays;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.migration.core.task.component.TaskSkipPolicy.skipIfDefaultTaskSkipPropertyIsSet;

/**
 * Adds JGroups FD socket bindings.
 * @author istudens
 */
public class AddJGroupsFDSocketBindings<S> extends ManageableServerConfigurationCompositeTask.Builder<S> {

    private static final List<SocketBinding> SOCKET_BINDINGS = Arrays.asList(
        new SocketBinding.Builder().name("jgroups-tcp-fd").requires("jgroups-tcp").interfaceName("private").port(57600).build(),
        new SocketBinding.Builder().name("jgroups-udp-fd").requires("jgroups-udp").interfaceName("private").port(54200).build()
    );

    private static final String REQUIRED_SUBSYSTEM = "jgroups";
    private static final String TASK_NAME = "socket-bindings.jgroups-fd.add";


    public AddJGroupsFDSocketBindings() {
        name(TASK_NAME);
        skipPolicyBuilders(TaskSkipPolicy.Builders.skipIfDefaultTaskSkipPropertyIsSet(),
                buildParameters -> context -> buildParameters.getServerConfiguration().findResources(SubsystemResource.class, REQUIRED_SUBSYSTEM).isEmpty());
        beforeRun(context -> context.getLogger().debugf("Adding JGroups FD socket bindings..."));
        final ManageableServerConfigurationCompositeSubtasks.Builder<S> subtasks = new ManageableServerConfigurationCompositeSubtasks.Builder<>();
        subtasks.subtask(SocketBindingGroupResource.class, new AddSocketBinding<>(SOCKET_BINDINGS));
        subtasks(subtasks);
        afterRun(context -> {
            if (context.hasSucessfulSubtasks()) {
                context.getLogger().infof("JGroups FD Socket bindings added.");
            } else {
                context.getLogger().debugf("No JGroups FD socket bindings added.");
            }
        });
    }

    public static class AddSocketBinding<S> extends ManageableResourceLeafTask.Builder<S, SocketBindingGroupResource> {

        protected AddSocketBinding(List<SocketBinding> socketBindings) {
            nameBuilder(parameters -> new ServerMigrationTaskName.Builder(TASK_NAME + ".steps").build());
            skipPolicy(skipIfDefaultTaskSkipPropertyIsSet());
            runBuilder(params -> context -> {
                final SocketBindingGroupResource socketBindingGroupResource = params.getResource();
                final ManageableServerConfiguration serverConfiguration = socketBindingGroupResource.getServerConfiguration();
                final PathAddress socketBindingGroupAddress = socketBindingGroupResource.getResourcePathAddress();

                final org.jboss.as.controller.client.helpers.Operations.CompositeOperationBuilder compositeOperationBuilder = org.jboss.as.controller.client.helpers.Operations.CompositeOperationBuilder.create();
                // add steps for each socket binding
                socketBindings.stream().forEach(binding -> {
                    if (socketBindingGroupResource.hasSocketBindingResource(binding.requires)) {
                        final ModelNode addOp = Util.createEmptyOperation(ADD, socketBindingGroupAddress.append(SOCKET_BINDING, binding.getName()));
                        addOp.get(INTERFACE).set(binding.getInterfaceName());
                        addOp.get(PORT).set(binding.getPort());
                        compositeOperationBuilder.addStep(addOp);
                        context.getLogger().debugf("JGroups FD Socket binding %s added to group %s.", binding.toString(), socketBindingGroupResource.getResourceName());
                    }
                });

                serverConfiguration.executeManagementOperation(compositeOperationBuilder.build().getOperation());

                return new ServerMigrationTaskResult.Builder()
                        .success()
                        .addAttribute("added", socketBindings.toString())
                        .build();
            });
        }
    }

    public static class SocketBinding {
        private final String name;
        private final String requires;
        private final String interfaceName;
        private final int port;

        private SocketBinding(Builder builder) {
            this.name = builder.name;
            this.interfaceName = builder.interfaceName;
            this.port = builder.port;
            this.requires = builder.requires;
        }

        public String getName() {
            return name;
        }

        public String getRequires() {
            return requires;
        }

        public String getInterfaceName() {
            return interfaceName;
        }

        public int getPort() {
            return port;
        }

        @Override
        public String toString() {
            return "SocketBinding{" +
                    "name='" + name + '\'' +
                    ", interfaceName='" + interfaceName + '\'' +
                    ", port=" + port +
                    '}';
        }

        public static class Builder {
            private String name;
            private String requires;
            private String interfaceName;
            private int port;

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder requires(String requires) {
                this.requires = requires;
                return this;
            }

            public Builder interfaceName(String interfaceName) {
                this.interfaceName = interfaceName;
                return this;
            }

            public Builder port(int port) {
                this.port = port;
                return this;
            }

            public SocketBinding build() {
                return new SocketBinding(this);
            }
        }
    }
}