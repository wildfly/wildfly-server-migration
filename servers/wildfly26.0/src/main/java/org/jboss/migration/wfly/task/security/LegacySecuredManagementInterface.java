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
package org.jboss.migration.wfly.task.security;

import org.jboss.migration.core.jboss.JBossServer;

import java.util.Objects;

public class LegacySecuredManagementInterface<S extends JBossServer<S>> {

    private final String name;
    private final String securityRealm;

    public LegacySecuredManagementInterface(String name, String securityRealm) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(securityRealm);
        this.name = name;
        this.securityRealm = securityRealm;
    }

    public String getName() {
        return name;
    }

    public String getSecurityRealm() {
        return securityRealm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LegacySecuredManagementInterface<?> that = (LegacySecuredManagementInterface<?>) o;
        return name.equals(that.name) && securityRealm.equals(that.securityRealm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, securityRealm);
    }

    @Override
    public String toString() {
        return "LegacySecuredManagementInterface{" +
                "name='" + name + '\'' +
                ", securityRealm='" + securityRealm + '\'' +
                '}';
    }
}
