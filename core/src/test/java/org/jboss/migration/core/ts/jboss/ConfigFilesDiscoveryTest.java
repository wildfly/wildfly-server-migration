/*
 * Copyright 2016 Red Hat, Inc.
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
package org.jboss.migration.core.ts.jboss;

import org.jboss.migration.core.ServerPath;
import org.jboss.migration.core.env.MigrationEnvironment;
import org.jboss.migration.core.jboss.JBossServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConfigFilesDiscoveryTest {
    private static final String STANDALONE_CONFIGS_PROP = JBossServer.Environment.getFullEnvironmentPropertyName(TestJBossServer.MIGRATION_NAME, JBossServer.Environment.PROPERTY_STANDALONE_CONFIG_FILES);

    @Rule
    public final TemporaryFolder tmp = new TemporaryFolder();

    private Path baseDir;
    private Path standaloneConfigDir;

    @Before
    public void prepareStandaloneConfigDir() throws IOException {
        baseDir = tmp.getRoot().toPath();
        standaloneConfigDir = tmp.newFolder("standalone", "configuration").toPath();

        byte[] standaloneConfigFile = "<server xmlns=\"urn:jboss:domain:1.7\" />".getBytes(StandardCharsets.UTF_8);
        Files.write(standaloneConfigDir.resolve("standalone.xml"), standaloneConfigFile);
        Files.write(standaloneConfigDir.resolve("foobar.xml"), standaloneConfigFile);
        Files.write(standaloneConfigDir.resolve("whatever.xml"), standaloneConfigFile);
    }

    private Set<Path> discoverStandaloneConfigFiles(MigrationEnvironment migrationEnvironment) {
        JBossServer server = new TestJBossServer(baseDir, migrationEnvironment);

        Collection<ServerPath> standaloneConfigs = server.getStandaloneConfigs();

        Set<Path> paths = new HashSet<>();
        for (ServerPath serverPath : standaloneConfigs) {
            paths.add(serverPath.getPath());
        }

        return paths;
    }

    @Test
    public void standalone_automatic() throws IOException {
        Set<Path> paths = discoverStandaloneConfigFiles(new MigrationEnvironment());

        assertEquals(3, paths.size());
        assertTrue(paths.contains(standaloneConfigDir.resolve("standalone.xml")));
        assertTrue(paths.contains(standaloneConfigDir.resolve("foobar.xml")));
        assertTrue(paths.contains(standaloneConfigDir.resolve("whatever.xml")));
    }

    @Test
    public void standalone_property_nothing() {
        MigrationEnvironment env = new MigrationEnvironment();
        env.setProperty(STANDALONE_CONFIGS_PROP, "");
        Set<Path> paths = discoverStandaloneConfigFiles(env);

        // as if the property wasn't even passed
        assertEquals(3, paths.size());
    }

    @Test
    public void standalone_property_absolutePaths() {
        MigrationEnvironment env = new MigrationEnvironment();
        env.setProperty(STANDALONE_CONFIGS_PROP, "" + standaloneConfigDir.resolve("standalone.xml").toAbsolutePath()
                + "," + standaloneConfigDir.resolve("foobar.xml").toAbsolutePath());
        try {
            discoverStandaloneConfigFiles(env);
        } catch (IllegalArgumentException e) {
            // expected, absolute paths in config are not supported
        }
    }

    @Test
    public void standalone_property_relativePaths() {
        MigrationEnvironment env = new MigrationEnvironment();
        env.setProperty(STANDALONE_CONFIGS_PROP, "standalone.xml,foobar.xml");
        Set<Path> paths = discoverStandaloneConfigFiles(env);

        assertEquals(2, paths.size());
        assertTrue(paths.contains(standaloneConfigDir.resolve("standalone.xml")));
        assertTrue(paths.contains(standaloneConfigDir.resolve("foobar.xml")));
    }

    @Test
    public void standalone_property_mixedPaths() {
        MigrationEnvironment env = new MigrationEnvironment();
        env.setProperty(STANDALONE_CONFIGS_PROP, "standalone.xml,"
                + standaloneConfigDir.resolve("foobar.xml").toAbsolutePath());
        try {
            discoverStandaloneConfigFiles(env);
        } catch (IllegalArgumentException e) {
            // expected, absolute paths in config are not supported
        }
    }
}
