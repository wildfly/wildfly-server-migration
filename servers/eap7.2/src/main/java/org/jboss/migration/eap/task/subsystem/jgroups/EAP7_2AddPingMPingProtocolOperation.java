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
 * Jgroups add protocol's Operation to add PING/MPING and relevant socket-binding based on the stack name.
 * @author istudens
 */
public class EAP7_2AddPingMPingProtocolOperation implements UpdateProtocols.Operation {

    private static final String UDP_PROTOCOL = "PING";
    private static final String TCP_PROTOCOL = "MPING";
    private static final String UDP_STACK = "udp";
    private static final String TCP_STACK = "tcp";
    private static final String SOCKET_BINDING = "socket-binding";
    private static final String JGROUPS_MPING = "jgroups-mping";

    @Override
    public void execute(UpdateProtocols.ProtocolStack protocolStack, TaskContext context) {
        switch (protocolStack.getName()) {
            case UDP_STACK:
                protocolStack.add(UDP_PROTOCOL);
                break;
            case TCP_STACK:
                ModelNode mPingValue = new ModelNode();
                mPingValue.get(SOCKET_BINDING).set(JGROUPS_MPING);
                protocolStack.add(TCP_PROTOCOL, mPingValue);
                break;
            default:
                // this should never happen, do nothing
        }
    }
}
