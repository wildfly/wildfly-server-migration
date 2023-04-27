/*
 * Copyright 2021 Red Hat, Inc.
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

package org.jboss.migration.wfly.task.subsystem.microprofile;

import org.jboss.migration.core.jboss.JBossExtensionNames;
import org.jboss.migration.core.jboss.JBossSubsystemNames;
import org.jboss.migration.wfly10.config.management.ProfileResource;
import org.jboss.migration.wfly10.config.task.management.subsystem.AddSubsystemResourceSubtaskBuilder;
import org.jboss.migration.wfly10.config.task.management.subsystem.AddSubsystemResources;

/**
 * @author emmartins
 */
public class WildFly25_0AddMicroprofileJwtSmallryeSubsystem<S> extends AddSubsystemResources<S> {

    public WildFly25_0AddMicroprofileJwtSmallryeSubsystem() {
        super(JBossExtensionNames.MICROPROFILE_JWT_SMALLRYE, new SubtaskBuilder<>());
        // do not add subsystem config to "standalone-load-balancer.xml" config
        skipPolicyBuilders(getSkipPolicyBuilder(),
                buildParameters -> context -> buildParameters.getServerConfiguration().getConfigurationPath().getPath().endsWith("standalone-load-balancer.xml"));
    }

    static class SubtaskBuilder<S> extends AddSubsystemResourceSubtaskBuilder<S> {
        SubtaskBuilder() {
            super(JBossSubsystemNames.MICROPROFILE_JWT_SMALLRYE);
            // do not add subsystem config to profile "load-balancer"
            skipPolicyBuilder(buildParameters -> context -> buildParameters.getResource().getResourceType() == ProfileResource.RESOURCE_TYPE && buildParameters.getResource().getResourceName().equals("load-balancer"));
        }
    }
}
