/*
 * Copyright 2017 Red Hat, Inc.
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

package org.jboss.migration.wfly11.task.subsystem.elytron;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author emmartins
 */
public class PropertiesRealmAddOperation {
    private final PathAddress subsystemPathAddress;
    private final String propertiesRealm;
    private Properties usersProperties;
    private Properties groupsProperties;

    public PropertiesRealmAddOperation(PathAddress subsystemPathAddress, String propertiesRealm) {
        this.subsystemPathAddress = subsystemPathAddress;
        this.propertiesRealm = propertiesRealm;
    }

    public PropertiesRealmAddOperation usersProperties(Properties properties) {
        this.usersProperties = properties;
        return this;
    }

    public PropertiesRealmAddOperation groupsProperties(Properties properties) {
        this.groupsProperties = properties;
        return this;
    }

    public ModelNode toModelNode() {
        /*
            "users-properties" => {
                "path" => "application-users.properties",
                "relative-to" => "jboss.server.config.dir",
                "digest-realm-name" => "ApplicationRealm"
            },
            "groups-properties" => {
                "path" => "application-roles.properties",
                "relative-to" => "jboss.server.config.dir"
            },
            "operation" => "add",
            "address" => [
                ("subsystem" => "elytron"),
                ("properties-realm" => "ApplicationRealm")
            ]
        */
        final PathAddress pathAddress = subsystemPathAddress.append("properties-realm", propertiesRealm);
        final ModelNode operation = Util.createAddOperation(pathAddress);
        if (usersProperties != null) {
            operation.get("users-properties").set(usersProperties.toModelNode());
        }
        if (groupsProperties != null) {
            operation.get("groups-properties").set(groupsProperties.toModelNode());
        }
        return operation;
    }

    public static class Properties {
        private final String path;
        private String relativeTo;
        private String digestRealmName;
        private Boolean plainText;

        public Properties(String path) {
            this.path = path;
        }

        public Properties digestRealmName(String digestRealmName) {
            this.digestRealmName = digestRealmName;
            return this;
        }

        public Properties plainText(boolean plainText) {
            this.plainText = plainText;
            return this;
        }

        public Properties relativeTo(String relativeTo) {
            this.relativeTo = relativeTo;
            return this;
        }

        ModelNode toModelNode() {
            final ModelNode modelNode = new ModelNode();
            modelNode.get("path").set(path);
            if (relativeTo != null) {
                modelNode.get("relative-to").set(relativeTo);
            }
            if (plainText != null) {
                modelNode.get("plain-text").set(plainText);
            }
            if (digestRealmName != null) {
                modelNode.get("digest-realm-name").set(digestRealmName);
            }
            return modelNode;
        }
    }
}
