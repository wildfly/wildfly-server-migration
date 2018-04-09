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

package org.jboss.migration.wfly11.task.hostexclusion;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXCLUDED_EXTENSIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST_EXCLUDE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MAJOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MINOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST_RELEASE;
import static org.jboss.migration.core.task.component.TaskSkipPolicy.skipIfDefaultTaskSkipPropertyIsSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;
import org.jboss.migration.core.task.ServerMigrationTaskName;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.wfly10.config.management.ManageableResource;
import org.jboss.migration.wfly10.config.management.ManageableServerConfiguration;
import org.jboss.migration.wfly10.config.task.management.configuration.ManageableServerConfigurationLeafTask;
import org.jboss.migration.wfly10.config.task.management.resources.ManageableResourcesCompositeSubtasks;
import org.jboss.migration.wfly10.config.task.management.resources.ManageableResourcesCompositeTask;

/**
 * @author wangc
 *
 */
public class AddHostExcludesResource<S> extends ManageableResourcesCompositeTask.Builder<S, ManageableResource> {

    public static final String WILDFLY100_NAME = "WildFly10.0";
    public static final String WILDFLY101_NAME = "WildFly10.1";
    public static final String WILDFLY100_ID = "WildFly10.0";
    public static final String WILDFLY101_ID = "WildFly10.1";
    public static final String[] WILDFLY100_API_VERSION = null;
    public static final String[] WILDFLY101_API_VERSION = null;
    public static final List<String> HOST_EXCLUDES_NAMES = new ArrayList<>();
    public static final Map<String, String> HOST_RELEASES_NAMES = new HashMap<>();
    public static final Map<String, String[]> HOST_RELEASES_API_VERSIONS = new HashMap<>();
    public static final Map<String, String[]> HOST_EXCLUDED_EXTENSIONS = new HashMap<>();

    public static final String[] WILDFLY100_EXCLUDED_EXTENSIONS = {
        "org.wildfly.extension.core-management",
        "org.wildfly.extension.discovery",
        "org.wildfly.extension.elytron"
        };

    public static final String[] WILDFLY101_EXCLUDED_EXTENSIONS = {
            "org.wildfly.extension.core-management",
            "org.wildfly.extension.discovery",
            "org.wildfly.extension.elytron"
        };

    static {
        HOST_EXCLUDES_NAMES.add(WILDFLY100_NAME);
        HOST_EXCLUDES_NAMES.add(WILDFLY101_NAME);
        HOST_RELEASES_NAMES.put(WILDFLY100_NAME, WILDFLY100_ID);
        HOST_RELEASES_NAMES.put(WILDFLY101_NAME, WILDFLY101_ID);
        HOST_RELEASES_API_VERSIONS.put(WILDFLY100_NAME, WILDFLY100_API_VERSION);
        HOST_RELEASES_API_VERSIONS.put(WILDFLY101_NAME, WILDFLY101_API_VERSION);
        HOST_EXCLUDED_EXTENSIONS.put(WILDFLY100_NAME, WILDFLY100_EXCLUDED_EXTENSIONS);
        HOST_EXCLUDED_EXTENSIONS.put(WILDFLY101_NAME, WILDFLY101_EXCLUDED_EXTENSIONS);
    }

    public AddHostExcludesResource() {
        name("host-excludes.add-exclude");
        skipPolicy(skipIfDefaultTaskSkipPropertyIsSet());
        beforeRun(context -> context.getLogger().debugf("Adding host excludes resources..."));
        subtasks(new ManageableResourcesCompositeSubtasks.Builder<S, ManageableResource>()
                .subtask(new AddHostExcludeResource<S>(getHostExcludesNames())));

        afterRun(context -> {
            if (context.hasSucessfulSubtasks()) {
                context.getLogger().infof("Host exclude resources added.");
            } else {
                context.getLogger().debugf("No host exclude resources added.");
            }
        });

    }

    public class AddHostExcludeResource<S> extends ManageableServerConfigurationLeafTask.Builder<S>  {

        protected AddHostExcludeResource(List<String> hostExcludesNames) {
            nameBuilder(parameters -> new ServerMigrationTaskName.Builder("host-excludes." + ".add-exclude").build());
            skipPolicy(skipIfDefaultTaskSkipPropertyIsSet());
            runBuilder(params -> context -> {
                final ManageableServerConfiguration configuration = params.getServerConfiguration();
                // add host-exclude op with names
                for (String name : hostExcludesNames) {
                    ModelNode addHostExcludeOp = Operations.createAddOperation(PathAddress.pathAddress(HOST_EXCLUDE, name).toModelNode());
                    String releaseName = getHostReleaseNames().get(name);
                    if (releaseName != null) {
                        addHostExcludeOp.get(HOST_RELEASE).set(getHostReleaseNames().get(name));
                    } else {
                        String[] apiVersion = getHostReleasesApiVersions().get(name);
                        addHostExcludeOp.get(MANAGEMENT_MAJOR_VERSION).set(apiVersion[0]);
                        addHostExcludeOp.get(MANAGEMENT_MINOR_VERSION).set(apiVersion[1]);
                    }
                    for (String value : getHostExcludedExtensions().get(name)) {
                        addHostExcludeOp.get(EXCLUDED_EXTENSIONS).add(value);
                    }
                    configuration.executeManagementOperation(addHostExcludeOp);
                    context.getLogger().debugf("Host exclude with name %s is added", name);
                }
                return ServerMigrationTaskResult.SUCCESS;
            });
        }
    }

    public List<String> getHostExcludesNames() {
        return AddHostExcludesResource.HOST_EXCLUDES_NAMES;
    }

    public Map<String, String> getHostReleaseNames() {
        return AddHostExcludesResource.HOST_RELEASES_NAMES;
    }

    public Map<String, String[]> getHostReleasesApiVersions() {
        return AddHostExcludesResource.HOST_RELEASES_API_VERSIONS;
    }

    public Map<String, String[]> getHostExcludedExtensions() {
        return AddHostExcludesResource.HOST_EXCLUDED_EXTENSIONS;
    }
}
