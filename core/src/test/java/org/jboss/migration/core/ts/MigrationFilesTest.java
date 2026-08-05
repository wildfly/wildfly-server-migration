/*
 * Copyright 2020 Red Hat, Inc.
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

package org.jboss.migration.core.ts;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import org.jboss.migration.core.MigrationFiles;
import org.jboss.migration.core.ServerMigration;
import org.jboss.migration.core.ServerMigrationFailureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simple test class for MigrationFiles copy method.
 *
 * @author rmartinc
 */
public class MigrationFilesTest {

    @TempDir
    Path tmp;

    private final Random random = new Random();

    private final MigrationFiles migrationFiles = new ServerMigration()
            .from(TestSourceServerProvider.SERVER.getBaseDir())
            .to(TestTargetServerProvider.SERVER.getBaseDir())
            .run()
            .getRootTask()
            .getServerMigrationContext()
            .getMigrationFiles();

    private Path createNewFile(String name, int size) throws IOException {
        Path file = tmp.resolve(name);
        return createNewFile(file, size);
    }

    private Path createNewFile(Path file, int size) throws IOException {
        try (OutputStream os = Files.newOutputStream(file)) {
            byte[] bytes = new byte[size];
            random.nextBytes(bytes);
            os.write(bytes);
        }
        return file;
    }

    @Test
    public void copyFile() throws IOException {
        Path source = createNewFile("source.bin", 32);
        Path target = tmp.resolve("target.bin");

        migrationFiles.copy(source, target);

        assertTrue(Files.exists(target), "Target file is created");
        assertArrayEquals(Files.readAllBytes(source), Files.readAllBytes(target), "Contents of the files are OK");
    }

    @Test
    public void copyFileSeveralLevels() throws IOException {
        Path source = createNewFile("source.bin", 32);
        Path target = tmp.resolve("level1").resolve("level2").resolve("target.bin");

        migrationFiles.copy(source, target);

        assertTrue(Files.exists(target), "Target file is created");
        assertArrayEquals(Files.readAllBytes(source), Files.readAllBytes(target), "Contents of the files are OK");
    }

    @Test
    public void copyFileBackup() throws IOException {
        Path source = createNewFile("source.bin", 32);
        Path target = createNewFile("target.bin", 32);

        migrationFiles.copy(source, target);

        assertTrue(Files.exists(target), "Target file is created");
        assertArrayEquals(Files.readAllBytes(source), Files.readAllBytes(target), "Contents of the files are OK");
        assertTrue(Files.exists(tmp.resolve("target.bin.beforeMigration")), "Backup file is created");
    }

    @Test
    public void copyFileWithALink() throws IOException {
        Path source = createNewFile("source.bin", 32);
        Path realTargetDir = Files.createDirectory(tmp.resolve("realLevel1"));
        Path linkTargetDir = tmp.resolve("linkLevel1");
        Files.createSymbolicLink(linkTargetDir, realTargetDir);
        Path target = linkTargetDir.resolve("target.bin");

        migrationFiles.copy(source, target);

        assertTrue(Files.exists(target), "Target file is created");
        assertArrayEquals(Files.readAllBytes(source), Files.readAllBytes(target), "Contents of the files are OK");
    }

    @Test
    public void copyFileWithALinkSeveralLevels() throws IOException {
        Path source = createNewFile("source.bin", 32);
        Path realTargetDir = Files.createDirectory(tmp.resolve("realLevel1"));
        Path linkTargetDir = tmp.resolve("linkLevel1");
        Files.createSymbolicLink(linkTargetDir, realTargetDir);
        Path target = linkTargetDir.resolve("level2").resolve("level3").resolve("target.bin");

        migrationFiles.copy(source, target);

        assertTrue(Files.exists(target), "Target file is created");
        assertArrayEquals(Files.readAllBytes(source), Files.readAllBytes(target), "Contents of the files are OK");
    }

    @Test
    public void copyDirectory() throws IOException {
        Path sourceDir = Files.createDirectory(tmp.resolve("source"));
        Path source1 = createNewFile(sourceDir.resolve("source1.bin"), 32);
        Path source2 = createNewFile(sourceDir.resolve("source2.bin"), 32);
        Path levels = sourceDir.resolve("level1").resolve("level2");
        Files.createDirectories(levels);
        Path source3 = createNewFile(levels.resolve("source3.bin"), 32);
        Path targetDir = Files.createDirectory(tmp.resolve("target"));

        migrationFiles.copy(sourceDir, targetDir);

        assertTrue(Files.exists(targetDir.resolve("source1.bin")), "Target source1.bin file is created");
        assertArrayEquals(Files.readAllBytes(source1), Files.readAllBytes(targetDir.resolve("source1.bin")), "Contents of source1.bin are OK");
        assertTrue(Files.exists(targetDir.resolve("source2.bin")), "Target source2.bin file is created");
        assertArrayEquals(Files.readAllBytes(source2), Files.readAllBytes(targetDir.resolve("source2.bin")), "Contents of source2.bin are OK");
        assertTrue(Files.exists(targetDir.resolve("level1").resolve("level2").resolve("source3.bin")), "Target source3.bin file is created");
        assertArrayEquals(Files.readAllBytes(source3),
                Files.readAllBytes(targetDir.resolve("level1").resolve("level2").resolve("source3.bin")), "Contents of source3.bin are OK");
    }

    @Test
    public void copyDirectoryAlreadyExists() throws IOException {
        Path sourceDir = Files.createDirectory(tmp.resolve("source"));
        createNewFile(sourceDir.resolve("source1.bin"), 32);
        Path target = createNewFile("target", 0);

        assertThrows(ServerMigrationFailureException.class,
                () -> migrationFiles.copy(sourceDir, target));
    }
}
