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
import org.jboss.migration.core.jboss.JBossServerConfiguration;
import org.jboss.migration.wfly10.WildFlyServer10;
import org.jboss.migration.wfly10.config.management.HostControllerConfiguration;
import org.jboss.migration.wfly10.config.task.ServerConfigurationMigration;
import org.wildfly.core.embedded.EmbeddedProcessFactory;
import org.wildfly.core.embedded.EmbeddedProcessStartException;
import org.wildfly.core.embedded.HostController;

import java.util.ArrayList;
import java.util.List;

/**
 * @author emmartins
 */
public class EmbeddedHostControllerConfiguration extends AbstractManageableServerConfiguration implements HostControllerConfiguration {

    private final String domainConfig;
    private final String hostConfig;
    private HostController hostController;

    private final DeploymentResourceImpl.Factory deploymentResources;
    private final DeploymentOverlayResourceImpl.Factory deploymentOverlayResources;
    private final HostResourceImpl.Factory hostResources;
    private final ProfileResourceImpl.Factory profileResources;
    private final ServerGroupResourceImpl.Factory serverGroupResources;

    protected EmbeddedHostControllerConfiguration(String domainConfig, String hostConfig, JBossServerConfiguration configurationPath, WildFlyServer10 server) {
        super("", PathAddress.EMPTY_ADDRESS, configurationPath, server);
        this.domainConfig = domainConfig;
        this.hostConfig = hostConfig;
        deploymentResources = new DeploymentResourceImpl.Factory(getResourcePathAddress(), this);
        addChildResourceFactory(deploymentResources);
        deploymentOverlayResources = new DeploymentOverlayResourceImpl.Factory(getResourcePathAddress(), this);
        addChildResourceFactory(deploymentOverlayResources);
        hostResources = new HostResourceImpl.Factory(getResourcePathAddress(), this);
        addChildResourceFactory(hostResources);
        profileResources = new ProfileResourceImpl.Factory(getResourcePathAddress(), this);
        addChildResourceFactory(profileResources);
        serverGroupResources = new ServerGroupResourceImpl.Factory(getResourcePathAddress(), this);
        addChildResourceFactory(serverGroupResources);
    }

    @Override
    protected ModelControllerClient startConfiguration() {

        final List<String> cmds = new ArrayList<>();
        if (domainConfig != null) {
            cmds.add("--domain-config="+ domainConfig);
        }
        if (hostConfig != null) {
            cmds.add("--host-config="+ hostConfig);
        }
        cmds.add("--admin-only");
        final String[] systemPackages = {"org.jboss.logmanager"};
        hostController = EmbeddedProcessFactory.createHostController(getServer().getBaseDir().toString(), null, systemPackages, cmds.toArray(new String[cmds.size()]));
        try {
            hostController.start();
        } catch (EmbeddedProcessStartException e) {
            throw new RuntimeException(e);
        }
        return hostController.getModelControllerClient();
    }

    @Override
    protected void stopConfiguration() {
        if (hostConfig == null) {
            writeConfiguration();
        }
        hostController.stop();
        hostController = null;
    }

    public static class DomainConfigFileMigrationFactory implements ServerConfigurationMigration.ManageableConfigurationProvider {
        @Override
        public HostControllerConfiguration getManageableConfiguration(JBossServerConfiguration configurationPath, WildFlyServer10 server) {
            return new EmbeddedHostControllerConfiguration(configurationPath.getPath().getFileName().toString(), null, configurationPath, server);
        }
    }

    public static class HostConfigFileMigrationFactory implements ServerConfigurationMigration.ManageableConfigurationProvider {
        @Override
        public HostControllerConfiguration getManageableConfiguration(JBossServerConfiguration configurationPath, WildFlyServer10 server) {
            return new EmbeddedHostControllerConfiguration(null, configurationPath.getPath().getFileName().toString(), configurationPath, server);
        }
    }
}
