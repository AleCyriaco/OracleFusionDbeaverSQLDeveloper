package com.fusionquery.installer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Consumer;

public class InstallTask {

    public static final String EXTENSION_JAR = "fusion-sqldev-extension-1.0.0.jar";
    public static final String DRIVER_JAR = "fusion-query-jdbc-1.0.0.jar";

    private static final String EXTENSIONS_DIR_NAME = "user_extensions";
    private static final String CONF_MARKER_START = "# >>> Fusion Query JDBC extension (managed)";
    private static final String CONF_MARKER_END   = "# <<< Fusion Query JDBC extension";

    private final Platform platform;
    private final Path userDir;
    private final List<SqlDevDetector.Detection> detections;
    private final Path installDir;
    private final Consumer<String> log;

    public InstallTask(Platform platform, Path userDir,
                       List<SqlDevDetector.Detection> detections,
                       Path installDir,
                       Consumer<String> log) {
        this.platform = platform;
        this.userDir = userDir;
        this.detections = detections;
        this.installDir = installDir;
        this.log = log;
    }

    // Convenience for callers that don't have an install dir.
    public InstallTask(Platform platform, Path userDir,
                       List<SqlDevDetector.Detection> detections,
                       Consumer<String> log) {
        this(platform, userDir, detections, null, log);
    }

    public void install() throws IOException {
        log.accept("Platform: " + platform);
        log.accept("User dir: " + userDir);
        Files.createDirectories(userDir);

        Path standaloneDir = platform.standaloneDir();
        Path extDir = userDir.resolve(EXTENSIONS_DIR_NAME);
        Path resolvedInstallDir = installDir != null ? installDir : platform.findInstallDir();
        boolean haveInstallDir = resolvedInstallDir != null && Platform.looksLikeInstallDir(resolvedInstallDir);
        Path sqldevExtensionsDir = haveInstallDir ? Platform.extensionsDir(resolvedInstallDir) : null;
        assertNotLocked(standaloneDir.resolve(DRIVER_JAR),
                        standaloneDir.resolve(EXTENSION_JAR),
                        extDir.resolve(EXTENSION_JAR),
                        sqldevExtensionsDir != null ? sqldevExtensionsDir.resolve(EXTENSION_JAR) : null);

        // 1) Canonical standalone location used by DBeaver / DataGrip / IntelliJ
        Files.createDirectories(standaloneDir);
        Path standaloneDriver = standaloneDir.resolve(DRIVER_JAR);
        Path standaloneExt = standaloneDir.resolve(EXTENSION_JAR);
        copyBundledResource(DRIVER_JAR, standaloneDriver);
        copyBundledResource(EXTENSION_JAR, standaloneExt);
        log.accept("Created standalone folder: " + standaloneDir);

        // 2) user_extensions copy of the extension (where SQL Developer scans)
        Files.createDirectories(extDir);
        Path extTarget = extDir.resolve(EXTENSION_JAR);
        copyBundledResource(EXTENSION_JAR, extTarget);
        log.accept("Copied extension JAR -> " + extTarget);

        // The OSGi/netigso bundle cache lives under <user.home>/.sqldeveloper
        // even when the configured user dir is %APPDATA%\sqldeveloper (26.x
        // derives it from user.home, not ide.user.dir). A stale cache keeps
        // serving the PREVIOUS copy of the extension after an upgrade, so
        // clear any caches found there as well — and before the early return
        // below, which fires when the user dir has no version folders yet.
        Path dotSqldev = Paths.get(System.getProperty("user.home"), ".sqldeveloper");
        if (!dotSqldev.equals(userDir)) {
            for (SqlDevDetector.Detection d : SqlDevDetector.findVersions(dotSqldev)) {
                clearCache(d.systemCache());
            }
        }

        if (detections.isEmpty()) {
            log.accept("No SQL Developer version directories found yet. The JARs are installed; "
                + "launch SQL Developer once so it creates its config, then re-run the installer "
                + "to enable the connection type and register the driver.");
            return;
        }

        // 3) Install into the install dir: the extension JAR goes into
        // sqldeveloper/extensions (the directory SQL Developer always scans,
        // where Oracle's own extensions live) and the launcher conf gets an
        // AddJavaLibFile for the driver. No ide.*.search.path overrides —
        // replacing ide.bundle.search.path made the OSGi boot of newer
        // SQL Developer versions exit silently before creating the system dir.
        if (haveInstallDir) {
            if (Files.isDirectory(sqldevExtensionsDir)) {
                copyBundledResource(EXTENSION_JAR, sqldevExtensionsDir.resolve(EXTENSION_JAR));
                log.accept("Copied extension JAR -> " + sqldevExtensionsDir.resolve(EXTENSION_JAR));
            } else {
                log.accept("(extensions dir missing, skipping copy: " + sqldevExtensionsDir + ")");
            }
            registerInBundlesInfo(resolvedInstallDir);
            Path launcherConf = Platform.launcherConf(resolvedInstallDir);
            if (Files.isRegularFile(launcherConf)) {
                updateLauncherConf(launcherConf, standaloneDir);
                log.accept("Patched launcher conf: " + launcherConf);
            } else {
                log.accept("(install dir found but launcher conf missing: " + launcherConf + ")");
            }
        } else {
            log.accept("(SQL Developer install dir not auto-detected — falling back to user-dir product.conf only)");
        }

        for (SqlDevDetector.Detection d : detections) {
            log.accept("--- " + d + " ---");
            // Point search.path at the canonical Oracle\fusion-query-jdbc-1.0.0
            // folder rather than user_extensions: on Windows the user dir is
            // 'SQL Developer' (with a space) which breaks AddVMOption parsing
            // — the launcher splits the value at the space. The canonical
            // path under %USERPROFILE%\Oracle has no spaces.
            updateProductConf(d.productConf, standaloneDir);
            // TPDRIVER points at the canonical standalone copy of the driver
            registerThirdPartyDriver(d, standaloneDriver);
            clearCache(d.systemCache());
        }

        log.accept("");
        log.accept("Installation complete. Restart SQL Developer (Cmd+Q / File > Exit) and reopen it.");
        log.accept("The new connection type 'Oracle Fusion Cloud (BIP)' will appear in 'New Database Connection' > Database Type.");
        log.accept("For DBeaver / DataGrip / IntelliJ point at: " + standaloneDriver);
    }

    public void uninstall() throws IOException {
        Path extDir = userDir.resolve(EXTENSIONS_DIR_NAME);
        Path standaloneDir = platform.standaloneDir();
        Path resolvedInstallDir = installDir != null ? installDir : platform.findInstallDir();
        Path sqldevExtensionsDir = resolvedInstallDir != null
                ? Platform.extensionsDir(resolvedInstallDir) : null;
        assertNotLocked(extDir.resolve(EXTENSION_JAR),
                        extDir.resolve(DRIVER_JAR),
                        standaloneDir.resolve(EXTENSION_JAR),
                        standaloneDir.resolve(DRIVER_JAR),
                        sqldevExtensionsDir != null ? sqldevExtensionsDir.resolve(EXTENSION_JAR) : null);

        Files.deleteIfExists(extDir.resolve(EXTENSION_JAR));
        Files.deleteIfExists(extDir.resolve(DRIVER_JAR));
        Files.deleteIfExists(standaloneDir.resolve(EXTENSION_JAR));
        Files.deleteIfExists(standaloneDir.resolve(DRIVER_JAR));
        try {
            if (Files.isDirectory(standaloneDir)
                    && !Files.list(standaloneDir).findAny().isPresent()) {
                Files.delete(standaloneDir);
            }
        } catch (IOException ignored) {}
        log.accept("Removed extension and driver JARs.");

        if (resolvedInstallDir != null) {
            if (sqldevExtensionsDir != null) {
                Files.deleteIfExists(sqldevExtensionsDir.resolve(EXTENSION_JAR));
            }
            Path bundlesInfo = Platform.bundlesInfo(resolvedInstallDir);
            if (Files.isRegularFile(bundlesInfo)) {
                removeManagedBlock(bundlesInfo);
                log.accept("Cleaned bundle registration in " + bundlesInfo);
            }
            Path launcherConf = Platform.launcherConf(resolvedInstallDir);
            if (Files.isRegularFile(launcherConf)) {
                removeManagedBlock(launcherConf);
                log.accept("Cleaned launcher conf: " + launcherConf);
            }
        }

        Path dotSqldev = Paths.get(System.getProperty("user.home"), ".sqldeveloper");
        if (!dotSqldev.equals(userDir)) {
            for (SqlDevDetector.Detection d : SqlDevDetector.findVersions(dotSqldev)) {
                clearCache(d.systemCache());
            }
        }

        for (SqlDevDetector.Detection d : detections) {
            removeManagedBlock(d.productConf);
            unregisterThirdPartyDriver(d);
            clearCache(d.systemCache());
            log.accept("Cleaned " + d);
        }
        log.accept("Uninstall complete.");
    }

    /**
     * Windows keeps a JAR that a running SQL Developer has loaded open without
     * sharing write access, so replacing it fails — historically halfway
     * through the install, leaving the JARs copied but nothing registered.
     * Probe every JAR we are about to replace before touching anything and
     * stop with an actionable message instead.
     */
    private void assertNotLocked(Path... targets) throws IOException {
        for (Path target : targets) {
            if (target == null || !Files.isRegularFile(target)) continue;
            // WRITE alone opens without truncating: the open itself is the test.
            try (OutputStream probe = Files.newOutputStream(target, StandardOpenOption.WRITE)) {
                // not locked
            } catch (IOException e) {
                throw new IOException(
                    "SQL Developer still seems to be running — Windows will not let the "
                    + "installer replace a JAR it has open:\n  " + target
                    + "\nQuit SQL Developer completely (File > Exit, then check Task Manager "
                    + "for a leftover javaw.exe) and run the installer again.", e);
            }
        }
    }

    /**
     * SQL Developer's OSGi boot (OracleIdeLauncher + equinox simpleconfigurator)
     * only loads bundles listed in configuration/bundles.info — a JAR dropped
     * into sqldeveloper/extensions is ignored until it appears there. Verified
     * against 26.2.0.186.2220: with this line present the bundle is resolved
     * and its embedded driver extracted into the netigso cache; '#' marker
     * comments are tolerated by the parser.
     */
    private void registerInBundlesInfo(Path installDir) throws IOException {
        Path bundlesInfo = Platform.bundlesInfo(installDir);
        if (!Files.isRegularFile(bundlesInfo)) {
            log.accept("(no configuration/bundles.info — pre-OSGi SQL Developer, list registration not needed)");
            return;
        }
        List<String> lines = Files.readAllLines(bundlesInfo, StandardCharsets.UTF_8);
        List<String> filtered = removeManagedBlockLines(lines);
        filtered.add(CONF_MARKER_START);
        filtered.add("com.fusionquery.sqldev,1.0.0,../sqldeveloper/extensions/" + EXTENSION_JAR + ",4,false");
        filtered.add(CONF_MARKER_END);
        Files.write(bundlesInfo, filtered, StandardCharsets.UTF_8);
        log.accept("Registered bundle in " + bundlesInfo);
    }

    private void copyBundledResource(String resourceName, Path target) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) throw new IOException("Bundled resource not found: " + resourceName);
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Patch the install dir's sqldeveloper.conf with the same managed block.
     * This is the most reliable entry point — the launcher always reads it,
     * even on portable installs where the user-dir product.conf is skipped.
     */
    private void updateLauncherConf(Path launcherConf, Path extDir) throws IOException {
        List<String> lines = Files.readAllLines(launcherConf, StandardCharsets.UTF_8);
        List<String> filtered = removeManagedBlockLines(lines);
        appendManagedBlock(filtered, extDir);
        Files.write(launcherConf, filtered, StandardCharsets.UTF_8);
    }

    private void appendManagedBlock(List<String> lines, Path extDir) {
        // Forward slashes: the conf format prefers them even on Windows, and
        // the canonical dir under %USERPROFILE%\Oracle contains no spaces.
        String driverJar = extDir.toAbsolutePath().resolve(DRIVER_JAR)
                .toString().replace('\\', '/');
        lines.add("");
        lines.add(CONF_MARKER_START);
        lines.add("AddJavaLibFile " + driverJar);
        lines.add(CONF_MARKER_END);
    }

    private void updateProductConf(Path productConf, Path extDir) throws IOException {
        if (!Files.isRegularFile(productConf)) {
            Files.createDirectories(productConf.getParent());
            Files.write(productConf, new ArrayList<String>(), StandardCharsets.UTF_8);
            log.accept("Created " + productConf + " (was missing — portable launcher reads sqldeveloper.conf from install root)");
        }
        List<String> lines = Files.readAllLines(productConf, StandardCharsets.UTF_8);
        List<String> filtered = removeManagedBlockLines(lines);

        appendManagedBlock(filtered, extDir);
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

    private Path productPreferences(SqlDevDetector.Detection d) {
        if (d.systemDir == null) return null;
        return d.systemDir.resolve("o.sqldeveloper").resolve("product-preferences.xml");
    }

    private void registerThirdPartyDriver(SqlDevDetector.Detection d, Path driverJar) {
        Path prefs = productPreferences(d);
        if (prefs == null) {
            log.accept("(no system dir for " + d + ", skipping Third Party JDBC registration)");
            return;
        }
        try {
            Document doc = loadOrCreatePrefs(prefs);
            Element list = ensureTpdriverList(doc);
            // SQL Developer resolves <url path="..."> with new File(prefsDir,
            // path), which converts absolute child paths to relative on
            // Windows — meaning both raw 'C:\...' values and file:// URIs end
            // up concatenated onto the product-preferences.xml location. The
            // format SQL Developer's own preferences GUI writes is a
            // relative-from-prefs path with forward slashes, so produce that.
            Path prefsDir = prefs.getParent().toAbsolutePath();
            String driverPath = prefsDir.relativize(driverJar.toAbsolutePath())
                    .toString().replace('\\', '/');

            // Remove any stale entry that points at our driver (any path that
            // ends in the canonical jar filename — covers historical absolute,
            // relative, and file:// variants).
            NodeList urls = list.getElementsByTagName("url");
            for (int i = urls.getLength() - 1; i >= 0; i--) {
                Element u = (Element) urls.item(i);
                String p = u.getAttribute("path");
                if (p != null && (p.equals(driverPath)
                        || p.endsWith("/" + DRIVER_JAR)
                        || p.endsWith("\\" + DRIVER_JAR))) {
                    list.removeChild(u);
                }
            }
            Element url = doc.createElement("url");
            url.setAttribute("path", driverPath);
            url.setAttribute("jar-entry", "");
            list.appendChild(url);
            writePrefs(doc, prefs);
            log.accept("Registered driver as Third Party JDBC -> " + driverPath);
        } catch (Exception e) {
            log.accept("WARNING: could not update " + prefs + ": " + e.getMessage());
        }
    }

    private void unregisterThirdPartyDriver(SqlDevDetector.Detection d) {
        Path prefs = productPreferences(d);
        if (prefs == null || !Files.isRegularFile(prefs)) return;
        try {
            Document doc = loadPrefs(prefs);
            NodeList lists = doc.getElementsByTagName("list");
            for (int i = 0; i < lists.getLength(); i++) {
                Element list = (Element) lists.item(i);
                if (!"TPDRIVER".equals(list.getAttribute("n"))) continue;
                NodeList urls = list.getElementsByTagName("url");
                for (int j = urls.getLength() - 1; j >= 0; j--) {
                    Element u = (Element) urls.item(j);
                    String p = u.getAttribute("path");
                    if (p != null && p.endsWith(DRIVER_JAR)) {
                        list.removeChild(u);
                    }
                }
            }
            writePrefs(doc, prefs);
            log.accept("Unregistered Third Party JDBC entry from " + prefs);
        } catch (Exception e) {
            log.accept("WARNING: could not update " + prefs + ": " + e.getMessage());
        }
    }

    private Document loadPrefs(Path prefs) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        try (InputStream in = Files.newInputStream(prefs)) {
            return db.parse(in);
        }
    }

    private Document loadOrCreatePrefs(Path prefs) throws Exception {
        if (Files.isRegularFile(prefs)) return loadPrefs(prefs);
        Files.createDirectories(prefs.getParent());
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();
        Element root = doc.createElementNS("http://xmlns.oracle.com/ide/hash", "ide:preferences");
        doc.appendChild(root);
        return doc;
    }

    private Element ensureTpdriverList(Document doc) {
        Element root = doc.getDocumentElement();

        Element dbConfig = findChildHash(root, "DBConfig");
        if (dbConfig == null) {
            dbConfig = doc.createElement("hash");
            dbConfig.setAttribute("n", "DBConfig");
            root.appendChild(dbConfig);
        }

        NodeList lists = dbConfig.getElementsByTagName("list");
        for (int i = 0; i < lists.getLength(); i++) {
            Element l = (Element) lists.item(i);
            if ("TPDRIVER".equals(l.getAttribute("n")) && l.getParentNode() == dbConfig) return l;
        }
        Element list = doc.createElement("list");
        list.setAttribute("n", "TPDRIVER");
        dbConfig.appendChild(list);
        return list;
    }

    private Element findChildHash(Element parent, String name) {
        NodeList kids = parent.getChildNodes();
        for (int i = 0; i < kids.getLength(); i++) {
            Node n = kids.item(i);
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            Element e = (Element) n;
            if ("hash".equals(e.getLocalName() != null ? e.getLocalName() : e.getNodeName())
                    && name.equals(e.getAttribute("n"))) return e;
        }
        return null;
    }

    private void writePrefs(Document doc, Path prefs) throws Exception {
        stripWhitespaceTextNodes(doc.getDocumentElement());
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t = tf.newTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3");
        try (OutputStream out = Files.newOutputStream(prefs)) {
            t.transform(new DOMSource(doc), new StreamResult(out));
        }
    }

    private static void stripWhitespaceTextNodes(Node node) {
        NodeList kids = node.getChildNodes();
        for (int i = kids.getLength() - 1; i >= 0; i--) {
            Node child = kids.item(i);
            if (child.getNodeType() == Node.TEXT_NODE
                    && child.getNodeValue() != null
                    && child.getNodeValue().trim().isEmpty()) {
                node.removeChild(child);
            } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                stripWhitespaceTextNodes(child);
            }
        }
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
