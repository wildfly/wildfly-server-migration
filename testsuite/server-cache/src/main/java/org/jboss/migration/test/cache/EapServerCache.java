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
import java.util.TreeSet;

/**
 * Provisions EAP server distributions into the cache from a user-supplied local directory.
 *
 * <p>EAP distributions are not publicly downloadable, so the user pre-populates a local
 * directory (specified via the {@value #LOCAL_PATH_PROPERTY} system property) with
 * either extracted EAP distribution directories or EAP zip archives named
 * {@code jboss-eap-<version>.zip}.</p>
 *
 * <h3>Filtering</h3>
 * <p>Only the EAP major.minor versions that appear in the migrations directory tree
 * (as either a target or a source) are ever provisioned. Any extra distributions in
 * the user-supplied directory are silently ignored.</p>
 *
 * <h3>Version selection — one entry per major.minor</h3>
 * <p>When the user-supplied directory contains several qualifiers for the same
 * major.minor (e.g. {@code jboss-eap-8.2.0.CR1} and {@code jboss-eap-8.2.0.Final}),
 * only the <em>best</em> one is copied into the cache:</p>
 * <ol>
 *   <li>Any entry whose qualifier contains "Final" wins over all others.</li>
 *   <li>Among ties the entry with the most-recent filesystem modification time wins.</li>
 * </ol>
 *
 * <p>If the property is not set, this class is a no-op and EAP tests will be skipped.</p>
 */
public class EapServerCache {

    /** System property pointing to the directory with user-supplied EAP distributions. */
    public static final String LOCAL_PATH_PROPERTY = "testsuite.eapServersDir";

    private final Path cacheDir;
    private final Path migrationsDir;

    public EapServerCache(Path cacheDir, Path migrationsDir) {
        this.cacheDir = cacheDir;
        this.migrationsDir = migrationsDir;
    }

    /**
     * Scans the user-supplied local directory for EAP distributions, filters to only
     * the versions required by the migrations tree, and copies the best qualifier for
     * each required major.minor into the cache.
     */
    public void populateFromLocalPath() throws IOException {
        String localPathProp = System.getProperty(LOCAL_PATH_PROPERTY);
        if (localPathProp == null || localPathProp.isEmpty()) {
            System.out.println("[EapServerCache] " + LOCAL_PATH_PROPERTY
                    + " not set — EAP tests will be skipped if needed EAP servers are missing in the cache.");
            return;
        }

        Path localPath = Path.of(localPathProp).toAbsolutePath().normalize();
        if (!Files.isDirectory(localPath)) {
            System.out.println("[EapServerCache] WARNING: " + LOCAL_PATH_PROPERTY
                    + " points to a non-existent directory: " + localPath
                    + " — EAP tests will be skipped.");
            return;
        }

        TreeSet<String> required = discoverEapVersions(migrationsDir);
        if (required.isEmpty()) {
            System.out.println("[EapServerCache] No EAP versions found in migrations tree — nothing to provision.");
            return;
        }
        System.out.println("[EapServerCache] Required EAP versions: " + required);
        System.out.println("[EapServerCache] Scanning local EAP path: " + localPath);

        for (String majorMinor : required) {
            provisionVersion(majorMinor, localPath);
        }
    }

    /**
     * Picks the best available candidate for {@code majorMinor} from {@code localPath}
     * and copies it into the cache as {@code eap-<version>}.
     */
    private void provisionVersion(String majorMinor, Path localPath) throws IOException {
        // Already cached? (any qualifier for this major.minor)
        Path cached = ServerCacheLookup.findInCache(cacheDir, "eap", majorMinor);
        if (cached != null) {
            System.out.println("[EapServerCache] Already cached: " + cached.getFileName());
            return;
        }

        // Collect all candidates from the local directory that match this major.minor
        List<Path> candidates = collectCandidates(localPath, majorMinor);
        if (candidates.isEmpty()) {
            System.out.println("[EapServerCache] No local distribution found for eap-" + majorMinor
                    + " — tests for this version will be skipped.");
            return;
        }

        Path best = pickBest(candidates);
        String eapVersion = extractEapVersion(best.getFileName().toString());
        if (eapVersion == null) return; // shouldn't happen

        Path dest = cacheDir.resolve("eap-" + eapVersion);
        if (best.getFileName().toString().endsWith(".zip")) {
            extractZipToCache(best, eapVersion, dest);
        } else {
            System.out.println("[EapServerCache] Caching " + best.getFileName() + " as " + dest.getFileName());
            copyDirectory(best, dest);
        }
    }

    /**
     * Returns all entries in {@code localPath} (directories and zip files) whose name
     * starts with {@code jboss-eap-<majorMinor>.}.
     */
    private static List<Path> collectCandidates(Path localPath, String majorMinor)
            throws IOException {
        List<Path> result = new ArrayList<>();
        String prefix = "jboss-eap-" + majorMinor;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(localPath, prefix + "*")) {
            for (Path entry : stream) {
                String name = entry.getFileName().toString();
                if (!name.startsWith(prefix)) continue; // must be <majorMinor><something>
                if (Files.isDirectory(entry) || name.endsWith(".zip")) {
                    result.add(entry);
                }
            }
        }
        return result;
    }

    /**
     * Picks the best candidate:
     * <ol>
     *   <li>Entries whose base name (zip stripped) contains "GA" beat all others.</li>
     *   <li>Ties are broken by filesystem modification time (newest wins).</li>
     * </ol>
     */
    private static Path pickBest(List<Path> candidates) {
        return candidates.stream().max((a, b) -> {
            boolean aFinal = baseName(a).contains("GA");
            boolean bFinal = baseName(b).contains("GA");
            if (aFinal != bFinal) return aFinal ? 1 : -1;
            // both Final or both non-Final — compare by mtime
            try {
                BasicFileAttributes attrA = Files.readAttributes(a, BasicFileAttributes.class);
                BasicFileAttributes attrB = Files.readAttributes(b, BasicFileAttributes.class);
                return attrA.lastModifiedTime().compareTo(attrB.lastModifiedTime());
            } catch (IOException e) {
                return 0;
            }
        }).orElseThrow(() -> new IllegalArgumentException("Empty candidate list"));
    }

    private static String baseName(Path p) {
        String name = p.getFileName().toString();
        return name.endsWith(".zip") ? name.substring(0, name.length() - 4) : name;
    }

    private void extractZipToCache(Path zip, String eapVersion, Path dest) throws IOException {
        Path tmp = cacheDir.resolve(".eap-extract-tmp");
        String dirName = baseName(zip);
        try {
            Files.createDirectories(tmp);
            System.out.println("[EapServerCache] Extracting " + zip.getFileName());
            unzip(zip, tmp);
            Path inner = tmp.resolve(dirName);
            if (!Files.isDirectory(inner)) {
                System.out.println("[EapServerCache] WARNING: Expected directory " + dirName
                        + " not found inside zip — skipping.");
                return;
            }
            System.out.println("[EapServerCache] Caching " + dirName + " as " + dest.getFileName());
            copyDirectory(inner, dest);
        } finally {
            deleteDirectory(tmp);
        }
    }

    // -----------------------------------------------------------------------
    // Version discovery
    // -----------------------------------------------------------------------

    /**
     * Walks the migrations directory tree to collect all unique EAP major.minor versions
     * (ignores wildfly* directories — those are handled by {@link WildFlyServerCache}).
     */
    static TreeSet<String> discoverEapVersions(Path migrationsDir) throws IOException {
        TreeSet<String> versions = new TreeSet<>();
        try (DirectoryStream<Path> targets = Files.newDirectoryStream(migrationsDir, "eap*")) {
            for (Path targetDir : targets) {
                if (!Files.isDirectory(targetDir)) continue;
                String targetVersion = extractMajorMinorFromMigrationDir(targetDir.getFileName().toString());
                if (targetVersion != null) versions.add(targetVersion);
                try (DirectoryStream<Path> sources = Files.newDirectoryStream(targetDir, "eap*")) {
                    for (Path sourceDir : sources) {
                        if (!Files.isDirectory(sourceDir)) continue;
                        String sourceVersion = extractMajorMinorFromMigrationDir(sourceDir.getFileName().toString());
                        if (sourceVersion != null) versions.add(sourceVersion);
                    }
                }
            }
        }
        return versions;
    }

    /**
     * Extracts the {@code "MAJOR.MINOR"} string from a migration directory name like {@code "eap8.2"}.
     * Returns {@code null} if the name doesn't match.
     */
    static String extractMajorMinorFromMigrationDir(String dirName) {
        if (!dirName.startsWith("eap")) return null;
        String version = dirName.substring("eap".length()); // e.g. "8.2"
        if (version.matches("\\d+\\.\\d+")) return version;
        return null;
    }

    /**
     * Extracts the full version string from a distribution name like
     * {@code "jboss-eap-8.2.0.GA"} → {@code "8.2.0.GA"}.
     * Returns {@code null} if the name doesn't match.
     */
    static String extractEapVersion(String dirName) {
        if (!dirName.startsWith("jboss-eap-")) return null;
        String version = dirName.substring("jboss-eap-".length());
        if (version.isEmpty()) return null;
        return version;
    }

    // -----------------------------------------------------------------------
    // File utilities
    // -----------------------------------------------------------------------

    private static void unzip(Path zipFile, Path targetDir) throws IOException {
        try (java.util.zip.ZipInputStream zis =
                new java.util.zip.ZipInputStream(Files.newInputStream(zipFile))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path resolved = targetDir.resolve(entry.getName()).normalize();
                if (!resolved.startsWith(targetDir)) {
                    throw new IOException("ZIP path traversal: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(resolved);
                } else {
                    Files.createDirectories(resolved.getParent());
                    Files.copy(zis, resolved, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        Files.createDirectories(target);
        Files.walk(source).forEach(src -> {
            try {
                Path dst = target.resolve(source.relativize(src));
                if (Files.isDirectory(src)) Files.createDirectories(dst);
                else Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        Files.walk(dir).sorted(java.util.Comparator.reverseOrder())
                .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
    }
}
