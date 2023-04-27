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

public class LegacySecurityRealm {

    private final String name;
    private Authentication authentication;
    private Authorization authorization;
    private ServerIdentities serverIdentities;

    public LegacySecurityRealm(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    public Authorization getAuthorization() {
        return authorization;
    }

    public void setAuthorization(Authorization authorization) {
        this.authorization = authorization;
    }

    public ServerIdentities getServerIdentities() {
        return serverIdentities;
    }

    public void setServerIdentities(ServerIdentities serverIdentities) {
        this.serverIdentities = serverIdentities;
    }

    public static String getElytronSecurityDomainName(String legacySecurityRealmName) {
        return legacySecurityRealmName+"-securitydomain";
    }

    public String getElytronSecurityDomainName() {
        return getElytronSecurityDomainName(name);
    }

    public String getElytronPropertiesRealmName() {
        return name+"-propertiesrealm";
    }

    public String getElytronHttpAuthenticationFactoryName() {
        return name+"-httpAuthenticationFactory";
    }

    public String getElytronSaslAuthenticationFactoryName() {
        return getElytronSaslAuthenticationFactoryName(name);
    }

    public static String getElytronSaslAuthenticationFactoryName(String legacySecurityRealmName) {
        return legacySecurityRealmName+"-saslAuthenticationFactory";
    }

    public String getElytronTLSKeyStoreName() {
        return name+"-TLS-keyStore";
    }

    public String getElytronTLSKeyManagerName() {
        return name+"-TLS-keyManager";
    }

    public String getElytronTLSServerSSLContextName() {
        return name+"-TLS-serverSSLContext";
    }

    @Override
    public String toString() {
        return "LegacySecurityRealm{" +
                "name='" + name + '\'' +
                ", authentication=" + authentication +
                ", authorization=" + authorization +
                ", serverIdentities=" + serverIdentities +
                '}';
    }

    public static class ServerIdentities {

        private LegacySecurityRealmSSLServerIdentity ssl;

        public LegacySecurityRealmSSLServerIdentity getSsl() {
            return ssl;
        }

        public void setSsl(LegacySecurityRealmSSLServerIdentity ssl) {
            this.ssl = ssl;
        }

        @Override
        public String toString() {
            return "ServerIdentities{" +
                    "ssl=" + ssl +
                    '}';
        }
    }

    public static class Authorization {
        private boolean mapGroupsToRoles = true;
        private Properties properties;

        public boolean isMapGroupsToRoles() {
            return mapGroupsToRoles;
        }

        public void setMapGroupsToRoles(boolean mapGroupsToRoles) {
            this.mapGroupsToRoles = mapGroupsToRoles;
        }

        public Properties getProperties() {
            return properties;
        }

        public void setProperties(Properties properties) {
            this.properties = properties;
        }

        @Override
        public String toString() {
            return "Authorization{" +
                    "mapGroupsToRoles=" + mapGroupsToRoles +
                    ", properties=" + properties +
                    '}';
        }
    }

    public static class Authentication {
        private Local local;
        private Properties properties;

        public Local getLocal() {
            return local;
        }

        public void setLocal(Local local) {
            this.local = local;
        }

        public Properties getProperties() {
            return properties;
        }

        public void setProperties(Properties properties) {
            this.properties = properties;
        }

        @Override
        public String toString() {
            return "Authentication{" +
                    "local=" + local +
                    ", properties=" + properties +
                    '}';
        }
    }

    public static class Properties {
        private String path;
        private String relativeTo;
        private boolean plainText;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getRelativeTo() {
            return relativeTo;
        }

        public void setRelativeTo(String relativeTo) {
            this.relativeTo = relativeTo;
        }

        public boolean isPlainText() {
            return plainText;
        }

        public void setPlainText(boolean plainText) {
            this.plainText = plainText;
        }

        @Override
        public String toString() {
            return "Properties{" +
                    "path='" + path + '\'' +
                    ", relativeTo='" + relativeTo + '\'' +
                    ", plainText=" + plainText +
                    '}';
        }
    }

    public static class Local {
        private String defaultUser;
        private String allowedUsers;
        private boolean skipGroupLoading;

        public String getDefaultUser() {
            return defaultUser;
        }

        public void setDefaultUser(String defaultUser) {
            this.defaultUser = defaultUser;
        }

        public String getAllowedUsers() {
            return allowedUsers;
        }

        public void setAllowedUsers(String allowedUsers) {
            this.allowedUsers = allowedUsers;
        }

        public boolean isSkipGroupLoading() {
            return skipGroupLoading;
        }

        public void setSkipGroupLoading(boolean skipGroupLoading) {
            this.skipGroupLoading = skipGroupLoading;
        }

        @Override
        public String toString() {
            return "Local{" +
                    "defaultUser='" + defaultUser + '\'' +
                    ", allowedUsers='" + allowedUsers + '\'' +
                    ", skipGroupLoading=" + skipGroupLoading +
                    '}';
        }
    }

}
