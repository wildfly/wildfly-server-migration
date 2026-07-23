/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.test.cache;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves a server distribution directory from the cache.
 *
 * <p>Cache directory names follow the pattern {@code <prefix>-<MAJOR>.<MINOR>.<qualifier>},
 * e.g. {@code wildfly-42.0.0.Final}, {@code wildfly-42.0.0.Beta1-SNAPSHOT},
 * {@code eap-8.2.0.Final}.</p>
 *
 * <p>A lookup for prefix {@code "wildfly"} and version {@code "42.0"} matches any directory
 * whose name starts with {@code wildfly-42.0.}.</p>
 *
 * <p>Exactly one match is required at test time; zero or multiple are hard errors.
 * During cache population {@link #findInCache} returns {@code null} on zero hits
 * and throws on multiple.</p>
 */
public class ServerCacheLookup {

    private ServerCacheLookup() {}

    // -----------------------------------------------------------------------
    // Public API — prefix-aware
    // -----------------------------------------------------------------------

    /**
     * Returns the single cached distribution directory, or {@code null} if none found.
     * Throws {@link IllegalStateException} if multiple directories match.
     *
     * <p>Used during cache population.</p>
     */
    public static Path findInCache(Path cacheDir, String prefix, String majorMinor)
            throws IOException {
        List<Path> matches = findMatches(cacheDir, prefix, majorMinor);
        if (matches.isEmpty()) return null;
        if (matches.size() == 1) return matches.get(0);
        // Multiple — sort by modification time descending
        matches.sort((a, b) -> {
            try {
                BasicFileAttributes attrA = Files.readAttributes(a, BasicFileAttributes.class);
                BasicFileAttributes attrB = Files.readAttributes(b, BasicFileAttributes.class);
                return attrB.lastModifiedTime().compareTo(attrA.lastModifiedTime());
            } catch (IOException e) {
                return 0;
            }
        });
        throw new IllegalStateException(
                "Multiple cached distributions match " + prefix + "-" + majorMinor + "* — "
                + "remove stale entries from the cache and retry. Found: " + matches);
    }

    /**
     * Returns the single cached distribution directory, failing hard on zero or multiple matches.
     *
     * <p>Used at test time.</p>
     */
    public static Path requireFromCache(Path cacheDir, String prefix, String majorMinor)
            throws IOException {
        List<Path> matches = findMatches(cacheDir, prefix, majorMinor);
        if (matches.isEmpty()) {
            throw new IllegalStateException(
                    "No cached distribution found for " + prefix + "-" + majorMinor
                    + "* in " + cacheDir
                    + " — run 'mvn install -pl testsuite/server-cache' to populate the cache.");
        }
        if (matches.size() > 1) {
            throw new IllegalStateException(
                    "Multiple cached distributions match " + prefix + "-" + majorMinor
                    + "* in " + cacheDir + " — remove stale entries and retry. Found: " + matches);
        }
        return matches.get(0);
    }

    // -----------------------------------------------------------------------
    // Legacy wildfly-only API (kept for backward compatibility with WildFlyServerCache)
    // -----------------------------------------------------------------------

    /** @see #findInCache(Path, String, String) */
    public static Path findInCache(Path cacheDir, String majorMinor) throws IOException {
        return findInCache(cacheDir, "wildfly", majorMinor);
    }

    /** @see #requireFromCache(Path, String, String) */
    public static Path requireFromCache(Path cacheDir, String majorMinor) throws IOException {
        return requireFromCache(cacheDir, "wildfly", majorMinor);
    }

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------

    private static List<Path> findMatches(Path cacheDir, String prefix, String majorMinor)
            throws IOException {
        List<Path> matches = new ArrayList<>();
        if (!Files.isDirectory(cacheDir)) return matches;
        String glob = prefix + "-" + majorMinor + "*";
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(cacheDir, glob)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) matches.add(entry);
            }
        }
        return matches;
    }
}
