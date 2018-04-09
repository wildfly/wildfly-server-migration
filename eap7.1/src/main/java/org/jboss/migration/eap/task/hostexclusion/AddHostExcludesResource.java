/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.migration.eap.task.hostexclusion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wangc
 *
 */
public class AddHostExcludesResource<S> extends org.jboss.migration.wfly11.task.hostexclusion.AddHostExcludesResource<S> {

    public static final String EAP62_NAME = "EAP62";
    public static final String EAP63_NAME = "EAP63";
    public static final String EAP64_NAME = "EAP64";
    public static final String EAP64z_NAME = "EAP64z";
    public static final String EAP70_NAME = "EAP70";
    public static final String EAP62_ID = "EAP6.2";
    public static final String EAP63_ID = "EAP6.3";
    public static final String EAP64_ID = "EAP6.4";
    public static final String EAP64z_ID = null;
    public static final String EAP70_ID = "EAP7.0";
    public static final String[] EAP62_API_VERSION = null;
    public static final String[] EAP63_API_VERSION = null;
    public static final String[] EAP64_API_VERSION = null;
    public static final String[] EAP64z_API_VERSION = {"1", "8"};
    public static final String[] EAP70_API_VERSION = null;

    public static final List<String> HOST_EXCLUDES_NAMES = new ArrayList<>();
    public static final Map<String, String> HOST_RELEASES_NAMES = new HashMap<>();
    public static final Map<String, String[]> HOST_RELEASES_API_VERSIONS = new HashMap<>();
    public static final Map<String, String[]> HOST_EXCLUDED_EXTENSIONS = new HashMap<>();

    public static final String[] EAP62_EXCLUDED_EXTENSIONS = {
            "org.wildfly.extension.batch.jberet",
            "org.wildfly.extension.bean-validation",
            "org.wildfly.extension.clustering.singleton",
            "org.wildfly.extension.core-management",
            "org.wildfly.extension.discovery",
            "org.wildfly.extension.elytron",
            "org.wildfly.extension.io",
            "org.wildfly.extension.messaging-activemq",
            "org.wildfly.extension.request-controller",
            "org.wildfly.extension.security.manager",
            "org.wildfly.extension.undertow",
            "org.wildfly.iiop-openjdk"
            };

    public static final String[] EAP63_EXCLUDED_EXTENSIONS = {
            "org.wildfly.extension.batch.jberet",
            "org.wildfly.extension.bean-validation",
            "org.wildfly.extension.clustering.singleton",
            "org.wildfly.extension.core-management",
            "org.wildfly.extension.discovery",
            "org.wildfly.extension.elytron",
            "org.wildfly.extension.io",
            "org.wildfly.extension.messaging-activemq",
            "org.wildfly.extension.request-controller",
            "org.wildfly.extension.security.manager",
            "org.wildfly.extension.undertow",
            "org.wildfly.iiop-openjdk"
            };

    public static final String[] EAP64_EXCLUDED_EXTENSIONS = {
            "org.wildfly.extension.batch.jberet",
            "org.wildfly.extension.bean-validation",
            "org.wildfly.extension.clustering.singleton",
            "org.wildfly.extension.core-management",
            "org.wildfly.extension.discovery",
            "org.wildfly.extension.elytron",
            "org.wildfly.extension.io",
            "org.wildfly.extension.messaging-activemq",
            "org.wildfly.extension.request-controller",
            "org.wildfly.extension.security.manager",
            "org.wildfly.extension.undertow",
            "org.wildfly.iiop-openjdk"
            };

    public static final String[] EAP64z_EXCLUDED_EXTENSIONS = {
            "org.wildfly.extension.batch.jberet",
            "org.wildfly.extension.bean-validation",
            "org.wildfly.extension.clustering.singleton",
            "org.wildfly.extension.core-management",
            "org.wildfly.extension.discovery",
            "org.wildfly.extension.elytron",
            "org.wildfly.extension.io",
            "org.wildfly.extension.messaging-activemq",
            "org.wildfly.extension.request-controller",
            "org.wildfly.extension.security.manager",
            "org.wildfly.extension.undertow",
            "org.wildfly.iiop-openjdk"
            };

    public static final String[] EAP70_EXCLUDED_EXTENSIONS = {
            "org.wildfly.extension.core-management",
            "org.wildfly.extension.discovery",
            "org.wildfly.extension.elytron"
    };

    static {
        HOST_EXCLUDES_NAMES.add(EAP62_NAME);
        HOST_EXCLUDES_NAMES.add(EAP63_NAME);
        HOST_EXCLUDES_NAMES.add(EAP64_NAME);
        HOST_EXCLUDES_NAMES.add(EAP64z_NAME);
        HOST_EXCLUDES_NAMES.add(EAP70_NAME);
        HOST_RELEASES_NAMES.put(EAP62_NAME, EAP62_ID);
        HOST_RELEASES_NAMES.put(EAP63_NAME, EAP63_ID);
        HOST_RELEASES_NAMES.put(EAP64_NAME, EAP64_ID);
        HOST_RELEASES_NAMES.put(EAP64z_NAME, EAP64z_ID);
        HOST_RELEASES_NAMES.put(EAP70_NAME, EAP70_ID);
        HOST_RELEASES_API_VERSIONS.put(EAP62_NAME, EAP62_API_VERSION);
        HOST_RELEASES_API_VERSIONS.put(EAP63_NAME, EAP63_API_VERSION);
        HOST_RELEASES_API_VERSIONS.put(EAP64_NAME, EAP64_API_VERSION);
        HOST_RELEASES_API_VERSIONS.put(EAP64z_NAME, EAP64z_API_VERSION);
        HOST_RELEASES_API_VERSIONS.put(EAP70_NAME, EAP70_API_VERSION);
        HOST_EXCLUDED_EXTENSIONS.put(EAP62_NAME, EAP62_EXCLUDED_EXTENSIONS);
        HOST_EXCLUDED_EXTENSIONS.put(EAP63_NAME, EAP63_EXCLUDED_EXTENSIONS);
        HOST_EXCLUDED_EXTENSIONS.put(EAP64_NAME, EAP64_EXCLUDED_EXTENSIONS);
        HOST_EXCLUDED_EXTENSIONS.put(EAP64z_NAME, EAP64z_EXCLUDED_EXTENSIONS);
        HOST_EXCLUDED_EXTENSIONS.put(EAP70_NAME, EAP70_EXCLUDED_EXTENSIONS);
    }

    @Override
    public List<String> getHostExcludesNames() {
        return org.jboss.migration.eap.task.hostexclusion.AddHostExcludesResource.HOST_EXCLUDES_NAMES;
    }

    @Override
    public Map<String, String> getHostReleaseNames() {
        return org.jboss.migration.eap.task.hostexclusion.AddHostExcludesResource.HOST_RELEASES_NAMES;
    }

    @Override
    public Map<String, String[]> getHostReleasesApiVersions() {
        return org.jboss.migration.eap.task.hostexclusion.AddHostExcludesResource.HOST_RELEASES_API_VERSIONS;
    }

    @Override
    public Map<String, String[]> getHostExcludedExtensions() {
        return org.jboss.migration.eap.task.hostexclusion.AddHostExcludesResource.HOST_EXCLUDED_EXTENSIONS;
    }
}
