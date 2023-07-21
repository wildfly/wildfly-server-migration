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

import org.jboss.migration.core.jboss.JBossSubsystemNames;
import org.jboss.migration.wfly10.config.task.management.subsystem.UpdateSubsystemResources;
import org.jboss.migration.wfly10.config.task.subsystem.jgroups.UpdateProtocols;

/**
 * @author istudens
 */
public class EAP7_2ToEAP8_0UpdateJGroupsSubsystem<S> extends UpdateSubsystemResources<S> {
    public EAP7_2ToEAP8_0UpdateJGroupsSubsystem() {
        super(JBossSubsystemNames.JGROUPS,
                new UpdateProtocols<>(new UpdateProtocols.Operations()
                        .remove("PING")
                        .remove("MPING")
                        .remove("MERGE3")
                        .remove("FD_SOCK")
                        .remove("FD_ALL")
                        .remove("VERIFY_SUSPECT")
                        .remove("pbcast.NAKACK2")
                        .remove("UNICAST3")
                        .remove("pbcast.STABLE")
                        .remove("pbcast.GMS")
                        .remove("UFC")
                        .remove("MFC")
                        .remove("FRAG3")
                        .add("RED")
                        .custom(new EAP7_2AddPingMPingProtocolOperation())
                        .add("MERGE3")
                        .custom(new EAP7_2AddFDSockProtocolOperation("FD_SOCK2"))
                        .add("FD_ALL3")
                        .add("VERIFY_SUSPECT2")
                        .add("pbcast.NAKACK2")
                        .add("UNICAST3")
                        .add("pbcast.STABLE")
                        .add("pbcast.GMS")
                        .add("UFC")
                        .add("MFC")
                        .add("FRAG4")
                ));
    }
}
