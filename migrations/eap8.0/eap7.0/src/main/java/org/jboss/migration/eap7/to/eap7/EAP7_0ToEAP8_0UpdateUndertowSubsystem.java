/*
 * Copyright 2018 Red Hat, Inc.
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

package org.jboss.migration.eap7.to.eap7;

import org.jboss.migration.core.jboss.JBossSubsystemNames;
import org.jboss.migration.wfly10.config.task.management.subsystem.UpdateSubsystemResources;
import org.jboss.migration.wfly10.config.task.subsystem.undertow.AddHttpsListener;
import org.jboss.migration.wfly10.config.task.subsystem.undertow.EnableHttp2;
import org.jboss.migration.wfly11.task.subsystem.undertow.AddHttpInvoker;
import org.jboss.migration.wfly13.task.subsystem.undertow.UnsetDefaultHostResponseHeaderServer;
import org.jboss.migration.wfly13.task.subsystem.undertow.UnsetDefaultHostResponseHeaderXPoweredBy;

/**
 * @author emmartins
 */
public class EAP7_0ToEAP8_0UpdateUndertowSubsystem<S> extends UpdateSubsystemResources<S> {
    public EAP7_0ToEAP8_0UpdateUndertowSubsystem() {
        super(JBossSubsystemNames.UNDERTOW,
                new AddHttpsListener<>(),
                new EnableHttp2<>(),
                new UnsetDefaultHostResponseHeaderServer<>(),
                new UnsetDefaultHostResponseHeaderXPoweredBy<>(),
                new AddHttpInvoker<>());
    }
}
