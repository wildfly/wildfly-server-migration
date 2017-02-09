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

package org.jboss.migration.eap6.to.eap7.tasks;

import org.jboss.migration.wfly10.config.task.subsystem.ExtensionNames;
import org.jboss.migration.wfly10.config.task.subsystem.SubsystemNames;
import org.jboss.migration.wfly10.config.task.subsystem.UpdateSubsystemTaskFactory;
import org.jboss.migration.wfly10.config.task.subsystem.ee.AddConcurrencyUtilitiesDefaultConfig;
import org.jboss.migration.wfly10.config.task.subsystem.ee.AddDefaultBindingsConfig;
import org.jboss.migration.wfly10.config.task.subsystem.ejb3.AddInfinispanPassivationStoreAndDistributableCache;
import org.jboss.migration.wfly10.config.task.subsystem.ejb3.DefinePassivationDisabledCacheRef;
import org.jboss.migration.wfly10.config.task.subsystem.ejb3.RefHttpRemotingConnectorInEJB3Remote;
import org.jboss.migration.wfly10.config.task.subsystem.infinispan.AddEjbCache;
import org.jboss.migration.wfly10.config.task.subsystem.infinispan.AddServerCache;
import org.jboss.migration.wfly10.config.task.subsystem.infinispan.FixHibernateCacheModuleName;
import org.jboss.migration.wfly10.config.task.subsystem.messaging.AddHttpAcceptorsAndConnectors;
import org.jboss.migration.wfly10.config.task.subsystem.remoting.AddHttpConnectorIfMissing;
import org.jboss.migration.wfly10.config.task.subsystem.undertow.AddBufferCache;
import org.jboss.migration.wfly10.config.task.subsystem.undertow.SetDefaultHostResponseHeaderServer;
import org.jboss.migration.wfly10.config.task.subsystem.undertow.SetDefaultHostResponseHeaderXPoweredBy;
import org.jboss.migration.wfly10.config.task.subsystem.undertow.AddWebsockets;
import org.jboss.migration.wfly10.config.task.subsystem.undertow.HttpListenerWorkerWarning;
import org.jboss.migration.wfly10.config.task.subsystem.undertow.SetDefaultHttpListenerRedirectSocket;

/**
 * @author emmartins
 */
public class EAPSubsystemUpdates7_0 {

    public static final UpdateSubsystemTaskFactory INFINISPAN = new UpdateSubsystemTaskFactory.Builder(SubsystemNames.INFINISPAN, ExtensionNames.INFINISPAN)
                                .subtasks(AddServerCache.INSTANCE, AddEjbCache.INSTANCE, FixHibernateCacheModuleName.INSTANCE)
                                .build();

    public static final UpdateSubsystemTaskFactory EE = new UpdateSubsystemTaskFactory.Builder(SubsystemNames.EE, ExtensionNames.EE)
                                .subtasks(AddConcurrencyUtilitiesDefaultConfig.INSTANCE, AddDefaultBindingsConfig.INSTANCE)
                                .build();

    public static final UpdateSubsystemTaskFactory EJB3 = new UpdateSubsystemTaskFactory.Builder(SubsystemNames.EJB3, ExtensionNames.EJB3)
                                .subtasks(RefHttpRemotingConnectorInEJB3Remote.INSTANCE, DefinePassivationDisabledCacheRef.INSTANCE, AddInfinispanPassivationStoreAndDistributableCache.INSTANCE)
                                .build();

    public static final UpdateSubsystemTaskFactory REMOTING = new UpdateSubsystemTaskFactory.Builder(SubsystemNames.REMOTING, ExtensionNames.REMOTING)
                                .subtasks(AddHttpConnectorIfMissing.INSTANCE)
                                .build();

    public static final UpdateSubsystemTaskFactory UNDERTOW = new UpdateSubsystemTaskFactory.Builder(SubsystemNames.UNDERTOW, ExtensionNames.UNDERTOW)
                                .subtasks(AddBufferCache.INSTANCE, SetDefaultHttpListenerRedirectSocket.INSTANCE, AddWebsockets.INSTANCE, new SetDefaultHostResponseHeaderServer(), new SetDefaultHostResponseHeaderXPoweredBy(),HttpListenerWorkerWarning.INSTANCE)
                                .build();

    public static final UpdateSubsystemTaskFactory MESSAGING_ACTIVEMQ = new UpdateSubsystemTaskFactory.Builder(SubsystemNames.MESSAGING_ACTIVEMQ, ExtensionNames.MESSAGING_ACTIVEMQ)
                                .subtasks(AddHttpAcceptorsAndConnectors.INSTANCE)
                                .build();

}
