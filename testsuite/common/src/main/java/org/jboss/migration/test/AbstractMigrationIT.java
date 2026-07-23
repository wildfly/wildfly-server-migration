/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.test;

import org.jboss.migration.test.cache.ServerCacheLookup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Base class for all per-target server migration integration tests.
 *
 * <p>Subclasses are JUnit 5 {@code @ParameterizedTest} suites using {@code @MethodSource}.
 * Each subclass declares a {@code static Collection<String> sourceVersions()} factory method
 * and two {@code @ParameterizedTest @MethodSource("sourceVersions")} methods that delegate
 * to {@link #runCleanMigration(String)} and {@link #runPatchedMigration(String)}.
 * All test logic, fixture setup and filesystem utilities live here.</p>
 *
 * <h3>Patch family</h3>
 * <p>The "before" XML patches are selected per source-server family. Subclasses return a
 * {@link PatchFamily} from {@link #patchFamily(String)} based on the source version string.
 * Patch files are loaded from this module's resources as
 * {@code cmtool-standalone-<family>.xml.patch} and {@code cmtool-domain-<family>.xml.patch}.</p>
 *
 * <h3>Skip behaviour</h3>
 * <p>If the <em>target</em> server is absent from the cache the entire test class is skipped
 * via {@link Assumptions} in the {@code @BeforeAll} setup. If only a specific
 * <em>source</em> server is absent, only that parameterised pair is skipped.</p>
 *
 * <h3>Directory layout per test run</h3>
 * <pre>
 *   &lt;module&gt;/target/migrations/&lt;source&gt;-to-&lt;target&gt;-clean/
 *     source/   — copy of source distribution
 *     target/   — copy of target distribution
 *     tool/     — unpacked migration tool
 *   &lt;module&gt;/target/migrations/&lt;source&gt;-to-&lt;target&gt;-patched/
 *     source/   — source with cmtool fixtures applied
 *     target/   — copy of target distribution
 *     tool/     — unpacked migration tool
 * </pre>
 * <p>Directories are deleted before each run and kept afterward for inspection.</p>
 */
public abstract class AbstractMigrationIT {

    /**
     * Identifies which family of before-patches to apply to a source server.
     * Patch resources are named {@code cmtool-standalone-<family>.xml.patch} etc.
     */
    public enum PatchFamily {
        WILDFLY,
        EAP7,
        EAP8
    }

    // -----------------------------------------------------------------------
    // Abstract methods — implemented by each per-target subclass
    // -----------------------------------------------------------------------

    /** The target server version string, e.g. {@code "42.0"} or {@code "8.2"}. */
    protected abstract String targetVersion();

    /**
     * The cache key prefix for the target server, e.g. {@code "wildfly"} or {@code "eap"}.
     * Used to look up {@code <prefix>-<targetVersion>.*} in the cache.
     */
    protected abstract String targetCachePrefix();

    /**
     * The cache key prefix for a given source version, e.g. {@code "wildfly"} or {@code "eap"}.
     */
    protected abstract String sourceCachePrefix(String sourceVersion);

    /**
     * Returns the {@link PatchFamily} to use when applying before-fixtures to a source
     * server of the given version.
     */
    protected abstract PatchFamily patchFamily(String sourceVersion);

    // -----------------------------------------------------------------------
    // Class-level shared state — populated once per concrete subclass JVM
    // -----------------------------------------------------------------------

    private static Path beforeDir;
    private static Path cacheDir;
    private static Path toolZip;
    private static Path migrationsDir;
    /** Root of the repo migrations/ tree, used for source-version discovery. */
    private static Path migrationsRoot;

    /**
     * Resolves all shared resources and skips the entire class if the target server
     * is absent from the cache.
     *
     * <p>Subclasses must not shadow this method.</p>
     */
    @BeforeAll
    static void resolveSharedResources() {
        beforeDir = Paths.get(requiredProperty("testsuite.beforeDir")).toAbsolutePath().normalize();
        cacheDir = Paths.get(requiredProperty("testsuite.serverCacheDir")).toAbsolutePath().normalize();

        migrationsDir = Paths.get(requiredProperty("migrations.buildDir")).toAbsolutePath().normalize();
        try {
            Files.createDirectories(migrationsDir);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create migrations dir: " + migrationsDir, e);
        }

        toolZip = Paths.get(requiredProperty("testsuite.toolZip")).toAbsolutePath().normalize();
        if (!Files.isRegularFile(toolZip)) {
            throw new IllegalStateException("Migration tool zip not found: " + toolZip);
        }

        migrationsRoot = Paths.get(requiredProperty("migrations.projectDir")).toAbsolutePath().normalize();
    }

    // -----------------------------------------------------------------------
    // Core test implementations — called by each subclass @ParameterizedTest
    // -----------------------------------------------------------------------

    /**
     * Runs the clean migration scenario for the given source version.
     * Subclasses expose this via {@code @ParameterizedTest @MethodSource}.
     */
    protected void runCleanMigration(String sourceVersion) throws Exception {
        Path targetDist = requireTarget();
        Path sourceDist = requireSource(sourceVersion);

        Path runDir = prepareRunDirectory(sourceVersion, "clean");
        copyDirectory(sourceDist, runDir.resolve("source"));
        copyDirectory(targetDist, runDir.resolve("target"));
        unzipTool(toolZip, runDir.resolve("tool"));

        int exitCode = runMigration(runDir, false);
        Assertions.assertEquals(0, exitCode, "Clean migration from " + sourceCachePrefix(sourceVersion)
                + sourceVersion + " to " + targetCachePrefix() + targetVersion()
                + " exited with non-zero code");
    }

    /**
     * Runs the patched migration scenario for the given source version.
     * Subclasses expose this via {@code @ParameterizedTest @MethodSource}.
     */
    protected void runPatchedMigration(String sourceVersion) throws Exception {
        Path targetDist = requireTarget();
        Path sourceDist = requireSource(sourceVersion);

        Path runDir = prepareRunDirectory(sourceVersion, "patched");
        copyDirectory(sourceDist, runDir.resolve("source"));
        copyDirectory(targetDist, runDir.resolve("target"));
        unzipTool(toolZip, runDir.resolve("tool"));

        applyBeforeFixtures(runDir.resolve("source"), patchFamily(sourceVersion));

        int exitCode = runMigration(runDir, true);
        Assertions.assertEquals(0, exitCode, "Patched migration from " + sourceCachePrefix(sourceVersion)
                + sourceVersion + " to " + targetCachePrefix() + targetVersion()
                + " exited with non-zero code");
    }

    // -----------------------------------------------------------------------
    // Cache helpers — skip (not fail) when a server is absent
    // -----------------------------------------------------------------------

    private Path requireTarget() throws IOException {
        String label = targetCachePrefix() + "-" + targetVersion();
        Path dist = ServerCacheLookup.findInCache(cacheDir, targetCachePrefix(), targetVersion());
        // Skip the ENTIRE class (all parameterized pairs) when the target is missing.
        Assumptions.assumeTrue(dist != null,
                "Target server not in cache, skipping all tests in this class: " + label);
        return dist;
    }

    private Path requireSource(String version) throws IOException {
        String prefix = sourceCachePrefix(version);
        Path dist = ServerCacheLookup.findInCache(cacheDir, prefix, version);
        Assumptions.assumeTrue(dist != null,
                "Source server not in cache, skipping: " + prefix + "-" + version);
        return dist;
    }

    // -----------------------------------------------------------------------
    // Run directory
    // -----------------------------------------------------------------------

    private Path prepareRunDirectory(String srcVer, String scenario) throws IOException {
        String name = sourceCachePrefix(srcVer) + srcVer + "-to-"
                + targetCachePrefix() + targetVersion() + "-" + scenario;
        Path runDir = migrationsDir.resolve(name);
        if (Files.isDirectory(runDir)) {
            deleteDirectory(runDir);
        }
        Files.createDirectories(runDir);
        return runDir;
    }

    // -----------------------------------------------------------------------
    // Before-fixtures application
    // -----------------------------------------------------------------------

    private static void applyBeforeFixtures(Path sourceDir, PatchFamily family) throws IOException {
        Path beforeDist = beforeDir.resolve("dist");
        if (!Files.isDirectory(beforeDist)) {
            throw new IOException("Cannot locate testsuite/before/dist at: " + beforeDist);
        }

        // ---- modules ----
        copyIfExists(beforeDist.resolve("cmtool"),
                sourceDir.resolve("cmtool"));
        copyIfExists(beforeDist.resolve("modules-system/cmtool"),
                sourceDir.resolve("modules/system/layers/base/cmtool"));
        copyIfExists(beforeDist.resolve("modules-custom/cmtool"),
                sourceDir.resolve("modules/cmtool"));

        // ---- deployment content ----
        copyIfExists(beforeDist.resolve("content"),
                sourceDir.resolve("standalone/data/content"));
        copyIfExists(beforeDist.resolve("content"),
                sourceDir.resolve("domain/data/content"));

        // ---- standalone deployments ----
        copyIfExists(beforeDist.resolve("standalone-deployments"),
                sourceDir.resolve("standalone/deployments"));

        // ---- cmtool-standalone.xml ----
        String familyId = family.name().toLowerCase(); // wildfly / eap7 / eap8
        Path standaloneConfigDir = sourceDir.resolve("standalone/configuration");
        Path standaloneDst = standaloneConfigDir.resolve("cmtool-standalone.xml");
        Files.copy(standaloneConfigDir.resolve("standalone.xml"), standaloneDst,
                StandardCopyOption.REPLACE_EXISTING);
        applyPatch(standaloneDst, loadResourceLines("cmtool-standalone-" + familyId + ".xml.patch"));

        // ---- cmtool-domain.xml ----
        Path domainConfigDir = sourceDir.resolve("domain/configuration");
        Path domainDst = domainConfigDir.resolve("cmtool-domain.xml");
        Files.copy(domainConfigDir.resolve("domain.xml"), domainDst,
                StandardCopyOption.REPLACE_EXISTING);
        applyPatch(domainDst, loadResourceLines("cmtool-domain-" + familyId + ".xml.patch"));
    }

    private static void copyIfExists(Path src, Path dst) throws IOException {
        if (Files.isDirectory(src)) {
            copyDirectory(src, dst);
        }
    }

    // -----------------------------------------------------------------------
    // Migration tool execution
    // -----------------------------------------------------------------------

    private static int runMigration(Path runDir, boolean withPatches)
            throws IOException, InterruptedException {
        Path toolHome = runDir.resolve("tool/jboss-server-migration");
        String os = System.getProperty("os.name", "").toLowerCase();
        String scriptName = os.contains("win") ? "jboss-server-migration.bat" : "jboss-server-migration.sh";
        Path script = toolHome.resolve(scriptName);
        script.toFile().setExecutable(true);

        List<String> cmd = new java.util.ArrayList<>();
        cmd.add(script.toAbsolutePath().toString());
        cmd.add("-n");
        cmd.add("--source");
        cmd.add(runDir.resolve("source").toAbsolutePath().toString());
        cmd.add("--target");
        cmd.add(runDir.resolve("target").toAbsolutePath().toString());
        if (withPatches) {
            cmd.add("-Djboss.server.migration.deployments.migrate-deployments.skip=false");
            cmd.add("-Djboss.server.migration.modules.includes=cmtool.module1");
            cmd.add("-Djboss.server.migration.modules.excludes=cmtool.module2,cmtool.module3");
        }
        return new ProcessBuilder(cmd).inheritIO().start().waitFor();
    }

    // -----------------------------------------------------------------------
    // Patch application
    // -----------------------------------------------------------------------

    private static void applyPatch(Path file, List<String> patchLines) throws IOException {
        String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        for (String line : patchLines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            if (trimmed.startsWith("s|") && trimmed.length() > 4) {
                String inner = trimmed.substring(2);
                int sep = inner.indexOf('|');
                if (sep < 0) continue;
                String pattern = inner.substring(0, sep);
                String rest = inner.substring(sep + 1);
                int trailSep = rest.lastIndexOf('|');
                String replacement = trailSep >= 0 ? rest.substring(0, trailSep) : rest;
                content = content.replace(pattern, replacement);
            }
        }
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    }

    private static List<String> loadResourceLines(String resourceName) throws IOException {
        URL url = AbstractMigrationIT.class.getClassLoader().getResource(resourceName);
        if (url == null) {
            throw new IOException("Patch resource not found on classpath: " + resourceName);
        }
        try (InputStream in = url.openStream()) {
            return Arrays.asList(new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n"));
        }
    }

    // -----------------------------------------------------------------------
    // File-system utilities
    // -----------------------------------------------------------------------

    private static void unzipTool(Path zipFile, Path dest) throws IOException {
        Files.createDirectories(dest);
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path resolved = dest.resolve(entry.getName()).normalize();
                if (!resolved.startsWith(dest)) {
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
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)),
                        StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                Files.delete(d);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static String requiredProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Required system property not set: " + name);
        }
        return value;
    }

    // -----------------------------------------------------------------------
    // Runtime source-version discovery
    // -----------------------------------------------------------------------

    /**
     * Discovers the source versions for a given target server by reading the migrations
     * directory tree at runtime.
     *
     * <p>Call this from a subclass {@code @MethodSource} factory method:</p>
     * <pre>{@code
     * static Collection<String> sourceVersions() {
     *     return AbstractMigrationIT.discoverSourceVersions("wildfly42.0", "wildfly");
     * }
     * }</pre>
     *
     * @param targetDirName  the subdirectory name under {@code migrations/}, e.g. {@code "wildfly42.0"}
     * @param sourcePrefix   glob prefix to filter source entries, e.g. {@code "wildfly"} or {@code "eap"}
     * @return collection of major.minor version strings
     */
    public static Collection<String> discoverSourceVersions(String targetDirName, String sourcePrefix) {
        Path targetDir = migrationsRoot.resolve(targetDirName);
        if (!Files.isDirectory(targetDir)) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        try (DirectoryStream<Path> stream =
                Files.newDirectoryStream(targetDir, sourcePrefix + "*")) {
            for (Path sourceDir : stream) {
                if (!Files.isDirectory(sourceDir)) continue;
                String name = sourceDir.getFileName().toString();
                String version = name.substring(sourcePrefix.length());
                if (version.matches("\\d+\\.\\d+")) {
                    result.add(version);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to discover source versions from: " + targetDir, e);
        }
        result.sort(AbstractMigrationIT::compareVersions);
        return result;
    }

    private static int compareVersions(String a, String b) {
        String[] partsA = a.split("\\.");
        String[] partsB = b.split("\\.");
        for (int i = 0; i < Math.min(partsA.length, partsB.length); i++) {
            int cmp = Integer.compare(Integer.parseInt(partsA[i]), Integer.parseInt(partsB[i]));
            if (cmp != 0) return cmp;
        }
        return Integer.compare(partsA.length, partsB.length);
    }
}
