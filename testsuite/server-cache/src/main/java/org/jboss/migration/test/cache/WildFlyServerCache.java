/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.test.cache;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Main entry point for the server cache updater.
 *
 * <p>Discovers all supported WildFly target and source versions from the migrations
 * directory tree, then provisions each one into the cache:</p>
 * <ol>
 *   <li>If a GitHub Final release exists and is newer than what is cached → replace.</li>
 *   <li>If no Final release exists → build from {@code wildfly/wildfly:main} and cache.</li>
 *   <li>If a SNAPSHOT built from main is cached and the branch has new commits → rebuild.</li>
 * </ol>
 *
 * <p>Usage: {@code WildFlyServerCache <migrationsDir> <cacheDir>}</p>
 *
 * <p>The build fails hard (non-zero exit) if any version cannot be provisioned.</p>
 */
public class WildFlyServerCache {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: WildFlyServerCache <migrationsDir> <cacheDir>");
            System.exit(1);
        }
        Path migrationsDir = Paths.get(args[0]).toAbsolutePath().normalize();
        Path cacheDir = Paths.get(args[1]).toAbsolutePath().normalize();
        Files.createDirectories(cacheDir);

        System.out.println("[WildFlyServerCache] migrations : " + migrationsDir);
        System.out.println("[WildFlyServerCache] cache      : " + cacheDir);

        // --- EAP: copy from user-supplied local directory (filtered to migrations tree) ---
        new EapServerCache(cacheDir, migrationsDir).populateFromLocalPath();

        // --- WildFly: discover from migrations tree, provision via GitHub / main branch ---
        TreeSet<String> wildflyVersions = discoverWildFlyVersions(migrationsDir);
        System.out.println("[WildFlyServerCache] WildFly versions to provision: " + wildflyVersions);

        List<String> failures = new ArrayList<>();
        MainBranchBuilder mainBuilder = new MainBranchBuilder(cacheDir);

        for (String majorMinor : wildflyVersions) {
            try {
                provision(majorMinor, cacheDir, mainBuilder);
            } catch (Exception e) {
                System.err.println("[WildFlyServerCache] FAILED to provision wildfly-" + majorMinor + ": " + e.getMessage());
                failures.add(majorMinor);
            }
        }

        if (!failures.isEmpty()) {
            System.err.println("[WildFlyServerCache] ERROR: Failed to provision WildFly versions: " + failures);
            System.exit(1);
        }
        System.out.println("[WildFlyServerCache] Cache update complete.");
    }

    /**
     * Provisions a single major.minor version into the cache.
     *
     * <p>Strategy:</p>
     * <ol>
     *   <li>Find the latest GitHub Final release tag for the version.</li>
     *   <li>Compare with what is currently cached.</li>
     *   <li>If the cached copy is already that Final release → done.</li>
     *   <li>If a newer Final release exists → download and replace.</li>
     *   <li>If no Final release exists at all → delegate to {@link MainBranchBuilder}.</li>
     *   <li>If there is a cached SNAPSHOT (from main) → also delegate to
     *       {@link MainBranchBuilder} which will check for new commits and rebuild if needed.</li>
     * </ol>
     */
    static void provision(String majorMinor, Path cacheDir, MainBranchBuilder mainBuilder)
            throws IOException, InterruptedException {
        System.out.println("[WildFlyServerCache] Provisioning wildfly-" + majorMinor + " ...");

        String latestFinal = GitHubReleases.latestFinalTag(majorMinor);
        Path cached = ServerCacheLookup.findInCache(cacheDir, majorMinor);

        if (latestFinal != null) {
            String expectedDirName = "wildfly-" + latestFinal;
            if (cached != null && cached.getFileName().toString().equals(expectedDirName)) {
                System.out.println("[WildFlyServerCache] Cache up-to-date: " + cached.getFileName());
                return;
            }
            // Newer Final available — remove old cached copy and download
            if (cached != null) {
                System.out.println("[WildFlyServerCache] Replacing " + cached.getFileName()
                        + " with " + expectedDirName);
                deleteDirectory(cached);
            }
            GitHubReleases.downloadAndExtract(latestFinal, cacheDir);
            System.out.println("[WildFlyServerCache] Cached " + expectedDirName);
        } else {
            // No Final release — use main branch (handles SNAPSHOT caching + update check)
            System.out.println("[WildFlyServerCache] No Final release found for " + majorMinor
                    + "; building from main branch");
            mainBuilder.getOrBuild(majorMinor);
        }
    }

    /**
     * Walks the migrations directory tree to collect all unique major.minor WildFly versions
     * (ignores eap* directories — those are handled by {@link EapServerCache}).
     */
    static TreeSet<String> discoverWildFlyVersions(Path migrationsDir) throws IOException {
        TreeSet<String> versions = new TreeSet<>();
        try (DirectoryStream<Path> targets = Files.newDirectoryStream(migrationsDir, "wildfly*")) {
            for (Path targetDir : targets) {
                if (!Files.isDirectory(targetDir)) continue;
                String targetVersion = extractMajorMinor(targetDir.getFileName().toString());
                if (targetVersion != null) versions.add(targetVersion);
                try (DirectoryStream<Path> sources = Files.newDirectoryStream(targetDir, "wildfly*")) {
                    for (Path sourceDir : sources) {
                        if (!Files.isDirectory(sourceDir)) continue;
                        String sourceVersion = extractMajorMinor(sourceDir.getFileName().toString());
                        if (sourceVersion != null) versions.add(sourceVersion);
                    }
                }
            }
        }
        return versions;
    }

    /**
     * Extracts the {@code "MAJOR.MINOR"} string from a directory name like {@code "wildfly42.0"}.
     * Returns {@code null} if the name doesn't match the expected pattern.
     */
    static String extractMajorMinor(String dirName) {
        if (!dirName.startsWith("wildfly")) return null;
        String version = dirName.substring("wildfly".length()); // e.g. "42.0"
        if (version.matches("\\d+\\.\\d+")) return version;
        return null;
    }

    private static void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        Files.walk(dir)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(p -> {
                    try { Files.delete(p); } catch (IOException ignored) {}
                });
    }
}
