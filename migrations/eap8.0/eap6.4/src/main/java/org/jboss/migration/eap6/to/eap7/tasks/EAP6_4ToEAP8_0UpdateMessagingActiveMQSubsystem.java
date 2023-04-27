/*
 * Copyright 2020 Red Hat, Inc.
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

package org.jboss.migration.eap6.to.eap7.tasks;

import org.jboss.migration.core.jboss.JBossSubsystemNames;
import org.jboss.migration.wfly10.config.task.management.subsystem.UpdateSubsystemResources;
import org.jboss.migration.wfly10.config.task.subsystem.messagingactivemq.AddHttpAcceptorsAndConnectors;

/**
 * @author emmartins
 */
public class EAP6_4ToEAP8_0UpdateMessagingActiveMQSubsystem<S> extends UpdateSubsystemResources<S> {
    public EAP6_4ToEAP8_0UpdateMessagingActiveMQSubsystem() {
        super(JBossSubsystemNames.MESSAGING_ACTIVEMQ,
                new AddHttpAcceptorsAndConnectors<>());
    }
}
