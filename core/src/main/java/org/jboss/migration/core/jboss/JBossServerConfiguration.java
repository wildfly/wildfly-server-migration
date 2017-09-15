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

package org.jboss.migration.core.jboss;

import org.jboss.migration.core.ServerPath;

import java.nio.file.Path;

/**
 * @author emmartins
 */
public class JBossServerConfiguration<S extends JBossServer<S>> extends ServerPath<S> implements AbsolutePathResolver {

    public enum Type {
        STANDALONE,
        DOMAIN,
        HOST
    }

    private final Type configurationType;

    public JBossServerConfiguration(Path path, Type configurationType, S server) {
        super(path, server);
        this.configurationType = configurationType;
    }

    @Override
    public S getServer() {
        return super.getServer();
    }

    public Path getConfigurationDir() {
        return getServer().getConfigurationDir(configurationType);
    }

    public Path getDataDir() {
        return getServer().getDataDir(configurationType);
    }

    public Path getContentDir() {
        return getServer().getContentDir(configurationType);
    }

    @Override
    public Path resolveNamedPath(String path) {
        return getServer().resolveNamedPath(path);
    }

    @Override
    public Path resolvePath(String path, String relativeTo) {
        return getServer().resolvePath(path, relativeTo);
    }
}
