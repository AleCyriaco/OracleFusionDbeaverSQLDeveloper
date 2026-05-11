package com.fusionquery.installer;

import java.io.File;
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
                    for (String candidate : new String[]{"sqldeveloper", "SQL Developer"}) {
                        Path p = Paths.get(appData, candidate);
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
