/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.wfly10.config.task.paths;

import org.jboss.migration.core.MigrationFiles;
import org.jboss.migration.core.ServerMigrationContext;
import org.jboss.migration.core.console.ConsoleWrapper;
import org.jboss.migration.core.console.JavaConsole;
import org.jboss.migration.core.env.MigrationEnvironment;
import org.jboss.migration.core.jboss.JBossServerConfiguration;
import org.jboss.migration.core.jboss.XmlConfigurationMigration;
import org.jboss.migration.core.task.ServerMigrationTask;
import org.jboss.migration.core.task.ServerMigrationTaskName;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.core.task.TaskExecutionImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Abstract base for unit tests of {@link XmlConfigurationMigration.Component} implementations.
 * <p>
 * Subclasses supply:
 * <ul>
 *   <li>the XML configuration content to parse (written into the target server's config dir),</li>
 *   <li>the {@link XmlConfigurationMigration.ComponentFactory} under test,</li>
 *   <li>the list of paths — relative to the server base dir — that must be copied from source to target.</li>
 * </ul>
 * The test creates dummy files for each expected path under the source server tree, runs the
 * component through {@link XmlConfigurationMigration}, and asserts that every expected file
 * was copied to the matching location in the target server tree.
 *
 * @author emmartins
 */
public abstract class AbstractXmlConfigurationMigrationComponentTest {

    @TempDir
    Path tmp;

    /**
     * @return the XML content to write to the target server's standalone config file.
     */
    protected abstract String getXmlConfig();

    /**
     * @return the component factory under test.
     */
    protected abstract XmlConfigurationMigration.ComponentFactory getComponentFactory();

    /**
     * @return paths relative to the server base dir that the component is expected to copy
     *         from source to target (e.g. {@code "standalone/data/vault.keystore"}).
     */
    protected abstract List<String> getExpectedCopiedPaths();

    @Test
    public void testExpectedPathsAreCopied() throws IOException {
        // --- set up source server tree -----------------------------------------
        final Path sourceBase = Files.createDirectories(tmp.resolve("source"));
        Files.createDirectories(sourceBase.resolve("standalone").resolve("configuration"));
        for (String relativePath : getExpectedCopiedPaths()) {
            final Path file = sourceBase.resolve(relativePath);
            Files.createDirectories(file.getParent());
            Files.write(file, "dummy".getBytes(StandardCharsets.UTF_8));
        }

        // --- set up target server tree -----------------------------------------
        final Path targetBase = Files.createDirectories(tmp.resolve("target"));
        final Path targetConfigDir = Files.createDirectories(
                targetBase.resolve("standalone").resolve("configuration"));
        final Path configFile = targetConfigDir.resolve("standalone.xml");
        Files.write(configFile, getXmlConfig().getBytes(StandardCharsets.UTF_8));

        // --- build JBossServer / JBossServerConfiguration instances ------------
        final MigrationEnvironment env = new MigrationEnvironment();
        final TestJBossServerForPaths sourceServer = new TestJBossServerForPaths(sourceBase, env);
        final TestJBossServerForPaths targetServer = new TestJBossServerForPaths(targetBase, env);

        final JBossServerConfiguration<TestJBossServerForPaths> sourceConfig =
                new JBossServerConfiguration<>(configFile, JBossServerConfiguration.Type.STANDALONE, sourceServer);
        final JBossServerConfiguration<TestJBossServerForPaths> targetConfig =
                new JBossServerConfiguration<>(configFile, JBossServerConfiguration.Type.STANDALONE, targetServer);

        // --- build and run XmlConfigurationMigration ---------------------------
        final XmlConfigurationMigration<TestJBossServerForPaths> migration =
                new XmlConfigurationMigration.Builder<TestJBossServerForPaths>()
                        .componentFactory(getComponentFactory())
                        .build(sourceConfig, targetConfig);

        final ConsoleWrapper console = new JavaConsole();
        final ServerMigrationContext migrationContext = new ServerMigrationContext() {
            private final MigrationFiles migrationFiles = new MigrationFiles();
            @Override
            public ConsoleWrapper getConsoleWrapper() {
                return console;
            }
            @Override
            public MigrationFiles getMigrationFiles() {
                return migrationFiles;
            }
            @Override
            public boolean isInteractive() {
                return false;
            }
            @Override
            public MigrationEnvironment getMigrationEnvironment() {
                return env;
            }
        };

        final ServerMigrationTask task = new ServerMigrationTask() {
            @Override
            public ServerMigrationTaskName getName() {
                return new ServerMigrationTaskName.Builder("paths.migrate-paths-requested-by-configuration")
                        .addAttribute("path", configFile.toString()).build();
            }
            @Override
            public ServerMigrationTaskResult run(TaskContext context) {
                return migration.run(context);
            }
        };

        new TaskExecutionImpl(task, migrationContext).run();

        // --- assert all expected files exist in the target ---------------------
        for (String relativePath : getExpectedCopiedPaths()) {
            final Path expected = targetBase.resolve(relativePath);
            assertTrue(Files.exists(expected),
                    "Expected file was not copied to target: " + relativePath);
        }
    }
}
