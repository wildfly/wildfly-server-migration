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
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.migration.core.jboss.JBossServerConfiguration;
import org.jboss.migration.wfly10.WildFlyServer10;
import org.jboss.migration.wfly10.config.management.HostControllerConfiguration;
import org.jboss.migration.wfly10.config.management.ManagementOperationException;
import org.jboss.migration.wfly10.config.task.ServerConfigurationMigration;
import org.wildfly.core.embedded.EmbeddedProcessFactory;
import org.wildfly.core.embedded.EmbeddedProcessStartException;
import org.wildfly.core.embedded.HostController;
import org.wildfly.security.manager.WildFlySecurityManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
    private final HostExcludeResourceImpl.Factory hostExcludeResources;

    private final ProfileResourceImpl.Factory profileResources;
    private final ServerGroupResourceImpl.Factory serverGroupResources;
    private final Map<String, String> propertiesToReset;

    protected EmbeddedHostControllerConfiguration(String domainConfig, String hostConfig, JBossServerConfiguration configurationPath, WildFlyServer10 server) {
        super("", PathAddress.EMPTY_ADDRESS, configurationPath, server);
        propertiesToReset = new HashMap<>();
        this.domainConfig = domainConfig;
        this.hostConfig = hostConfig;
        deploymentResources = new DeploymentResourceImpl.Factory(getResourcePathAddress(), this);
        addChildResourceFactory(deploymentResources);
        deploymentOverlayResources = new DeploymentOverlayResourceImpl.Factory(getResourcePathAddress(), this);
        addChildResourceFactory(deploymentOverlayResources);
        hostResources = new HostResourceImpl.Factory(getResourcePathAddress(), this);
        addChildResourceFactory(hostResources);
        hostExcludeResources = new HostExcludeResourceImpl.Factory(getResourcePathAddress(), this);
        addChildResourceFactory(hostExcludeResources);
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
        cmds.add("-Dorg.wildfly.logging.embedded=false");
        if (!getServer().getEnvironment().isDefaultDomainBaseDir()) {
            setSystemProperty("jboss.domain.base.dir", getServer().getDomainDir());
        }
        if (!getServer().getEnvironment().isDefaultDomainConfigDir()) {
            setSystemProperty("jboss.domain.config.dir", getServer().getDomainConfigurationDir());
        }
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
    public void start() {
        super.start();
        // CMTOOL-400, domain config may have issues that prevent loading, migration should fail
        if (domainConfig != null && profileResources.getResources().isEmpty()) {
            final ModelNode operation = new ModelNode();
            operation.get(ModelDescriptionConstants.OP).set("read-boot-errors");
            final ModelNode address = operation.get(ModelDescriptionConstants.ADDRESS);
            address.add("host",  hostResources.getResourceNames().iterator().next());
            address.add("core-service", "management");
            final ModelNode result = executeManagementOperation(operation);
            if (!result.asList().isEmpty()) {
                throw new ManagementOperationException("Host controller failed to boot the domain configuration. Boot errors: "+result);
            }
        }
    }

    @Override
    protected void stopConfiguration() {
        if (hostConfig == null) {
            writeConfiguration();
        }
        hostController.stop();
        hostController = null;
        resetProperties();
    }

    public static class DomainConfigFileMigrationFactory implements ServerConfigurationMigration.ManageableConfigurationProvider {
        @Override
        public HostControllerConfiguration getManageableConfiguration(JBossServerConfiguration configurationPath, WildFlyServer10 server) {
            return new EmbeddedHostControllerConfiguration(configurationPath.getPathRelativeToConfigurationDir().toString(), null, configurationPath, server);
        }
    }

    public static class HostConfigFileMigrationFactory implements ServerConfigurationMigration.ManageableConfigurationProvider {
        @Override
        public HostControllerConfiguration getManageableConfiguration(JBossServerConfiguration configurationPath, WildFlyServer10 server) {
            return new EmbeddedHostControllerConfiguration(null, configurationPath.getPathRelativeToConfigurationDir().toString(), configurationPath, server);
        }
    }

    private void setSystemProperty(final String name, final Object value) {
        if (value != null) {
            final String currentValue = WildFlySecurityManager.getPropertyPrivileged(name, null);
            WildFlySecurityManager.setPropertyPrivileged(name, value.toString());
            propertiesToReset.put(name, currentValue);
        }
    }

    private void resetProperties() {
        final Iterator<Map.Entry<String, String>> iterator = propertiesToReset.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<String, String> entry = iterator.next();
            if (entry.getValue() == null) {
                WildFlySecurityManager.clearPropertyPrivileged(entry.getKey());
            } else {
                WildFlySecurityManager.setPropertyPrivileged(entry.getKey(), entry.getValue());
            }
            iterator.remove();
        }
    }
}
