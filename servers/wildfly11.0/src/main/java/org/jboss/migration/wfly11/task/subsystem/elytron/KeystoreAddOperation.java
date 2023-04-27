/*
 * Copyright 2022 Red Hat, Inc.
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
 * @author emmartins
 */
public class KeystoreAddOperation {

    private final PathAddress subsystemPathAddress;

    private final String name;
    private String path;
    private String relativeTo;
    private String keystorePassword;
    private String aliasFilter;
    private String type;

    public KeystoreAddOperation(PathAddress subsystemPathAddress, String name) {
        this.subsystemPathAddress = subsystemPathAddress;
        this.name = name;
    }

    public KeystoreAddOperation aliasFilter(String aliasFilter) {
        this.aliasFilter = aliasFilter;
        return this;
    }

    public KeystoreAddOperation keystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
        return this;
    }

    public KeystoreAddOperation path(String path) {
        this.path = path;
        return this;
    }

    public KeystoreAddOperation relativeTo(String relativeTo) {
        this.relativeTo = relativeTo;
        return this;
    }

    public KeystoreAddOperation type(String type) {
        this.type = type;
        return this;
    }

    public ModelNode toModelNode() {
        final PathAddress pathAddress = subsystemPathAddress.append("key-store", name);
        final ModelNode operation = Util.createAddOperation(pathAddress);
        if (aliasFilter != null) {
            operation.get("alias-filter").set(aliasFilter);
        }
        if (path != null) {
            operation.get("path").set(path);
        }
        if (relativeTo != null) {
            operation.get("relative-to").set(relativeTo);
        }
        if (keystorePassword != null) {
            final ModelNode credentialReferenceNode = new ModelNode();
            credentialReferenceNode.get("clear-text").set(keystorePassword);
            operation.get("credential-reference").set(credentialReferenceNode);
        }
        if (type != null) {
            operation.get("type").set(type);
        }
        return operation;
    }
}
