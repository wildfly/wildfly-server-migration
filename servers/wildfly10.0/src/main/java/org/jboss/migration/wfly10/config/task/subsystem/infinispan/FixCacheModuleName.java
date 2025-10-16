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
package org.jboss.migration.wfly10.config.task.subsystem.infinispan;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.migration.core.env.TaskEnvironment;
import org.jboss.migration.core.jboss.ModuleIdentifier;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.wfly10.config.management.ManageableServerConfiguration;
import org.jboss.migration.wfly10.config.management.SubsystemResource;
import org.jboss.migration.wfly10.config.task.management.subsystem.UpdateSubsystemResourceSubtaskBuilder;

import java.util.Objects;

import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

/**
 * A task which changes the module name of any Hibernate Cache present in Infinispan subsystem.
 * @author emmartins
 */
public class FixCacheModuleName<S> extends UpdateSubsystemResourceSubtaskBuilder<S> {

    public interface EnvironmentProperties {
        String NEW_MODULE_NAME = "moduleName";
    }

    public enum CacheType {
        HIBERNATE, WEB
    }

    private final CacheType cacheType;

    private final String defaultNewModuleName;
    private final String moduleAttrName;

    public FixCacheModuleName(CacheType cacheType, String defaultNewModuleName) {
        this(cacheType, defaultNewModuleName, MODULE_ATTR_NAME);
    }

    public FixCacheModuleName(CacheType cacheType, String defaultNewModuleName, String moduleAttrName) {
        this.cacheType = cacheType;
        String taskName = "fix-" + cacheType.name().toLowerCase() + "-cache-module-name";
        subtaskName(taskName);
        this.defaultNewModuleName = Objects.requireNonNull(defaultNewModuleName);
        this.moduleAttrName = Objects.requireNonNull(moduleAttrName);
    }

    private static final String CACHE_CONTAINER = "cache-container";
    private static final String MODULE_ATTR_NAME = "module";


    @Override
    protected ServerMigrationTaskResult updateConfiguration(ModelNode config, S source, SubsystemResource subsystemResource, TaskContext context, TaskEnvironment taskEnvironment) {
        final String cacheName = cacheType.name().toLowerCase();
        final PathAddress subsystemPathAddress = subsystemResource.getResourcePathAddress();
        final ManageableServerConfiguration configurationManagement = subsystemResource.getServerConfiguration();
        // do migration
        if (!config.hasDefined(CACHE_CONTAINER, cacheName)) {
            context.getLogger().debugf("%s cache container not defined.", cacheName);
            return ServerMigrationTaskResult.SKIPPED;
        }
        final ModelNode cache = config.get(CACHE_CONTAINER, cacheName);
        if (!cache.hasDefined(moduleAttrName)) {
            context.getLogger().debugf("%s cache container module not defined.", cacheName);
            return ServerMigrationTaskResult.SKIPPED;
        }
        // read env properties
        final String newModuleName = taskEnvironment.getPropertyAsString(EnvironmentProperties.NEW_MODULE_NAME, defaultNewModuleName);
        final String moduleName = cache.get(moduleAttrName).asString();
        final boolean isList = moduleName.startsWith("[");
        final String simpleModuleName = !isList
                ? moduleName                                            // keep it as is, or...
                : moduleName.substring(1, moduleName.length() - 2);     // remove enveloped [ and ]
        if (ModuleIdentifier.fromString(simpleModuleName).equals(ModuleIdentifier.fromString(newModuleName))) {
            context.getLogger().debugf("%s cache container module already defined with correct module name.",  cacheName);
            return ServerMigrationTaskResult.SKIPPED;
        }
        // /subsystem=infinispan/cache-container=cacheName:write-attribute(name=MODULE_ATTR_NAME,value=MODULE_ATTR_NEW_VALUE)
        final ModelNode op = Util.createEmptyOperation(WRITE_ATTRIBUTE_OPERATION, subsystemPathAddress.append(pathElement(CACHE_CONTAINER, cacheName)));
        op.get(NAME).set(moduleAttrName);
        if (isList) {
            op.get(VALUE).set(new ModelNode().add(newModuleName));
        } else {
            op.get(VALUE).set(newModuleName);
        }
        configurationManagement.executeManagementOperation(op);
        return ServerMigrationTaskResult.SUCCESS;
    }
}