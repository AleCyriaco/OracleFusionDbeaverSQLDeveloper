package com.fusionquery.installer;

import java.io.File;
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

        for (Map.Entry<String, Path> e : versionDirs.entrySet()) {
            String version = e.getKey();
            Path productConfDir = e.getValue();
            Path productConf = productConfDir.resolve("product.conf");
            Path systemDir = systemDirs.get(version);
            if (Files.isRegularFile(productConf)) {
                out.add(new Detection(version, productConf, systemDir));
            }
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
