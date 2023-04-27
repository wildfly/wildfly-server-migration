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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LegacySecurityDomain {

    public static final Set<String> SUPPORTED_LOGIN_MODULE_CODES = Stream.of("RealmDirect", "Remoting").collect(Collectors.toSet());

    private final String name;
    private final String profile;
    private String cacheType;

    private Authentication authentication;
    private AuthenticationJaspi authenticationJaspi;
    private Authorization authorization;

    public LegacySecurityDomain(String name, String profile) {
        this.name = Objects.requireNonNull(name);
        this.profile = profile;
    }

    public String getName() {
        return name;
    }

    public String getProfile() {
        return profile;
    }

    public String getCacheType() {
        return cacheType;
    }

    public void setCacheType(String cacheType) {
        this.cacheType = cacheType;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    public AuthenticationJaspi getAuthenticationJaspi() {
        return authenticationJaspi;
    }

    public void setAuthenticationJaspi(AuthenticationJaspi authenticationJaspi) {
        this.authenticationJaspi = authenticationJaspi;
    }

    public Authorization getAuthorization() {
        return authorization;
    }

    public void setAuthorization(Authorization authorization) {
        this.authorization = authorization;
    }

    public String getElytronSecurityDomainName() {
        return "legacy-securityDomain"+name+"-securityDomain";
    }

    @Override
    public String toString() {
        return "LegacySecurityDomain{" +
                "name='" + name + '\'' +
                ", profile='" + profile + '\'' +
                ", cacheType='" + cacheType + '\'' +
                ", authentication=" + authentication +
                ", authenticationJaspi=" + authenticationJaspi +
                ", authorization=" + authorization +
                '}';
    }

    public static class Authentication {

        private final List<LoginModule> loginModules = new ArrayList<>();

        public List<LoginModule> getLoginModules() {
            return loginModules;
        }

        @Override
        public String toString() {
            return "Authentication{" +
                    "loginModules=" + loginModules +
                    '}';
        }
    }

    public static class Module {

        private String code;
        private String flag;
        private String name;
        private String module;
        private final Map<String, String> moduleOptions = new HashMap<>();

        public String getCode() {
            return code;
        }

        protected void setCode(String code) {
            this.code = Objects.requireNonNull(code);
        }

        public String getFlag() {
            return flag;
        }

        protected void setFlag(String flag) {
            this.flag = flag;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getModule() {
            return module;
        }

        public void setModule(String module) {
            this.module = module;
        }

        public Map<String, String> getModuleOptions() {
            return moduleOptions;
        }
    }

    public static class LoginModule extends Module {
        @Override
        public String toString() {
            return "LoginModule{" +
                    "code='" + getCode() + '\'' +
                    ", flag='" + getFlag() + '\'' +
                    ", name='" + getName() + '\'' +
                    ", module='" + getModule() + '\'' +
                    ", moduleOptions=" + getModuleOptions().keySet() +
                    '}';
        }
    }

    public static class AuthenticationJaspi {
        private final List<LoginModuleStack> loginModulesStacks = new ArrayList<>();
        private final List<AuthModule> authModules = new ArrayList<>();

        public List<AuthModule> getAuthModules() {
            return authModules;
        }

        public List<LoginModuleStack> getLoginModulesStacks() {
            return loginModulesStacks;
        }

        @Override
        public String toString() {
            return "AuthenticationJaspi{" +
                    "loginModulesStacks=" + loginModulesStacks +
                    ", authModules=" + authModules +
                    '}';
        }
    }

    public static class LoginModuleStack {
        private final List<LoginModule> loginModules = new ArrayList<>();
        private final String name;
        public LoginModuleStack(String name) {
            this.name = Objects.requireNonNull(name);
        }
        public String getName() {
            return name;
        }
        public List<LoginModule> getLoginModules() {
            return loginModules;
        }

        @Override
        public String toString() {
            return "LoginModuleStack{" +
                    "loginModules=" + loginModules +
                    ", name='" + name + '\'' +
                    '}';
        }
    }

    public static class AuthModule extends Module {
        private String loginModuleStackRef;
        public String getLoginModuleStackRef() {
            return loginModuleStackRef;
        }
        public void setLoginModuleStackRef(String loginModuleStackRef) {
            this.loginModuleStackRef = loginModuleStackRef;
        }
        @Override
        public String toString() {
            return "AuthModule{" +
                    "code='" + getCode() + '\'' +
                    ", flag='" + getFlag() + '\'' +
                    ", name='" + getName() + '\'' +
                    ", module='" + getModule() + '\'' +
                    ", loginModuleStackRef='" + getLoginModuleStackRef() + '\'' +
                    ", moduleOptions=" + getModuleOptions().keySet() +
                    '}';
        }
    }

    public static class Authorization {
        private final List<PolicyModule> policyModules = new ArrayList<>();
        public List<PolicyModule> getPolicyModules() {
            return policyModules;
        }
        @Override
        public String toString() {
            return "Authorization{" +
                    "policyModules=" + policyModules +
                    '}';
        }
    }

    public static class PolicyModule extends Module {
        @Override
        public String toString() {
            return "PolicyModule{" +
                    "code='" + getCode() + '\'' +
                    ", flag='" + getFlag() + '\'' +
                    ", name='" + getName() + '\'' +
                    ", module='" + getModule() + '\'' +
                    ", moduleOptions=" + getModuleOptions().keySet() +
                    '}';
        }
    }
}
