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
package org.jboss.migration.core;

import org.jboss.migration.core.logger.ServerMigrationLogger;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * The migration files.
 * @author emmartins
 */
public class MigrationFiles {

    private final Map<Path, Path> copiedFiles;

    public MigrationFiles() {
        this.copiedFiles = new HashMap<>();
    }

    /**
     * Copy a path.
     * @param source the source path
     * @param target the target path
     * @throws IllegalArgumentException if the source does not exists or any of the paths is not absolute
     * @throws ServerMigrationFailureException if the path copy failed
     */
    public synchronized void copy(final Path source, final Path target) throws IllegalArgumentException, ServerMigrationFailureException {
        if (!Files.exists(source)) {
            throw new IllegalArgumentException("Source path "+source+" does not exists.");
        }
        if (!source.isAbsolute()) {
            throw new IllegalArgumentException("Source path "+source+" is not an absolute path.");
        }
        if (!target.isAbsolute()) {
            throw new IllegalArgumentException("Target path "+target+" is not an absolute path.");
        }
        try {
            createDirectories(target.getParent());
            if (Files.isDirectory(source)) {
                copyDir(source, target, copiedFiles);
            } else {
                copyFile(source, target, copiedFiles);
            }
        } catch (IOException e) {
            throw new ServerMigrationFailureException("Failed to copy "+source+" to "+target, e);
        }
    }

    private static final CopyOption[] COPY_FILE_OPTIONS = { COPY_ATTRIBUTES, REPLACE_EXISTING };
    private static final CopyOption[] BACKUP_FILE_OPTIONS = { REPLACE_EXISTING };

    private static void createDirectories(Path path) throws IOException {
        if (!Files.isDirectory(path)) {
            // get the last existing parent
            Path dir = path.getParent();
            while (dir != null && !Files.exists(dir)) {
                dir = dir.getParent();
            }
            if (dir == null || !Files.isDirectory(dir)) {
                throw new IOException("Invalid parent directory: " + dir);
            }
            // create all the directories that are missing
            Path child = dir;
            for (Path name : dir.relativize(path)) {
                child = child.resolve(name);
                Files.createDirectory(child);
            }
        }
    }

    private static void copyFile(Path source, Path target, Map<Path, Path> copiedFiles) throws IOException {
        if (copiedFiles.put(target, source) == null) {
            if (Files.exists(target)) {
                // backup
                ServerMigrationLogger.ROOT_LOGGER.tracef("Backing up target %s before copy", target);
                final Path backup = target.resolveSibling(target.getFileName().toString()+".beforeMigration");
                Files.move(target, backup, BACKUP_FILE_OPTIONS);
            }
        }
        ServerMigrationLogger.ROOT_LOGGER.tracef("Copying file %s to %s", source, target);
        Files.copy(source, target, COPY_FILE_OPTIONS);
        ServerMigrationLogger.ROOT_LOGGER.tracef("File %s copied to %s.", source, target);
    }

    private static void copyDir(Path source, Path target, Map<Path, Path> copiedFiles) throws IOException {
        Files.walkFileTree(source, CopyVisitor.FILE_VISITOR_OPTIONS, Integer.MAX_VALUE, new CopyVisitor(source, target, copiedFiles));
    }

    private static class CopyVisitor extends SimpleFileVisitor<Path> {

        static final EnumSet<FileVisitOption> FILE_VISITOR_OPTIONS = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
        static final CopyOption[] COPY_DIR_OPTIONS = new CopyOption[] { COPY_ATTRIBUTES };

        private final Map<Path, Path> copiedFiles;
        private final Path source;
        private final Path target;

        CopyVisitor(Path source, Path target, Map<Path, Path> copiedFiles) {
            this.source = source;
            this.target = target;
            this.copiedFiles = copiedFiles;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path sourceDir, BasicFileAttributes attrs) throws IOException {
            Path targetDir = getTargetPath(sourceDir);
            try {
                Files.copy(sourceDir, targetDir, COPY_DIR_OPTIONS);
            } catch (FileAlreadyExistsException e) {
                if (!Files.isDirectory(targetDir)) {
                    throw e;
                }
            }
            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path sourceFile, BasicFileAttributes attrs) throws IOException {
            final Path targetFile = getTargetPath(sourceFile);
            copyFile(sourceFile, targetFile, copiedFiles);
            return CONTINUE;
        }

        private Path getTargetPath(Path sourcePath) {
            return target.resolve(source.relativize(sourcePath));
        }

        @Override
        public FileVisitResult postVisitDirectory(Path sourceDir, IOException e) throws IOException {
            if (e == null) {
                final Path targetDir = getTargetPath(sourceDir);
                Files.setLastModifiedTime(targetDir, Files.getLastModifiedTime(sourceDir));
            }
            return CONTINUE;
        }
    }
}