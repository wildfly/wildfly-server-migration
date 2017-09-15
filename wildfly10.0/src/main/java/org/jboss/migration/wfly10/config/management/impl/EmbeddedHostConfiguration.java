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

package org.jboss.migration.wfly10.config.management.impl;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.migration.wfly10.config.management.HostConfiguration;
import org.jboss.migration.wfly10.config.management.HostControllerConfiguration;
import org.jboss.migration.wfly10.config.task.HostMigration;

import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * @author emmartins
 */
public class EmbeddedHostConfiguration extends AbstractManageableServerConfiguration implements HostConfiguration {

    private final HostControllerConfiguration hostController;

    private final JvmResourceImpl.Factory jvmResources;
    private final ManagementInterfaceResourceImpl.Factory managementInterfaceResources;
    private final SecurityRealmResourceImpl.Factory securityRealmResources;
    private final SubsystemResourceImpl.Factory subsystemResources;


    public EmbeddedHostConfiguration(HostControllerConfiguration hostController, String host) {
        super(host, pathAddress(HOST, host), hostController.getConfigurationPath(), hostController.getServer());
        this.hostController = hostController;

        jvmResources = new JvmResourceImpl.Factory(getResourcePathAddress(), this);
        addChildResourceFactory(jvmResources);
        subsystemResources = new SubsystemResourceImpl.Factory(getResourcePathAddress(), this);
        addChildResourceFactory(subsystemResources);
        final PathAddress managementCoreServicePathAddress = getResourcePathAddress().append(CORE_SERVICE, MANAGEMENT);
        managementInterfaceResources = new ManagementInterfaceResourceImpl.Factory(managementCoreServicePathAddress, this);
        addChildResourceFactory(managementInterfaceResources);
        securityRealmResources = new SecurityRealmResourceImpl.Factory(managementCoreServicePathAddress, this);
        addChildResourceFactory(securityRealmResources);
    }

    @Override
    protected ModelControllerClient startConfiguration() {
        return hostController.getModelControllerClient();
    }

    @Override
    protected void stopConfiguration() {
        writeConfiguration();
    }

    public static class HostConfigFileMigrationFactory implements HostMigration.HostConfigurationProvider {
        @Override
        public EmbeddedHostConfiguration getHostConfiguration(String host, HostControllerConfiguration hostController) {
            return new EmbeddedHostConfiguration(hostController, host);
        }
    }
}
