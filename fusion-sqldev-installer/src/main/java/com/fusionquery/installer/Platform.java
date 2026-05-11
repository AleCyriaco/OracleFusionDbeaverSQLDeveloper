package com.fusionquery.installer;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public enum Platform {
    WINDOWS, MACOS, LINUX, UNKNOWN;

    public static Platform detect() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) return WINDOWS;
        if (os.contains("mac") || os.contains("darwin")) return MACOS;
        if (os.contains("nix") || os.contains("nux") || os.contains("aix")) return LINUX;
        return UNKNOWN;
    }

    /** SQL Developer user directory (where ~/.sqldeveloper lives). */
    public Path userDir() {
        String home = System.getProperty("user.home");
        switch (this) {
            case WINDOWS:
                String appData = System.getenv("APPDATA");
                if (appData != null && !appData.isEmpty()) {
                    // Prefer the candidate that actually contains a SQL Developer
                    // <version>/product.conf — covers both historical "SQL Developer"
                    // (capitalized) and modern "sqldeveloper" (lowercase) layouts.
                    String[] candidates = {"SQL Developer", "sqldeveloper"};
                    for (String c : candidates) {
                        Path p = Paths.get(appData, c);
                        if (looksLikeUserDir(p)) return p;
                    }
                    // Fall back to any candidate that simply exists on disk.
                    for (String c : candidates) {
                        Path p = Paths.get(appData, c);
                        if (p.toFile().exists()) return p;
                    }
                    return Paths.get(appData, "sqldeveloper");
                }
                return Paths.get(home, ".sqldeveloper");
            case MACOS:
            case LINUX:
            default:
                return Paths.get(home, ".sqldeveloper");
        }
    }

    /** True if the given directory contains at least one '<version>/product.conf'. */
    private static boolean looksLikeUserDir(Path dir) {
        if (!Files.isDirectory(dir)) return false;
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(dir)) {
            for (Path entry : entries) {
                if (Files.isDirectory(entry)
                        && Files.isRegularFile(entry.resolve("product.conf"))) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * Canonical standalone location where the driver and extension JARs land
     * for use by any JDBC client (DBeaver, DataGrip, etc.).
     */
    public Path standaloneDir() {
        String home = System.getProperty("user.home");
        switch (this) {
            case WINDOWS:
                String userProfile = System.getenv("USERPROFILE");
                if (userProfile == null || userProfile.isEmpty()) userProfile = home;
                return Paths.get(userProfile, "Oracle", "fusion-query-jdbc-1.0.0");
            case MACOS:
            case LINUX:
            default:
                return Paths.get(home, "Oracle", "fusion-query-jdbc-1.0.0");
        }
    }

    /** Common SQL Developer install locations to suggest in the UI. */
    public Path[] commonInstallPaths() {
        switch (this) {
            case MACOS:
                return new Path[] {
                    Paths.get("/Applications/SQLDeveloper.app/Contents/Resources/sqldeveloper")
                };
            case WINDOWS:
                return new Path[] {
                    Paths.get("C:\\Program Files\\sqldeveloper"),
                    Paths.get("C:\\sqldeveloper"),
                    Paths.get(System.getProperty("user.home"), "sqldeveloper")
                };
            case LINUX:
            default:
                return new Path[] {
                    Paths.get("/opt/sqldeveloper"),
                    Paths.get("/usr/local/sqldeveloper"),
                    Paths.get(System.getProperty("user.home"), "sqldeveloper")
                };
        }
    }
}
