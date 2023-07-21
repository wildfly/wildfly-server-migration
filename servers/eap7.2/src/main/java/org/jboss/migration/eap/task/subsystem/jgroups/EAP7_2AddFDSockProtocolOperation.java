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
package org.jboss.migration.eap.task.subsystem.jgroups;

import org.jboss.dmr.ModelNode;
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.wfly10.config.task.subsystem.jgroups.UpdateProtocols;

/**
 * Jgroups add protocol's Operation to add FD_SOCK/FD_SOCK2 and relevant socket-binding based on the stack name.
 * @author istudens
 */
public class EAP7_2AddFDSockProtocolOperation implements UpdateProtocols.Operation {

    private static final String UDP_STACK = "udp";
    private static final String TCP_STACK = "tcp";
    private static final String SOCKET_BINDING = "socket-binding";
    private static final String JGROUPS_UDP_FD = "jgroups-udp-fd";
    private static final String JGROUPS_TCP_FD = "jgroups-tcp-fd";

    private String newProtocol;

    public EAP7_2AddFDSockProtocolOperation(String newProtocol) {
        this.newProtocol = newProtocol;
    }

    @Override
    public void execute(UpdateProtocols.ProtocolStack protocolStack, TaskContext context) {
        final ModelNode mPingValue = new ModelNode();
        if (UDP_STACK.equals(protocolStack.getName())) {
            mPingValue.get(SOCKET_BINDING).set(JGROUPS_UDP_FD);
        } else if (TCP_STACK.equals(protocolStack.getName())) {
            mPingValue.get(SOCKET_BINDING).set(JGROUPS_TCP_FD);
        }
        protocolStack.add(newProtocol, mPingValue);
    }
}
