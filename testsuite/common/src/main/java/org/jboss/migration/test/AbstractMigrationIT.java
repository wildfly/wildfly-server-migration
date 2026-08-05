/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.test;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.migration.test.cache.ServerCacheLookup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.wildfly.core.embedded.Configuration;
import org.wildfly.core.embedded.EmbeddedProcessFactory;
import org.wildfly.core.embedded.EmbeddedProcessStartException;
import org.wildfly.core.embedded.HostController;
import org.wildfly.core.embedded.StandaloneServer;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
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
import java.util.Map;
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
 *
 * <h3>Pass criterion</h3>
 * <p>Two assertions are made per scenario:</p>
 * <ol>
 *   <li>The migration tool exits with code {@code 0}.</li>
 *   <li>Every configuration file in the migrated target server boots without errors,
 *       verified using an embedded server in admin-only mode and a management client
 *       {@code read-boot-errors} operation. This catches subsystem-level failures that
 *       the migration's own embedded boot may have silently tolerated.</li>
 * </ol>
 *
 * <h3>Boot verification</h3>
 * <p>Standalone configs ({@code standalone/configuration/*.xml}) are booted via
 * {@link EmbeddedProcessFactory#createStandaloneServer(Configuration)} with
 * {@code --admin-only}. Domain configs ({@code domain/configuration/domain.xml} +
 * {@code host.xml}) are booted via
 * {@link EmbeddedProcessFactory#createHostController(String, String[], String[], String[])}.
 * After start, {@code read-boot-errors} is issued via the {@link ModelControllerClient};
 * a non-empty result fails the test.</p>
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

    /**
     * The server name prefix shared by both target and source servers, used as:
     * <ul>
     *   <li>The cache lookup prefix, e.g. {@code "wildfly"} → {@code wildfly-42.0.0.Final/}</li>
     *   <li>The migration directory prefix, e.g. {@code "wildfly"} → {@code migrations/wildfly42.0/}</li>
     *   <li>The run-directory label, e.g. {@code wildfly41.0-to-wildfly42.0-clean/}</li>
     * </ul>
     * Examples: {@code "wildfly"}, {@code "eap"}.
     */
    protected abstract String serverNamePrefix();

    /**
     * The target server version string, e.g. {@code "42.0"} or {@code "8.1"}.
     */
    protected abstract String targetVersion();

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
        System.out.println("\n[RunMigration] " + serverNamePrefix() + sourceVersion + " → "
                + serverNamePrefix() + targetVersion() + " [clean]");

        Path targetDist = requireTarget();
        Path sourceDist = requireSource(sourceVersion);

        Path runDir = prepareRunDirectory(sourceVersion, "clean");
        copyDirectory(sourceDist, runDir.resolve("source"));
        copyDirectory(targetDist, runDir.resolve("target"));
        unzipTool(toolZip, runDir.resolve("tool"));

        int exitCode = runMigration(runDir, false);
        Assertions.assertEquals(0, exitCode, "Clean migration from " + serverNamePrefix()
                + sourceVersion + " to " + serverNamePrefix() + targetVersion()
                + " exited with non-zero code");

        verifyMigratedServerBoots(runDir, sourceVersion, "clean");
    }

    /**
     * Runs the patched migration scenario for the given source version.
     * Subclasses expose this via {@code @ParameterizedTest @MethodSource}.
     */
    protected void runPatchedMigration(String sourceVersion) throws Exception {
        System.out.println("\n[RunMigration] " + serverNamePrefix() + sourceVersion + " → "
                + serverNamePrefix() + targetVersion() + " [patched]");

        Path targetDist = requireTarget();
        Path sourceDist = requireSource(sourceVersion);

        Path runDir = prepareRunDirectory(sourceVersion, "patched");
        copyDirectory(sourceDist, runDir.resolve("source"));
        copyDirectory(targetDist, runDir.resolve("target"));
        unzipTool(toolZip, runDir.resolve("tool"));

        applyBeforeFixtures(runDir.resolve("source"), patchFamily(sourceVersion));

        int exitCode = runMigration(runDir, true);
        Assertions.assertEquals(0, exitCode, "Patched migration from " + serverNamePrefix()
                + sourceVersion + " to " + serverNamePrefix() + targetVersion()
                + " exited with non-zero code");

        verifyMigratedServerBoots(runDir, sourceVersion, "patched");
    }

    // -----------------------------------------------------------------------
    // Cache helpers — skip (not fail) when a server is absent
    // -----------------------------------------------------------------------

    private Path requireTarget() throws IOException {
        String label = serverNamePrefix() + "-" + targetVersion();
        Path dist = ServerCacheLookup.findInCache(cacheDir, serverNamePrefix(), targetVersion());
        Assumptions.assumeTrue(dist != null,
                "Target server not in cache, skipping all tests in this class: " + label);
        return dist;
    }

    private Path requireSource(String version) throws IOException {
        Path dist = ServerCacheLookup.findInCache(cacheDir, serverNamePrefix(), version);
        Assumptions.assumeTrue(dist != null,
                "Source server not in cache, skipping: " + serverNamePrefix() + "-" + version);
        return dist;
    }

    // -----------------------------------------------------------------------
    // Run directory
    // -----------------------------------------------------------------------

    private Path prepareRunDirectory(String srcVer, String scenario) throws IOException {
        String name = serverNamePrefix() + srcVer + "-to-"
                + serverNamePrefix() + targetVersion() + "-" + scenario;
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
        ProcessBuilder pb = new ProcessBuilder(cmd);
        Map<String, String> env = pb.environment();
        // Prevent the Windows .bat script from calling "pause" (waiting for keyboard input)
        // when launched non-interactively from ProcessBuilder.
        env.put("NOPAUSE", "true");
        if (withPatches) {
            // use JAVA_OPTS to avoid issues on Windows OS, which splits cmds on = and ,
            String javaOpts = env.getOrDefault("JAVA_OPTS", "");
            javaOpts += " -Djboss.server.migration.deployments.migrate-deployments.skip=false";
            javaOpts += " -Djboss.server.migration.modules.includes=cmtool.module1";
            javaOpts += " -Djboss.server.migration.modules.excludes=cmtool.module2,cmtool.module3";
            env.put("JAVA_OPTS", javaOpts);
        }

        // Pipe stdout+stderr through System.out so Failsafe captures migration tool output.
        // The drain thread MUST be joined after waitFor() — on Windows the process blocks
        // writing to the pipe if the buffer fills and nothing is draining it.
        Process process = pb.directory(toolHome.toFile())
                .redirectErrorStream(true)
                .start();
        Thread drainThread = startDrainThread(process.getInputStream(), System.out);
        int exitCode = process.waitFor();
        drainThread.join();
        return exitCode;
    }

    /**
     * Starts a thread that reads {@code in} line by line and writes to {@code out}.
     * Returns the started thread so the caller can {@link Thread#join()} it after
     * {@link Process#waitFor()} to ensure all output is flushed.
     */
    private static Thread startDrainThread(InputStream in, PrintStream out) {
        Thread t = new Thread(() -> {
            try (java.io.BufferedReader reader =
                    new java.io.BufferedReader(new java.io.InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    out.println(line);
                }
            } catch (IOException ignored) {
                // process closed its stream — normal on exit
            }
        });
        t.setDaemon(true);
        t.start();
        return t;
    }

    // -----------------------------------------------------------------------
    // Boot verification
    // -----------------------------------------------------------------------

    /**
     * Parses the migration report to discover which configs were migrated, then boots
     * each one in embedded admin-only mode and asserts no boot errors.
     *
     * <p>Only configs recorded in the report are tested — exactly those the tool actually
     * migrated. The report is at
     * {@code runDir/tool/jboss-server-migration/reports/migration-report.xml}.</p>
     */
    private void verifyMigratedServerBoots(Path runDir, String sourceVersion, String scenario)
            throws IOException {
        String label = serverNamePrefix() + sourceVersion + " → "
                + serverNamePrefix() + targetVersion() + " [" + scenario + "]";

        Path report = runDir.resolve(
                "tool/jboss-server-migration/reports/migration-report.xml");
        if (!Files.isRegularFile(report)) {
            System.out.println("[BootVerification] No migration report found at " + report
                    + " — skipping boot verification for " + label);
            return;
        }

        MigrationReportTargets targets = parseMigrationReport(report);

        // --- standalone configs: one embedded boot per migrated config ---
        for (Path configPath : targets.standaloneConfigs) {
            bootStandaloneConfig(configPath, label);
        }

        // --- domain configs: pair each domain.xml with host.xml ---
        for (Path domainXml : targets.domainConfigs) {
            Path serverHome = domainXml.getParent().getParent().getParent(); // .../target/
            // Prefer host.xml; fall back to first available host config
            Path hostXml = targets.hostConfigs.stream()
                    .filter(h -> h.getFileName().toString().equals("host.xml"))
                    .findFirst()
                    .orElse(targets.hostConfigs.isEmpty() ? null : targets.hostConfigs.get(0));
            if (hostXml == null) {
                System.out.println("[BootVerification] No host config in report — skipping domain boot for " + label);
                continue;
            }
            bootDomainConfig(serverHome, domainXml.getFileName().toString(),
                    hostXml.getFileName().toString(), label);
        }
    }

    /**
     * Holds the sets of migrated config file paths extracted from the migration report.
     */
    private static final class MigrationReportTargets {
        final List<Path> standaloneConfigs = new ArrayList<>();
        final List<Path> domainConfigs = new ArrayList<>();
        final List<Path> hostConfigs = new ArrayList<>();
    }

    /**
     * Parses {@code migration-report.xml} and collects {@code targetPath} attributes from
     * tasks whose name matches {@code standalone-configuration(*)},
     * {@code domain-configuration(*)}, or {@code host-configuration(*)}.
     */
    private static MigrationReportTargets parseMigrationReport(Path reportFile) throws IOException {
        MigrationReportTargets result = new MigrationReportTargets();
        try {
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().parse(reportFile.toFile());
            NodeList tasks = doc.getElementsByTagName("task");
            for (int i = 0; i < tasks.getLength(); i++) {
                Element task = (Element) tasks.item(i);
                String name = task.getAttribute("name");
                if (!name.startsWith("standalone-configuration(")
                        && !name.startsWith("domain-configuration(")
                        && !name.startsWith("host-configuration(")) {
                    continue;
                }
                // Only SUCCESS tasks have a targetPath
                NodeList attributes = task.getElementsByTagName("attribute");
                for (int j = 0; j < attributes.getLength(); j++) {
                    Element attr = (Element) attributes.item(j);
                    if ("targetPath".equals(attr.getAttribute("name"))) {
                        Path targetPath = Paths.get(attr.getAttribute("value"));
                        if (name.startsWith("standalone-configuration(")) {
                            result.standaloneConfigs.add(targetPath);
                        } else if (name.startsWith("domain-configuration(")) {
                            result.domainConfigs.add(targetPath);
                        } else {
                            result.hostConfigs.add(targetPath);
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to parse migration report: " + reportFile, e);
        }
        result.standaloneConfigs.sort(Path::compareTo);
        result.hostConfigs.sort(Path::compareTo);
        return result;
    }

    /**
     * Boots a standalone config (given as absolute path) in embedded admin-only mode
     * and asserts no boot errors.
     */
    private static void bootStandaloneConfig(Path configPath, String label) throws IOException {
        // The server home is configPath's grandparent: .../standalone/configuration/foo.xml
        Path serverHome = configPath.getParent().getParent().getParent();
        String configFileName = configPath.getFileName().toString();
        System.out.println("\n[BootVerification] standalone -c " + configFileName
                + "  (" + label + ")\n");

        Configuration config = Configuration.Builder.of(serverHome)
                .addCommandArgument("--server-config=" + configFileName)
                .addCommandArgument("--admin-only")
                .addCommandArgument("-Dorg.wildfly.logging.embedded=false")
                .addSystemPackage("org.jboss.logmanager")
                .build();

        StandaloneServer server = EmbeddedProcessFactory.createStandaloneServer(config);
        try {
            server.start();
        } catch (EmbeddedProcessStartException e) {
            Assertions.fail("Embedded standalone failed to start with config " + configFileName
                    + "  (" + label + "): " + e.getMessage());
            return;
        }
        try (ModelControllerClient client = server.getModelControllerClient()) {
            assertNoBootErrors(client, configFileName, label, false, null);
        } finally {
            server.stop();
        }
    }

    /**
     * Boots a domain+host config pair in embedded admin-only mode and asserts no boot errors.
     *
     * @param serverHome     the target server home directory
     * @param domainConfig   filename of the domain config (e.g. {@code "domain.xml"})
     * @param hostConfig     filename of the host config (e.g. {@code "host.xml"})
     */
    private static void bootDomainConfig(Path serverHome, String domainConfig,
            String hostConfig, String label) throws IOException {
        System.out.println("\n[BootVerification] domain --domain-config=" + domainConfig
                + " --host-config=" + hostConfig + "  (" + label + ")\n");

        String[] cmds = {
            "--domain-config=" + domainConfig,
            "--host-config=" + hostConfig,
            "--admin-only",
            "-Dorg.wildfly.logging.embedded=false"
        };
        String[] systemPackages = {"org.jboss.logmanager"};
        HostController hc = EmbeddedProcessFactory.createHostController(
                serverHome.toAbsolutePath().toString(), null, systemPackages, cmds);
        try {
            hc.start();
        } catch (EmbeddedProcessStartException e) {
            Assertions.fail("Embedded host controller failed to start (" + domainConfig + "+"
                    + hostConfig + ")  (" + label + "): " + e.getMessage());
            return;
        }
        try (ModelControllerClient client = hc.getModelControllerClient()) {
            ModelNode hostsOp = new ModelNode();
            hostsOp.get("operation").set("read-children-names");
            hostsOp.get("address").setEmptyList();
            hostsOp.get("child-type").set("host");
            ModelNode hostsResult = client.execute(hostsOp);
            String hostName = hostsResult.get("result").asList().get(0).asString();
            assertNoBootErrors(client, domainConfig + "+" + hostConfig, label, true, hostName);
        } finally {
            hc.stop();
        }
    }

    /**
     * Executes {@code read-boot-errors} and fails the test if the list is non-empty.
     *
     * @param domain    {@code true} to target {@code /host=<hostName>/core-service=management},
     *                  {@code false} for standalone {@code /core-service=management}
     * @param hostName  required when {@code domain=true}
     */
    private static void assertNoBootErrors(ModelControllerClient client,
            String configDescription, String label, boolean domain, String hostName)
            throws IOException {
        ModelNode op = new ModelNode();
        op.get("operation").set("read-boot-errors");
        ModelNode addr = op.get("address");
        if (domain) {
            addr.add("host", hostName);
        }
        addr.add("core-service", "management");

        ModelNode result = client.execute(op);
        String outcome = result.get("outcome").asString();
        if (!"success".equals(outcome)) {
            // read-boot-errors not supported on this version — skip the check
            System.out.println("[BootVerification] WARNING: read-boot-errors not supported on "
                    + configDescription + "  (" + label + ") — skipping boot-error check");
            return;
        }
        List<ModelNode> errors = result.get("result").asList();
        if (!errors.isEmpty()) {
            Assertions.fail("Boot errors in " + configDescription + "  (" + label + "): " + errors);
        }
        System.out.println("[BootVerification] OK: " + configDescription + "  (" + label + ")");
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
     * <p>Lists all subdirectories of {@code migrations/<targetDirName>/} whose names match
     * the pattern {@code <anyPrefix><MAJOR>.<MINOR>}, extracts the version portion, and
     * returns them sorted numerically. No prefix filter is applied — any server product
     * family is included.</p>
     *
     * <p>Call this from a subclass {@code @MethodSource} factory method:</p>
     * <pre>{@code
     * static Collection<String> sourceVersions() {
     *     return AbstractMigrationIT.discoverSourceVersions("wildfly42.0");
     * }
     * }</pre>
     *
     * @param targetDirName  the subdirectory name under {@code migrations/}, e.g. {@code "wildfly42.0"}
     * @return collection of major.minor version strings (without the server name prefix)
     */
    public static Collection<String> discoverSourceVersions(String targetDirName) {
        Path targetDir = migrationsRoot.resolve(targetDirName);
        if (!Files.isDirectory(targetDir)) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetDir)) {
            for (Path sourceDir : stream) {
                if (!Files.isDirectory(sourceDir)) continue;
                String name = sourceDir.getFileName().toString();
                // Strip any leading non-digit prefix (e.g. "wildfly", "eap") to get "MAJOR.MINOR"
                String version = name.replaceFirst("^[a-zA-Z]+", "");
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
