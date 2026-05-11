package com.fusionquery.installer;

import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects SQL Developer versions present in the user directory and the
 * matching version-specific subdirs (product.conf folder + system_cache folder).
 *
 * SQL Developer layout in user-dir:
 *   ~/.sqldeveloper/24.3.1/product.conf
 *   ~/.sqldeveloper/system24.3.1.347.1826/system_cache/
 */
public class SqlDevDetector {

    private static final Pattern VERSION_DIR = Pattern.compile("^(\\d+\\.\\d+\\.\\d+)$");
    private static final Pattern SYSTEM_DIR = Pattern.compile("^system(\\d+\\.\\d+\\.\\d+)(\\.\\d+\\.\\d+)?$");

    public static List<Detection> findVersions(Path userDir) {
        List<Detection> out = new ArrayList<>();
        if (!Files.isDirectory(userDir)) return out;

        Map<String, Path> versionDirs = new TreeMap<>();
        Map<String, Path> systemDirs = new TreeMap<>();

        try (DirectoryStream<Path> entries = Files.newDirectoryStream(userDir)) {
            for (Path entry : entries) {
                if (!Files.isDirectory(entry)) continue;
                String name = entry.getFileName().toString();

                Matcher m1 = VERSION_DIR.matcher(name);
                if (m1.matches()) {
                    versionDirs.put(m1.group(1), entry);
                    continue;
                }
                Matcher m2 = SYSTEM_DIR.matcher(name);
                if (m2.matches()) {
                    systemDirs.put(m2.group(1), entry);
                }
            }
        } catch (Exception ignored) {}

        // Union of versions found via either signal. Portable SQL Developer
        // installs sometimes have only the system<version> dir (the launcher
        // reads sqldeveloper.conf from the install root, never creating
        // <userdir>/<version>/product.conf) — in that case product.conf is
        // synthesized by the installer.
        Set<String> versions = new TreeSet<>();
        versions.addAll(versionDirs.keySet());
        versions.addAll(systemDirs.keySet());

        for (String version : versions) {
            Path versionDir = versionDirs.get(version);
            if (versionDir == null) versionDir = userDir.resolve(version);
            Path productConf = versionDir.resolve("product.conf");
            Path systemDir = systemDirs.get(version);
            out.add(new Detection(version, productConf, systemDir));
        }
        return out;
    }

    public static class Detection {
        public final String version;
        public final Path productConf;
        public final Path systemDir; // may be null (no cache yet)

        public Detection(String version, Path productConf, Path systemDir) {
            this.version = version;
            this.productConf = productConf;
            this.systemDir = systemDir;
        }

        public Path systemCache() {
            return systemDir == null ? null : systemDir.resolve("system_cache");
        }

        @Override
        public String toString() { return "SQL Developer " + version; }
    }
}
