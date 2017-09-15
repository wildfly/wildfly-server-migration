/*
 * Copyright 2016 Red Hat, Inc.
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

package org.jboss.migration.wfly10.config.task.update;

import org.jboss.migration.core.jboss.JBossServer;
import org.jboss.migration.core.jboss.JBossServerConfiguration;
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.wfly10.WildFlyServer10;
import org.jboss.migration.wfly10.config.task.ServerConfigurationMigration;

import java.nio.file.Path;

/**
 * The xml config factory for WildFly10 updates, simply copies the config file to target server.
 * @author emmartins
 */
public class CopySourceXMLConfiguration<S extends JBossServer<S>> implements ServerConfigurationMigration.XMLConfigurationProvider<JBossServerConfiguration<S>> {
    @Override
    public JBossServerConfiguration getXMLConfiguration(JBossServerConfiguration<S> source, JBossServerConfiguration.Type targetConfigurationType, WildFlyServer10 target, TaskContext context) {
        final Path targetConfigFilePath = target.getConfigurationDir(targetConfigurationType).resolve(source.getPath().getFileName());
        context.getLogger().tracef("Target configuration file is %s", targetConfigFilePath);
        // copy xml from source to target
        context.getMigrationFiles().copy(source.getPath(), targetConfigFilePath);
        context.getLogger().debugf("Source XML configuration copied to target server ( %s ).", targetConfigFilePath);
        return new JBossServerConfiguration(targetConfigFilePath, targetConfigurationType, target);
    }
}
