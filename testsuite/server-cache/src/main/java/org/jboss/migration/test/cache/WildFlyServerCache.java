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
 * <h3>Provisioning strategy</h3>
 * <p><b>Target versions</b> (appear as {@code migrations/wildfly<M>.<N>/} directories):</p>
 * <ol>
 *   <li>Try GitHub for {@code wildfly-<tag>.zip} → download if found.</li>
 *   <li>If NOT found on GitHub → build from {@code wildfly/wildfly:main}.</li>
 * </ol>
 * <p><b>Source versions</b> (appear only as sources under a target dir, not as targets
 * themselves): GitHub download only. If no Final release is found, the version is simply
 * not cached — source servers are never built from a branch.</p>
 *
 * <p>Usage: {@code WildFlyServerCache <migrationsDir> <cacheDir>}</p>
 * <p>The build fails hard (non-zero exit) if any <em>target</em> version cannot be
 * provisioned. Missing source versions produce a warning and the corresponding tests
 * will self-skip.</p>
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

        // --- WildFly: separate target versions (main-branch fallback allowed) from
        //              source-only versions (GitHub only, skip if absent) ---
        VersionSets versionSets = discoverWildFlyVersions(migrationsDir);
        System.out.println("[WildFlyServerCache] WildFly target versions : " + versionSets.targetVersions);
        System.out.println("[WildFlyServerCache] WildFly source-only versions: " + versionSets.sourceOnlyVersions);

        List<String> failures = new ArrayList<>();
        MainBranchBuilder mainBuilder = new MainBranchBuilder(cacheDir);

        // Targets: standard + optional legacy EE; main-branch fallback
        for (String majorMinor : versionSets.targetVersions) {
            try {
                provisionTarget(majorMinor, cacheDir, mainBuilder);
            } catch (Exception e) {
                System.err.println("[WildFlyServerCache] FAILED to provision target wildfly-"
                        + majorMinor + ": " + e.getMessage());
                failures.add(majorMinor);
            }
        }

        // Sources: GitHub only; warn and continue if absent
        for (String majorMinor : versionSets.sourceOnlyVersions) {
            try {
                provisionSourceFromGitHub(majorMinor, cacheDir);
            } catch (Exception e) {
                System.err.println("[WildFlyServerCache] WARNING: failed to provision source wildfly-"
                        + majorMinor + ": " + e.getMessage());
                // Not added to failures — tests will self-skip
            }
        }

        if (!failures.isEmpty()) {
            System.err.println("[WildFlyServerCache] ERROR: Failed to provision WildFly target versions: " + failures);
            System.exit(1);
        }
        System.out.println("[WildFlyServerCache] Cache update complete.");
    }

    /**
     * Provisions a target version: standard dist from GitHub or main-branch build.
     */
    static void provisionTarget(String majorMinor, Path cacheDir, MainBranchBuilder mainBuilder)
            throws IOException, InterruptedException {
        System.out.println("[WildFlyServerCache] Provisioning target wildfly-" + majorMinor + " ...");

        String latestFinal = GitHubReleases.latestFinalTag(majorMinor);
        Path cached = ServerCacheLookup.findInCache(cacheDir, "wildfly", majorMinor);

        if (latestFinal != null) {
            String expectedDirName = "wildfly-" + latestFinal;
            if (cached == null || !cached.getFileName().toString().equals(expectedDirName)) {
                if (cached != null) {
                    System.out.println("[WildFlyServerCache] Replacing " + cached.getFileName()
                            + " with " + expectedDirName);
                    deleteDirectory(cached);
                }
                GitHubReleases.downloadAndExtract(latestFinal, cacheDir);
                System.out.println("[WildFlyServerCache] Cached " + expectedDirName);
            } else {
                System.out.println("[WildFlyServerCache] Dist up-to-date: " + expectedDirName);
            }
        } else {
            // No Final on GitHub → build from main (targets only)
            System.out.println("[WildFlyServerCache] No Final release found for " + majorMinor
                    + "; building from main branch");
            mainBuilder.getOrBuild(majorMinor);
        }
    }

    /**
     * Provisions a source-only version from GitHub. If no Final release is found,
     * logs a warning and returns without caching — the corresponding tests will self-skip.
     */
    static void provisionSourceFromGitHub(String majorMinor, Path cacheDir) throws IOException {
        System.out.println("[WildFlyServerCache] Provisioning source wildfly-" + majorMinor + " ...");

        String latestFinal = GitHubReleases.latestFinalTag(majorMinor);
        if (latestFinal == null) {
            System.out.println("[WildFlyServerCache] No Final release found for source wildfly-"
                    + majorMinor + " — tests for this source will be skipped");
            return;
        }

        String expectedDirName = "wildfly-" + latestFinal;
        Path cached = ServerCacheLookup.findInCache(cacheDir, "wildfly", majorMinor);
        if (cached != null && cached.getFileName().toString().equals(expectedDirName)) {
            System.out.println("[WildFlyServerCache] Source dist up-to-date: " + expectedDirName);
            return;
        }
        if (cached != null) {
            System.out.println("[WildFlyServerCache] Replacing " + cached.getFileName()
                    + " with " + expectedDirName);
            deleteDirectory(cached);
        }
        GitHubReleases.downloadAndExtract(latestFinal, cacheDir);
        System.out.println("[WildFlyServerCache] Cached source " + expectedDirName);
    }

    /** Holds target versions and source-only versions discovered from the migrations tree. */
    static final class VersionSets {
        final TreeSet<String> targetVersions = new TreeSet<>();
        final TreeSet<String> sourceOnlyVersions = new TreeSet<>();
    }

    /**
     * Walks the migrations directory tree, separating target versions (have their own
     * {@code migrations/wildfly<M>.<N>/} directory) from source-only versions (appear
     * only as sources under some target, never as a target themselves).
     */
    static VersionSets discoverWildFlyVersions(Path migrationsDir) throws IOException {
        VersionSets result = new VersionSets();
        try (DirectoryStream<Path> targets = Files.newDirectoryStream(migrationsDir, "wildfly*")) {
            for (Path targetDir : targets) {
                if (!Files.isDirectory(targetDir)) continue;
                String targetVersion = extractMajorMinor(targetDir.getFileName().toString());
                if (targetVersion != null) result.targetVersions.add(targetVersion);
                try (DirectoryStream<Path> sources = Files.newDirectoryStream(targetDir, "wildfly*")) {
                    for (Path sourceDir : sources) {
                        if (!Files.isDirectory(sourceDir)) continue;
                        String sourceVersion = extractMajorMinor(sourceDir.getFileName().toString());
                        if (sourceVersion != null && !result.targetVersions.contains(sourceVersion)) {
                            result.sourceOnlyVersions.add(sourceVersion);
                        }
                    }
                }
            }
        }
        // A version discovered as source before its target dir was seen may be in both sets;
        // targets take precedence — remove from sourceOnly if later found to be a target too.
        result.sourceOnlyVersions.removeAll(result.targetVersions);
        return result;
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
