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
public class KeyManagerAddOperation {

    private final PathAddress subsystemPathAddress;

    private final String name;
    private String keystore;
    private String aliasFilter;
    private boolean generateSelfSignedCertificateHost;
    private String keyPassword;

    public KeyManagerAddOperation(PathAddress subsystemPathAddress, String name) {
        this.subsystemPathAddress = subsystemPathAddress;
        this.name = name;
    }

    public KeyManagerAddOperation aliasFilter(String aliasFilter) {
        this.aliasFilter = aliasFilter;
        return this;
    }

    public KeyManagerAddOperation keystore(String keystore) {
        this.keystore = keystore;
        return this;
    }

    public KeyManagerAddOperation keyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
        return this;
    }

    public KeyManagerAddOperation generateSelfSignedCertificateHost(boolean generateSelfSignedCertificateHost) {
        this.generateSelfSignedCertificateHost = generateSelfSignedCertificateHost;
        return this;
    }

    public ModelNode toModelNode() {
        final PathAddress pathAddress = subsystemPathAddress.append("key-manager", name);
        final ModelNode operation = Util.createAddOperation(pathAddress);
        if (aliasFilter != null) {
            operation.get("alias-filter").set(aliasFilter);
        }
        if (keystore != null) {
            operation.get("key-store").set(keystore);
        }
        if (generateSelfSignedCertificateHost) {
            // TODO consider user interaction here to allow a different host name
            operation.get("generate-self-signed-certificate-host").set("localhost");
        }
        if (keyPassword != null) {
            final ModelNode credentialReferenceNode = new ModelNode();
            credentialReferenceNode.get("clear-text").set(keyPassword);
            operation.get("credential-reference").set(credentialReferenceNode);
        }

        return operation;
    }
}
