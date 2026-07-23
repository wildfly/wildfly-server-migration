/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.test.cache;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Interacts with GitHub releases for the {@code wildfly/wildfly} project.
 *
 * <p>Tag names follow the pattern {@code <MAJOR>.<MINOR>.<PATCH>.Final}, e.g.
 * {@code 32.0.0.Final}. The download URL for the zip is
 * {@code https://github.com/wildfly/wildfly/releases/download/<tag>/wildfly-<tag>.zip}
 * and the zip extracts to a single top-level directory named {@code wildfly-<tag>}.</p>
 */
public class GitHubReleases {

    private static final String DOWNLOAD_URL =
            "https://github.com/wildfly/wildfly/releases/download/%s/wildfly-%s.zip";

    /**
     * Returns the release tag of the latest {@code .Final} release for the given major.minor
     * version prefix (e.g. {@code "32.0"}), or {@code null} if no such release exists.
     *
     * <p>Probes candidate patch versions starting at {@code <major>.<minor>.0.Final} and
     * increments the patch until a 404 is returned.</p>
     */
    public static String latestFinalTag(String majorMinor) {
        String best = null;
        // WildFly releases only use patch 0 for standard releases, but probe 0-9 to be safe
        for (int patch = 0; patch < 10; patch++) {
            String tag = majorMinor + "." + patch + ".Final";
            if (releaseExists(tag)) {
                best = tag;
            } else if (best != null) {
                // found a gap after a hit — stop
                break;
            }
        }
        return best;
    }

    /**
     * Returns {@code true} if a GitHub release zip exists for the given tag.
     */
    public static boolean releaseExists(String tag) {
        String urlStr = String.format(DOWNLOAD_URL, tag, tag);
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("HEAD");
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(10_000);
            int code = conn.getResponseCode();
            conn.disconnect();
            return code == 200;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Downloads the zip for {@code tag} and extracts it into {@code cacheDir},
     * producing {@code cacheDir/wildfly-<tag>/}.
     *
     * @return the extracted distribution directory
     */
    public static Path downloadAndExtract(String tag, Path cacheDir) throws IOException {
        String urlStr = String.format(DOWNLOAD_URL, tag, tag);
        Path zipPath = cacheDir.resolve("wildfly-" + tag + ".zip");

        System.out.println("[GitHubReleases] Downloading " + urlStr);
        try (InputStream in = new URL(urlStr).openStream()) {
            Files.copy(in, zipPath, StandardCopyOption.REPLACE_EXISTING);
        }

        System.out.println("[GitHubReleases] Extracting " + zipPath);
        unzip(zipPath, cacheDir);
        Files.deleteIfExists(zipPath);

        Path extracted = cacheDir.resolve("wildfly-" + tag);
        if (!Files.isDirectory(extracted)) {
            throw new IOException("Expected directory not found after extraction: " + extracted);
        }
        return extracted;
    }

    private static void unzip(Path zipFile, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path resolved = targetDir.resolve(entry.getName()).normalize();
                if (!resolved.startsWith(targetDir)) {
                    throw new IOException("ZIP path traversal attempt: " + entry.getName());
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
}
