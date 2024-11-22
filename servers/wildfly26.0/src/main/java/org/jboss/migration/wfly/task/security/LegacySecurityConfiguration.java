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

import org.jboss.migration.core.jboss.JBossServerConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LegacySecurityConfiguration {

    public static final String DEFAULT_ELYTRON_APPLICATION_DOMAIN_NAME = "ApplicationDomain";
    public static final String DEFAULT_ELYTRON_APPLICATION_HTTP_AUTHENTICATION_FACTORY_NAME = "application-http-authentication";
    public static final String DEFAULT_ELYTRON_APPLICATION_SASL_AUTHENTICATION_FACTORY_NAME = "application-sasl-authentication";

    private final JBossServerConfiguration targetConfiguration;
    private final Map<String, LegacySecurityRealm> legacySecurityRealms = new HashMap<>();
    private final Set<LegacySecuredManagementInterface> securedManagementInterfaces = new HashSet<>();
    private final List<LegacySecurityDomain> legacySecurityDomains = new ArrayList<>();
    private String domainControllerRemoteSecurityRealm;

    public LegacySecurityConfiguration(JBossServerConfiguration targetConfiguration) {
        this.targetConfiguration = targetConfiguration;
    }

    public JBossServerConfiguration getTargetConfiguration() {
        return targetConfiguration;
    }

    public Map<String, LegacySecurityRealm> getLegacySecurityRealms() {
        return legacySecurityRealms;
    }

    public LegacySecurityRealm getLegacySecurityRealm(String name) {
        return legacySecurityRealms.get(name);
    }

    public Set<LegacySecuredManagementInterface> getSecuredManagementInterfaces() {
        return securedManagementInterfaces;
    }

    public List<LegacySecurityDomain> getLegacySecurityDomains() {
        return legacySecurityDomains;
    }

    public String getDomainControllerRemoteSecurityRealm() {
        return domainControllerRemoteSecurityRealm;
    }

    public void setDomainControllerRemoteSecurityRealm(String domainControllerRemoteSecurityRealm) {
        this.domainControllerRemoteSecurityRealm = domainControllerRemoteSecurityRealm;
    }

    public String getDefaultElytronApplicationRealmName() {
        return "ApplicationRealm";
    }

    public String getDefaultElytronManagementRealmName() {
        return "ManagementRealm";
    }

    public String getDefaultElytronApplicationDomainName() {
        return DEFAULT_ELYTRON_APPLICATION_DOMAIN_NAME;
    }

    public String getDefaultElytronManagementDomainName() {
        return "ManagementDomain";
    }

    public String getDefaultElytronApplicationHttpAuthenticationFactoryName() {
        return DEFAULT_ELYTRON_APPLICATION_HTTP_AUTHENTICATION_FACTORY_NAME;
    }

    public String getDefaultElytronManagementHttpAuthenticationFactoryName() {
        return "management-http-authentication";
    }

    public String getDefaultElytronApplicationSaslAuthenticationFactoryName() {
        return DEFAULT_ELYTRON_APPLICATION_SASL_AUTHENTICATION_FACTORY_NAME;
    }

    public String getDefaultElytronManagementSaslAuthenticationFactoryName() {
        return "management-sasl-authentication";
    }

    public String getDefaultElytronTLSKeyStoreName() {
        return "applicationKS";
    }

    public String getDefaultElytronTLSKeyManagerName() {
        return "applicationKM";
    }

    public String getDefaultElytronTLSServerSSLContextName() {
        return "applicationSSC";
    }

    @Override
    public String toString() {
        return "LegacySecurityConfiguration{" +
                "targetConfiguration=" + targetConfiguration +
                ", legacySecurityRealms=" + legacySecurityRealms +
                ", securedManagementInterfaces=" + securedManagementInterfaces +
                ", legacySecurityDomains=" + legacySecurityDomains +
                ", domainControllerRemoteSecurityRealm='" + domainControllerRemoteSecurityRealm + '\'' +
                '}';
    }

    public boolean requiresMigration() {
        return !legacySecurityRealms.isEmpty() || !legacySecurityDomains.isEmpty() || !securedManagementInterfaces.isEmpty();
    }
}
