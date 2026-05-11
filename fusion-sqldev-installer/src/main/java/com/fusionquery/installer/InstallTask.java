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

        // 1) Canonical standalone location used by DBeaver / DataGrip / IntelliJ
        Path standaloneDir = platform.standaloneDir();
        Files.createDirectories(standaloneDir);
        Path standaloneDriver = standaloneDir.resolve(DRIVER_JAR);
        Path standaloneExt = standaloneDir.resolve(EXTENSION_JAR);
        copyBundledResource(DRIVER_JAR, standaloneDriver);
        copyBundledResource(EXTENSION_JAR, standaloneExt);
        log.accept("Created standalone folder: " + standaloneDir);

        // 2) user_extensions copy of the extension (where SQL Developer scans)
        Path extDir = userDir.resolve(EXTENSIONS_DIR_NAME);
        Files.createDirectories(extDir);
        Path extTarget = extDir.resolve(EXTENSION_JAR);
        copyBundledResource(EXTENSION_JAR, extTarget);
        log.accept("Copied extension JAR -> " + extTarget);

        if (detections.isEmpty()) {
            log.accept("No SQL Developer version directories found yet. The JARs are installed; "
                + "launch SQL Developer once so it creates its config, then re-run the installer "
                + "to enable the connection type and register the driver.");
            return;
        }

        for (SqlDevDetector.Detection d : detections) {
            log.accept("--- " + d + " ---");
            updateProductConf(d.productConf, extDir);
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
        Files.deleteIfExists(extDir.resolve(EXTENSION_JAR));
        Files.deleteIfExists(extDir.resolve(DRIVER_JAR));

        Path standaloneDir = platform.standaloneDir();
        Files.deleteIfExists(standaloneDir.resolve(EXTENSION_JAR));
        Files.deleteIfExists(standaloneDir.resolve(DRIVER_JAR));
        try {
            if (Files.isDirectory(standaloneDir)
                    && !Files.list(standaloneDir).findAny().isPresent()) {
                Files.delete(standaloneDir);
            }
        } catch (IOException ignored) {}
        log.accept("Removed extension and driver JARs.");

        for (SqlDevDetector.Detection d : detections) {
            removeManagedBlock(d.productConf);
            unregisterThirdPartyDriver(d);
            clearCache(d.systemCache());
            log.accept("Cleaned " + d);
        }
        log.accept("Uninstall complete.");
    }

    private void copyBundledResource(String resourceName, Path target) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) throw new IOException("Bundled resource not found: " + resourceName);
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void updateProductConf(Path productConf, Path extDir) throws IOException {
        if (!Files.isRegularFile(productConf)) {
            Files.createDirectories(productConf.getParent());
            Files.write(productConf, new ArrayList<String>(), StandardCharsets.UTF_8);
            log.accept("Created " + productConf + " (was missing — portable launcher reads sqldeveloper.conf from install root)");
        }
        List<String> lines = Files.readAllLines(productConf, StandardCharsets.UTF_8);
        List<String> filtered = removeManagedBlockLines(lines);

        String extDirStr = extDir.toAbsolutePath().toString();
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
            String driverPath = driverJar.toAbsolutePath().toString();

            // Remove any stale entry that points at a different copy of our driver,
            // then add the canonical one pointing at user_extensions/.
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
