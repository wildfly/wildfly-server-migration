/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.test.cache;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Clones or updates the {@code wildfly/wildfly} {@code main} branch, builds it,
 * and copies the produced {@code dist/target/wildfly-*} directory into the cache
 * under its original name (e.g. {@code wildfly-42.0.0.Beta1-SNAPSHOT}).
 *
 * <p>The working clone lives at {@value #CLONE_DIR_NAME} directory, in {@code <cacheDir>}, and is
 * reused across builds. The cached distribution is stored at
 * {@code <cacheDir>/wildfly-<built-version>/}.</p>
 */
public class MainBranchBuilder {

    private static final String WILDFLY_REPO = "https://github.com/wildfly/wildfly.git";
    private static final String CLONE_DIR_NAME = ".src";

    private final Path cacheDir;

    public MainBranchBuilder(Path cacheDir) {
        this.cacheDir = cacheDir;
    }

    /**
     * Ensures the main-branch distribution for {@code majorMinor} (e.g. {@code "42.0"}) is
     * cached. Clones if the working directory does not exist. Checks for new commits; rebuilds
     * and replaces the cached copy if there are changes or no cached copy exists.
     *
     * @return the cached distribution directory
     */
    public Path getOrBuild(String majorMinor) throws IOException, InterruptedException {
        Files.createDirectories(cacheDir);
        Path cloneDir = cacheDir.resolve(CLONE_DIR_NAME);

        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            runCommand(cacheDir, "git", "config", "--system", "core.longpaths", "true");
        }

        if (!Files.isDirectory(cloneDir)) {
            System.out.println("[MainBranchBuilder] Cloning " + WILDFLY_REPO);
            runCommand(cacheDir, "git", "clone", "--depth=1", WILDFLY_REPO, CLONE_DIR_NAME);
        } else {
            System.out.println("[MainBranchBuilder] Fetching latest commits for main");
            runCommand(cloneDir, "git", "fetch", "--depth=1", "origin", "main");
        }

        // Find the cached distribution (if any) for this major.minor
        Path cached = ServerCacheLookup.findInCache(cacheDir, majorMinor);

        // Check whether origin/main has commits not reflected in the local HEAD
        boolean hasNewCommits = hasNewCommits(cloneDir);

        if (cached != null && !hasNewCommits) {
            System.out.println("[MainBranchBuilder] No new commits; reusing cached " + cached.getFileName());
            return cached;
        }

        // Apply the fetched commits
        runCommand(cloneDir, "git", "reset", "--hard", "origin/main");

        System.out.println("[MainBranchBuilder] Building WildFly main (this may take several minutes)...");
        runCommand(cloneDir, mvnCommand(), "-B", "install", "-DskipTests",
                "-Denforcer.skip=true", "-Dcheckstyle.skip=true");

        // Locate the built dist dir (wildfly-<version>/)
        Path builtDist = findBuiltDist(cloneDir);

        // Remove old cached copy for this major.minor if it exists
        if (cached != null) {
            System.out.println("[MainBranchBuilder] Replacing cached " + cached.getFileName()
                    + " with " + builtDist.getFileName());
            deleteDirectory(cached);
        }

        // Copy the built distribution into the cache preserving its original directory name
        Path dest = cacheDir.resolve(builtDist.getFileName());
        copyDirectory(builtDist, dest);
        System.out.println("[MainBranchBuilder] Cached " + dest);
        return dest;
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /** Returns {@code true} if {@code origin/main} is ahead of the local HEAD. */
    private static boolean hasNewCommits(Path cloneDir) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("git", "rev-list", "--count", "HEAD..origin/main")
                .directory(cloneDir.toFile())
                .redirectErrorStream(true);
        Process p = pb.start();
        String out = new String(p.getInputStream().readAllBytes()).trim();
        p.waitFor();
        try {
            return Integer.parseInt(out) > 0;
        } catch (NumberFormatException e) {
            // If we can't parse, assume there may be changes
            return true;
        }
    }

    /** Finds the first {@code wildfly-*} directory inside {@code cloneDir/dist/target/}. */
    private static Path findBuiltDist(Path cloneDir) throws IOException {
        Path distTarget = cloneDir.resolve("dist/target");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(distTarget, "wildfly-*")) {
            List<Path> candidates = new ArrayList<>();
            for (Path p : stream) {
                if (Files.isDirectory(p) && !p.getFileName().toString().endsWith(".jar")) {
                    candidates.add(p);
                }
            }
            if (candidates.isEmpty()) {
                throw new IOException("No wildfly-* distribution found under " + distTarget);
            }
            if (candidates.size() > 1) {
                // Pick the most recently modified
                candidates.sort((a, b) -> {
                    try {
                        BasicFileAttributes attrA = Files.readAttributes(a, BasicFileAttributes.class);
                        BasicFileAttributes attrB = Files.readAttributes(b, BasicFileAttributes.class);
                        return attrB.lastModifiedTime().compareTo(attrA.lastModifiedTime());
                    } catch (IOException e) {
                        return 0;
                    }
                });
            }
            return candidates.get(0);
        }
    }

    private static void runCommand(Path workDir, String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command)
                .directory(workDir.toFile())
                .inheritIO();
        int exitCode = pb.start().waitFor();
        if (exitCode != 0) {
            throw new IOException("Command failed [" + exitCode + "]: " + String.join(" ", command));
        }
    }

    private static String mvnCommand() {
        return System.getProperty("os.name", "").toLowerCase().contains("win") ? "mvn.cmd" : "mvn";
    }

    private static void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        Files.walk(dir)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(p -> {
                    try { Files.delete(p); } catch (IOException ignored) {}
                });
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        Files.createDirectories(target);
        Files.walk(source).forEach(src -> {
            try {
                Path dst = target.resolve(source.relativize(src));
                if (Files.isDirectory(src)) {
                    Files.createDirectories(dst);
                } else {
                    Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
