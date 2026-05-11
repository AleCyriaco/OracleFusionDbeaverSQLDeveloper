package com.fusionquery.jdbc;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FusionCatalogService {

    private static final String CATALOG_REST = "/xmlpserver/services/rest/v1/catalogservice";
    private static final String CATALOG_SOAP = "/xmlpserver/services/v2/CatalogService";
    private static final Pattern DM_URL_PATTERN =
            Pattern.compile("(<dataModel\\s+url=\")[^\"]*(\")");

    private final String baseUrl;
    private final String username;
    private final String password;
    private final int timeout;
    private boolean useSoap = false;

    public FusionCatalogService(String baseUrl, String username, String password, int timeoutMs) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.username = username;
        this.password = password;
        this.timeout = timeoutMs;
    }

    public void setUseSoap(boolean useSoap) {
        this.useSoap = useSoap;
    }

    public DeployResult ensureDeployed() {
        String userReportPath = "/~" + username + "/FusionQuery/v1/csv.xdo";
        String sharedReportPath = "/Custom/FusionQuery/Proxy/v1/csv.xdo";

        try {
            if (objectExists(userReportPath)) {
                return new DeployResult(true, false, userReportPath, null);
            }
        } catch (Exception ignored) {}

        try {
            if (objectExists(sharedReportPath)) {
                return new DeployResult(true, false, sharedReportPath, null);
            }
        } catch (Exception ignored) {}

        try {
            String deployedPath = deployToUserFolder();
            return new DeployResult(true, true, deployedPath, null);
        } catch (Exception e) {
            return new DeployResult(false, false, null,
                    "Auto-deploy failed: " + e.getMessage());
        }
    }

    private String deployToUserFolder() throws IOException {
        String folder = "/~" + username + "/FusionQuery";
        String v1Folder = folder + "/v1";
        String dmPath = v1Folder + "/dm.xdm";
        String reportPath = v1Folder + "/csv.xdo";

        createFolder(folder);
        createFolder(v1Folder);

        byte[] templateBytes = loadBundledTemplate();
        byte[] dmContent = null;
        byte[] reportContent = null;

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(templateBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                byte[] data = zis.readAllBytes();
                if (name.endsWith("dm.xdmz")) {
                    dmContent = data;
                } else if (name.endsWith("csv.xdoz")) {
                    reportContent = data;
                }
                zis.closeEntry();
            }
        }

        if (dmContent == null || reportContent == null) {
            throw new IOException("Invalid template: missing dm.xdmz or csv.xdoz");
        }

        reportContent = patchReportDmPath(reportContent, dmPath);

        uploadObject(dmPath, dmContent, "xdmz");
        uploadObject(reportPath, reportContent, "xdoz");

        return reportPath;
    }

    static byte[] patchReportDmPath(byte[] xdozBytes, String newDmPath) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(xdozBytes));
             ZipOutputStream zos = new ZipOutputStream(result)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                byte[] data = zis.readAllBytes();
                ZipEntry newEntry = new ZipEntry(entry.getName());
                zos.putNextEntry(newEntry);

                if (entry.getName().endsWith("_report.xdo")) {
                    String xml = new String(data, StandardCharsets.UTF_8);
                    Matcher m = DM_URL_PATTERN.matcher(xml);
                    xml = m.replaceFirst("$1" + Matcher.quoteReplacement(newDmPath) + "$2");
                    zos.write(xml.getBytes(StandardCharsets.UTF_8));
                } else {
                    zos.write(data);
                }

                zos.closeEntry();
                zis.closeEntry();
            }
        }
        return result.toByteArray();
    }

    private boolean objectExists(String path) throws IOException {
        if (useSoap) {
            return objectExistsSoap(path);
        }
        return objectExistsRest(path);
    }

    private boolean objectExistsRest(String path) throws IOException {
        String urlStr = baseUrl + CATALOG_REST + "?objectAbsolutePath="
                + java.net.URLEncoder.encode(path, "UTF-8");
        HttpURLConnection conn = openConnection(urlStr, "GET");
        conn.setDoOutput(false);
        int status = conn.getResponseCode();
        conn.disconnect();
        return status == 200;
    }

    private boolean objectExistsSoap(String path) throws IOException {
        String parentFolder = path.substring(0, path.lastIndexOf('/'));
        String fileName = path.substring(path.lastIndexOf('/') + 1);

        String soap = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\""
                + " xmlns:v2=\"http://xmlns.oracle.com/oxp/service/v2\">\n"
                + "  <soapenv:Body>\n"
                + "    <v2:getFolderContents>\n"
                + "      <v2:folderAbsolutePath>" + escapeXml(parentFolder) + "</v2:folderAbsolutePath>\n"
                + "      <v2:userID>" + escapeXml(username) + "</v2:userID>\n"
                + "      <v2:password>" + escapeXml(password) + "</v2:password>\n"
                + "    </v2:getFolderContents>\n"
                + "  </soapenv:Body>\n"
                + "</soapenv:Envelope>";

        String response = postSoap(CATALOG_SOAP, soap);
        return response.contains("<fileName>" + fileName + "</fileName>")
                || response.contains("<fileName>" + fileName.replace(".xdo", "") + "</fileName>");
    }

    private void createFolder(String folderPath) throws IOException {
        if (useSoap) {
            createFolderSoap(folderPath);
        } else {
            createFolderRest(folderPath);
        }
    }

    private void createFolderRest(String folderPath) throws IOException {
        String parent = folderPath.substring(0, folderPath.lastIndexOf('/'));
        String name = folderPath.substring(folderPath.lastIndexOf('/') + 1);
        if (parent.isEmpty()) parent = "/";

        String urlStr = baseUrl + CATALOG_REST + "/folder";
        String body = "{\"folderAbsolutePathURL\":\"" + escapeJson(parent)
                + "\",\"folderName\":\"" + escapeJson(name) + "\"}";

        HttpURLConnection conn = openConnection(urlStr, "POST");
        conn.setRequestProperty("Content-Type", "application/json");
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(payload.length);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload);
        }
        int status = conn.getResponseCode();
        conn.disconnect();
        if (status != 200 && status != 201 && status != 409) {
            throw new IOException("Failed to create folder " + folderPath + ": HTTP " + status);
        }
    }

    private void createFolderSoap(String folderPath) throws IOException {
        String soap = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\""
                + " xmlns:v2=\"http://xmlns.oracle.com/oxp/service/v2\">\n"
                + "  <soapenv:Body>\n"
                + "    <v2:createFolder>\n"
                + "      <v2:folderAbsolutePath>" + escapeXml(folderPath) + "</v2:folderAbsolutePath>\n"
                + "      <v2:userID>" + escapeXml(username) + "</v2:userID>\n"
                + "      <v2:password>" + escapeXml(password) + "</v2:password>\n"
                + "    </v2:createFolder>\n"
                + "  </soapenv:Body>\n"
                + "</soapenv:Envelope>";

        String response = postSoap(CATALOG_SOAP, soap);
        if (response.contains("Fault") && !response.contains("already exists")) {
            throw new IOException("SOAP createFolder failed for " + folderPath);
        }
    }

    private void uploadObject(String catalogPath, byte[] content, String objectType) throws IOException {
        if (useSoap) {
            uploadObjectSoap(catalogPath, content, objectType);
        } else {
            uploadObjectRest(catalogPath, content, objectType);
        }
    }

    private void uploadObjectRest(String catalogPath, byte[] content, String objectType) throws IOException {
        String urlStr = baseUrl + CATALOG_REST;
        String b64 = Base64.getEncoder().encodeToString(content);
        String body = "{\"objectAbsolutePathURL\":\"" + escapeJson(catalogPath)
                + "\",\"objectType\":\"" + escapeJson(objectType)
                + "\",\"objectData\":\"" + b64 + "\"}";

        HttpURLConnection conn = openConnection(urlStr, "POST");
        conn.setRequestProperty("Content-Type", "application/json");
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(payload.length);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload);
        }
        int status = conn.getResponseCode();
        conn.disconnect();
        if (status != 200 && status != 201) {
            throw new IOException("Failed to upload " + catalogPath + ": HTTP " + status);
        }
    }

    private void uploadObjectSoap(String catalogPath, byte[] content, String objectType) throws IOException {
        String b64 = Base64.getEncoder().encodeToString(content);
        String soap = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\""
                + " xmlns:v2=\"http://xmlns.oracle.com/oxp/service/v2\">\n"
                + "  <soapenv:Body>\n"
                + "    <v2:uploadObject>\n"
                + "      <v2:reportObjectAbsolutePathURL>" + escapeXml(catalogPath) + "</v2:reportObjectAbsolutePathURL>\n"
                + "      <v2:objectType>" + escapeXml(objectType) + "</v2:objectType>\n"
                + "      <v2:objectZippedData>" + b64 + "</v2:objectZippedData>\n"
                + "      <v2:userID>" + escapeXml(username) + "</v2:userID>\n"
                + "      <v2:password>" + escapeXml(password) + "</v2:password>\n"
                + "    </v2:uploadObject>\n"
                + "  </soapenv:Body>\n"
                + "</soapenv:Envelope>";

        String response = postSoap(CATALOG_SOAP, soap);
        if (response.contains("Fault")) {
            throw new IOException("SOAP upload failed for " + catalogPath);
        }
    }

    private String postSoap(String servicePath, String soapBody) throws IOException {
        String urlStr = baseUrl + servicePath;
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
        conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        conn.setRequestProperty("SOAPAction", "");

        String auth = username + ":" + password;
        conn.setRequestProperty("Authorization",
                "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8)));

        byte[] payload = soapBody.getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(payload.length);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload);
        }

        int status = conn.getResponseCode();
        InputStream rawIs = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
        InputStream is = wrapIfGzip(rawIs, conn.getContentEncoding());
        String response = readStream(is);
        conn.disconnect();

        if (status >= 400 && !response.contains("already exists")) {
            throw new IOException("SOAP HTTP " + status + ": " + response);
        }
        return response;
    }

    private byte[] loadBundledTemplate() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/FusionQueryProxy.xdrz")) {
            if (is == null) {
                throw new IOException("Bundled template FusionQueryProxy.xdrz not found in JAR");
            }
            return is.readAllBytes();
        }
    }

    private HttpURLConnection openConnection(String urlStr, String method) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setDoOutput(true);
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);

        String auth = username + ":" + password;
        conn.setRequestProperty("Authorization",
                "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8)));
        return conn;
    }

    private static String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString().trim();
        }
    }

    private static InputStream wrapIfGzip(InputStream is, String contentEncoding) throws IOException {
        if (is == null) return new ByteArrayInputStream(new byte[0]);
        if ("gzip".equalsIgnoreCase(contentEncoding)) {
            return new GZIPInputStream(is);
        }
        return is;
    }

    private static String escapeXml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
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
