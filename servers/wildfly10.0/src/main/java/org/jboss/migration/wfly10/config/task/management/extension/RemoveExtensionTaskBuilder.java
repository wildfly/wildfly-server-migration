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

package org.jboss.migration.wfly10.config.task.management.extension;

import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.wfly10.config.management.ManageableServerConfiguration;
import org.jboss.migration.wfly10.config.task.management.configuration.ManageableServerConfigurationLeafTask;

/**
 * A task which removes an extension from a server configuration.
 * @author emmartins
 */
public class RemoveExtensionTaskBuilder<S> extends ManageableServerConfigurationLeafTask.Builder<S> {
    public RemoveExtensionTaskBuilder(String extensionModule) {
        name("extension."+extensionModule+".remove");
        runBuilder(params -> context -> {
            final ManageableServerConfiguration extensionResourceParent = params.getServerConfiguration();
            if (extensionResourceParent.getExtensionResourceNames().contains(extensionModule)) {
                extensionResourceParent.removeExtensionResource(extensionModule);
                context.getLogger().infof("Extension %s removed.",extensionModule);
                return ServerMigrationTaskResult.SUCCESS;
            } else {
                context.getLogger().debugf("Skipped extension %s removal, doesn't exists in config.", extensionModule);
                return ServerMigrationTaskResult.SKIPPED;
            }
        });
    }
}
