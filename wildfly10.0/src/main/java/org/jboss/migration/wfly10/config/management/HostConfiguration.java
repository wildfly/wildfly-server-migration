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

/**
 * @author emmartins
 */
public interface HostConfiguration extends ManageableServerConfiguration, JvmResource.Parent, ManagementInterfaceResource.Parent, SecurityRealmResource.Parent, SubsystemResource.Parent {

    ManageableServerConfigurationType RESOURCE_TYPE = new ManageableServerConfigurationType(HostConfiguration.class, JvmResource.RESOURCE_TYPE, ManagementInterfaceResource.RESOURCE_TYPE, SecurityRealmResource.RESOURCE_TYPE, SubsystemResource.RESOURCE_TYPE);

    @Override
    default ManageableServerConfigurationType getResourceType() {
        return RESOURCE_TYPE;
    }
}