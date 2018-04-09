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

package org.jboss.migration.wfly10.config.management;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.migration.core.jboss.AbsolutePathResolver;
import org.jboss.migration.core.jboss.JBossServerConfiguration;
import org.jboss.migration.core.jboss.ResolvablePath;
import org.jboss.migration.wfly10.WildFlyServer10;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author emmartins
 */
public interface ManageableServerConfiguration extends AbsolutePathResolver, ManageableResource, ExtensionResource.Parent, HostExcludeResource.Parent, InterfaceResource.Parent, PathResource.Parent, SocketBindingGroupResource.Parent, SystemPropertyResource.Parent {

    void start();
    void stop();
    boolean isStarted();
    ModelNode executeManagementOperation(ModelNode operation) throws ManagementOperationException;
    WildFlyServer10 getServer();
    Path resolvePath(String path) throws ManagementOperationException;
    ModelControllerClient getModelControllerClient();
    JBossServerConfiguration getConfigurationPath();

    default ManageableServerConfigurationType getConfigurationType() {
        return (ManageableServerConfigurationType) getResourceType();
    }

    @Override
    default Path resolveNamedPath(String path) {
        PathResource pathResource = getPathResource(path);
        if (pathResource != null) {
            return resolvePath(new ResolvablePath(pathResource.getResourceConfiguration()));
        } else {
            return getServer().resolveNamedPath(path);
        }
    }

    @Override
    default Path resolvePath(String path, String relativeTo) {
        path = getServer().resolveExpression(path);
        if (path == null) {
            return null;
        }
        final Path resolvedPath;
        if (relativeTo == null) {
            resolvedPath = Paths.get(path).toAbsolutePath();
        } else {
            final Path resolvedRelativeTo = resolveNamedPath(relativeTo);
            if (resolvedRelativeTo == null) {
                return null;
            }
            resolvedPath = path != null ? resolvedRelativeTo.resolve(path).toAbsolutePath() : resolvedRelativeTo.toAbsolutePath();
        }
        return resolvedPath;
    }
}