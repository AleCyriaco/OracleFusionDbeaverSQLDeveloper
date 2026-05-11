package com.fusionquery.installer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Consumer;

public class InstallTask {

    public static final String EXTENSION_JAR = "fusion-sqldev-extension-1.0.0.jar";

    private static final String EXTENSIONS_DIR_NAME = "user_extensions";
    private static final String CONF_MARKER_START = "# >>> Fusion Query JDBC extension (managed)";
    private static final String CONF_MARKER_END   = "# <<< Fusion Query JDBC extension";

    private final Platform platform;
    private final Path userDir;
    private final List<SqlDevDetector.Detection> detections;
    private final Consumer<String> log;

    public InstallTask(Platform platform, Path userDir,
                       List<SqlDevDetector.Detection> detections,
                       Consumer<String> log) {
        this.platform = platform;
        this.userDir = userDir;
        this.detections = detections;
        this.log = log;
    }

    public void install() throws IOException {
        log.accept("Platform: " + platform);
        log.accept("User dir: " + userDir);
        Files.createDirectories(userDir);

        Path extDir = userDir.resolve(EXTENSIONS_DIR_NAME);
        Files.createDirectories(extDir);
        Path target = extDir.resolve(EXTENSION_JAR);
        copyBundledExtension(target);
        log.accept("Copied extension JAR -> " + target);

        if (detections.isEmpty()) {
            log.accept("No SQL Developer version directories found yet. The JAR is installed; "
                + "when you launch SQL Developer the first time it will create its config, "
                + "then re-run the installer to apply settings.");
            return;
        }

        for (SqlDevDetector.Detection d : detections) {
            log.accept("--- " + d + " ---");
            updateProductConf(d.productConf, extDir);
            clearCache(d.systemCache());
        }

        log.accept("");
        log.accept("Installation complete. Restart SQL Developer (Cmd+Q / File > Exit) and reopen it.");
        log.accept("The new connection type 'Oracle Fusion Cloud (BIP)' will appear in 'New Database Connection' > Database Type.");
    }

    public void uninstall() throws IOException {
        Path extDir = userDir.resolve(EXTENSIONS_DIR_NAME);
        Path target = extDir.resolve(EXTENSION_JAR);
        Files.deleteIfExists(target);
        log.accept("Removed extension JAR.");

        for (SqlDevDetector.Detection d : detections) {
            removeManagedBlock(d.productConf);
            clearCache(d.systemCache());
            log.accept("Cleaned " + d);
        }
        log.accept("Uninstall complete.");
    }

    private void copyBundledExtension(Path target) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(EXTENSION_JAR)) {
            if (in == null) throw new IOException("Bundled extension JAR not found: " + EXTENSION_JAR);
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void updateProductConf(Path productConf, Path extDir) throws IOException {
        List<String> lines = Files.readAllLines(productConf, StandardCharsets.UTF_8);
        List<String> filtered = removeManagedBlockLines(lines);

        String extDirStr = extDir.toAbsolutePath().toString();
        // SQL Developer config files are loaded by the launcher, which doesn't expand $HOME; use absolute paths
        filtered.add("");
        filtered.add(CONF_MARKER_START);
        filtered.add("AddVMOption -Dide.bundle.search.path=" + extDirStr);
        filtered.add("AddVMOption -Dide.extension.search.path=sqldeveloper/extensions:jdev/extensions:ide/extensions:" + extDirStr);
        filtered.add(CONF_MARKER_END);

        Files.write(productConf, filtered, StandardCharsets.UTF_8);
        log.accept("Updated " + productConf);
    }

    private void removeManagedBlock(Path productConf) throws IOException {
        if (!Files.isRegularFile(productConf)) return;
        List<String> lines = Files.readAllLines(productConf, StandardCharsets.UTF_8);
        Files.write(productConf, removeManagedBlockLines(lines), StandardCharsets.UTF_8);
    }

    private List<String> removeManagedBlockLines(List<String> lines) {
        List<String> out = new ArrayList<>(lines.size());
        boolean inBlock = false;
        for (String line : lines) {
            if (line.trim().equals(CONF_MARKER_START)) { inBlock = true; continue; }
            if (line.trim().equals(CONF_MARKER_END))   { inBlock = false; continue; }
            if (!inBlock) out.add(line);
        }
        while (!out.isEmpty() && out.get(out.size() - 1).trim().isEmpty()) {
            out.remove(out.size() - 1);
        }
        return out;
    }

    private void clearCache(Path cache) throws IOException {
        if (cache == null || !Files.isDirectory(cache)) return;
        deleteRecursively(cache);
        log.accept("Cleared cache " + cache);
    }

    private static void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult visitFile(Path f, BasicFileAttributes a) throws IOException {
                Files.delete(f); return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                Files.delete(d); return FileVisitResult.CONTINUE;
            }
        });
    }
}
