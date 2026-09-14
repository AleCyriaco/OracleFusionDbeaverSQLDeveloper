package com.fusionquery.jdbc;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.w3c.dom.Document;

/** Creates the private proxy using the documented Publisher v2 CatalogService. */
public class FusionCatalogService {
    private static final String CATALOG_SOAP = "/xmlpserver/services/v2/CatalogService";
    private static final Pattern DM_URL_PATTERN =
            Pattern.compile("(<dataModel\\s+url=\")[^\"]*(\")");
    private static final Pattern METADATA_PATH_PATTERN = Pattern.compile(
            "(<key><!\\[CDATA\\[path\\]\\]></key>\\s*<value><!\\[CDATA\\[).*?(\\]\\]></value>)",
            Pattern.DOTALL);

    private final String baseUrl;
    private final String username;
    private final String password;
    private final int timeout;

    public FusionCatalogService(String baseUrl, String username, String password, int timeoutMs) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.username = username;
        this.password = password;
        this.timeout = timeoutMs;
    }

    /** Kept for callers of 1.0.0. Catalog operations now always use SOAP on all hosts. */
    @Deprecated
    public void setUseSoap(boolean useSoap) {}

    public DeployResult ensureDeployed() {
        String folder = "/~" + username + "/FusionQuery";
        String versionFolder = folder + "/v1";
        String dmPath = versionFolder + "/dm.xdm";
        String reportPath = versionFolder + "/csv.xdo";
        try {
            boolean hasModel = objectExists(dmPath);
            boolean hasReport = objectExists(reportPath);
            if (hasModel && hasReport) {
                return new DeployResult(true, false, reportPath, null);
            }

            // Validate the bundled archives before creating anything in the catalog.
            byte[][] template = loadTemplateObjects();
            byte[] model = patchArchive(template[0], null, dmPath);
            byte[] report = patchArchive(template[1], dmPath, reportPath);
            ensureFolder(folder);
            ensureFolder(versionFolder);
            if (!hasModel) uploadIfMissing(dmPath, model, "xdm");
            if (!hasReport) uploadIfMissing(reportPath, report, "xdo");
            if (!objectExists(dmPath) || !objectExists(reportPath)) {
                throw new IOException("Publisher did not confirm both the data model and report after upload");
            }
            return new DeployResult(true, true, reportPath, null);
        } catch (IOException e) {
            return new DeployResult(false, false, null,
                    "Cannot prepare BI Publisher proxy at " + reportPath + ": " + safeMessage(e)
                    + ". The Fusion user must be allowed to create folders, data models and reports in My Folders"
                    + " and use the data model's data source. No shared report was selected as a fallback.");
        }
    }

    private void ensureFolder(String path) throws IOException {
        if (objectExists(path)) return;
        try {
            call("createFolder", element("folderAbsolutePath", path));
        } catch (IOException failure) {
            // Another simultaneous DBeaver connection may have created this folder.
            if (!existsAfterFailure(path)) throw new IOException("Creating folder " + path + ": " + safeMessage(failure), failure);
        }
        if (!objectExists(path)) throw new IOException("Folder was not created: " + path);
    }

    private void uploadIfMissing(String path, byte[] content, String type) throws IOException {
        if (objectExists(path)) return;
        try {
            try {
                uploadObject(path, content, type);
            } catch (IOException failure) {
                // Some Fusion pods require the archive type despite the v2 API documenting xdm/xdo.
                // Retry only an explicit type rejection, never authentication or permission failures.
                if (!requiresArchiveType(failure, type)) throw failure;
                uploadObject(path, content, type + "z");
            }
        } catch (IOException failure) {
            if (!existsAfterFailure(path)) throw new IOException("Uploading " + path + ": " + safeMessage(failure), failure);
        }
    }

    private void uploadObject(String path, byte[] content, String type) throws IOException {
        call("uploadObject", element("reportObjectAbsolutePathURL", path)
                + element("objectType", type)
                + element("objectZippedData", Base64.getEncoder().encodeToString(content)));
    }

    private static boolean requiresArchiveType(IOException error, String type) {
        if (!"xdm".equals(type) && !"xdo".equals(type)) return false;
        String message = String.valueOf(error.getMessage()).toLowerCase(Locale.ROOT);
        return message.contains("unsupported report object's type")
                && message.contains("[" + type + "]")
                && message.contains(type + "z");
    }

    private boolean existsAfterFailure(String path) {
        try { return objectExists(path); }
        catch (IOException ignored) { return false; }
    }

    private boolean objectExists(String path) throws IOException {
        Document response = call("objectExist", element("reportObjectAbsolutePath", path));
        String exists = SoapXml.text(response, "objectExistReturn");
        if ("true".equals(exists) || "1".equals(exists)) return true;
        if ("false".equals(exists) || "0".equals(exists)) return false;
        throw new IOException("Missing/invalid objectExistReturn while checking " + path);
    }

    private Document call(String operation, String arguments) throws IOException {
        String envelope = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\""
                + " xmlns:v2=\"http://xmlns.oracle.com/oxp/service/v2\"><s:Body><v2:" + operation + ">"
                + arguments + element("userID", username) + element("password", password)
                + "</v2:" + operation + "></s:Body></s:Envelope>";
        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + CATALOG_SOAP).openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            conn.setRequestProperty("SOAPAction", "");
            conn.setRequestProperty("Accept-Encoding", "gzip");
            conn.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString(
                    (username + ":" + password).getBytes(StandardCharsets.UTF_8)));
            byte[] payload = envelope.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(payload.length);
            try (OutputStream output = conn.getOutputStream()) { output.write(payload); }
            int status = conn.getResponseCode();
            InputStream stream = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String response = "";
            if (stream != null) {
                try (InputStream input = "gzip".equalsIgnoreCase(conn.getContentEncoding())
                        ? new GZIPInputStream(stream) : stream) {
                    response = new String(readBytes(input), StandardCharsets.UTF_8);
                }
            }
            Document document;
            try {
                document = SoapXml.parse(response);
            } catch (IOException invalidXml) {
                throw new IOException(operation + " returned HTTP " + status
                        + " without a valid SOAP response from " + CATALOG_SOAP, invalidXml);
            }
            SoapXml.checkFault(document);
            if (status < 200 || status >= 300) {
                throw new IOException(operation + " returned HTTP " + status + " from " + CATALOG_SOAP);
            }
            if (document.getElementsByTagNameNS("*", operation + "Response").getLength() == 0) {
                throw new IOException("Missing " + operation + "Response from BI Publisher");
            }
            return document;
        } catch (IOException e) {
            throw new IOException(safeMessage(e));
        } finally {
            conn.disconnect();
        }
    }

    private String safeMessage(Exception error) {
        String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        if (password != null && !password.isEmpty()) {
            message = message.replace(password, "[redacted]").replace(SoapXml.escape(password), "[redacted]");
            message = message.replace(Base64.getEncoder().encodeToString(
                    (username + ":" + password).getBytes(StandardCharsets.UTF_8)), "[redacted]");
        }
        return message;
    }

    private static String element(String name, String value) {
        return "<v2:" + name + ">" + SoapXml.escape(value) + "</v2:" + name + ">";
    }

    private byte[][] loadTemplateObjects() throws IOException {
        byte[] model = null;
        byte[] report = null;
        try (InputStream input = getClass().getResourceAsStream("/FusionQueryProxy.xdrz")) {
            if (input == null) throw new IOException("Bundled template FusionQueryProxy.xdrz not found in JAR");
            try (ZipInputStream zip = new ZipInputStream(input)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (entry.getName().endsWith("dm.xdmz")) model = readBytes(zip);
                    else if (entry.getName().endsWith("csv.xdoz")) report = readBytes(zip);
                }
            }
        }
        if (model == null || report == null) throw new IOException("Invalid template: missing dm.xdmz or csv.xdoz");
        return new byte[][]{model, report};
    }

    static byte[] patchReportDmPath(byte[] archive, String dmPath) throws IOException {
        return patchArchive(archive, dmPath, null);
    }

    private static byte[] patchArchive(byte[] archive, String dmPath, String catalogPath) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        boolean modelReferencePatched = false;
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(archive));
             ZipOutputStream output = new ZipOutputStream(result)) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                byte[] data = readBytes(input);
                if (dmPath != null && entry.getName().endsWith("_report.xdo")) {
                    String xml = new String(data, StandardCharsets.UTF_8);
                    Matcher matcher = DM_URL_PATTERN.matcher(xml);
                    if (!matcher.find()) throw new IOException("Report template has no data model URL");
                    xml = matcher.replaceFirst("$1" + Matcher.quoteReplacement(SoapXml.escape(dmPath)) + "$2");
                    data = xml.getBytes(StandardCharsets.UTF_8);
                    modelReferencePatched = true;
                } else if (catalogPath != null && entry.getName().endsWith("~metadata.meta")) {
                    String xml = new String(data, StandardCharsets.UTF_8);
                    xml = METADATA_PATH_PATTERN.matcher(xml).replaceFirst("$1"
                            + Matcher.quoteReplacement(URLEncoder.encode(catalogPath, "UTF-8")) + "$2");
                    data = xml.getBytes(StandardCharsets.UTF_8);
                }
                output.putNextEntry(new ZipEntry(entry.getName()));
                output.write(data);
                output.closeEntry();
            }
        }
        if (dmPath != null && !modelReferencePatched) throw new IOException("Invalid archive: missing _report.xdo");
        return result.toByteArray();
    }

    static byte[] readBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int size;
        while ((size = input.read(buffer)) != -1) output.write(buffer, 0, size);
        return output.toByteArray();
    }

    public static class DeployResult {
        public final boolean found;
        public final boolean wasInstalled;
        public final String reportPath;
        public final String error;

        DeployResult(boolean found, boolean wasInstalled, String reportPath, String error) {
            this.found = found;
            this.wasInstalled = wasInstalled;
            this.reportPath = reportPath;
            this.error = error;
        }
    }
}
