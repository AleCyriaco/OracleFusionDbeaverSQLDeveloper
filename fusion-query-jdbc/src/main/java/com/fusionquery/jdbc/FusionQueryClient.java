package com.fusionquery.jdbc;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class FusionQueryClient {

    private static final int DEFAULT_PAGE_SIZE = 1000;
    private static final String DEFAULT_REPORT_PATH = "/Custom/FusionQuery/Proxy/v1/csv.xdo";
    private static final Gson GSON = new Gson();

    private final String baseUrl;
    private String reportPath;
    private final String username;
    private final String password;
    private final int timeout;
    private final boolean verifySsl;
    private boolean useSoap = false;
    private FusionSoapClient soapClient;

    public FusionQueryClient(String baseUrl, String username, String password) {
        this(baseUrl, username, password, DEFAULT_REPORT_PATH, 120, true);
    }

    public FusionQueryClient(String baseUrl, String username, String password,
                             String reportPath, int timeoutSeconds, boolean verifySsl) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.username = username;
        this.password = password;
        this.reportPath = reportPath != null ? reportPath : DEFAULT_REPORT_PATH;
        this.timeout = timeoutSeconds * 1000;
        this.verifySsl = verifySsl;
    }

    public QueryResult query(String sql, int pageSize, int page) {
        int offset = page * pageSize;
        String paginatedSql = wrapPaginated(sql, offset, pageSize);
        String encoded = encodeSql(paginatedSql);

        try {
            byte[] csvBytes;
            if (useSoap) {
                csvBytes = getSoapClient().runReport(reportPath, encoded);
            } else {
                csvBytes = executeRest(encoded);
            }
            return parseCsvResponse(csvBytes, page, pageSize, offset);
        } catch (Exception e) {
            if (!useSoap) {
                useSoap = true;
                try {
                    byte[] csvBytes = getSoapClient().runReport(reportPath, encoded);
                    return parseCsvResponse(csvBytes, page, pageSize, offset);
                } catch (Exception e2) {
                    return QueryResult.error("REST failed: " + e.getMessage() + " | SOAP failed: " + e2.getMessage());
                }
            }
            return QueryResult.error(e.getMessage());
        }
    }

    public QueryResult query(String sql) {
        return query(sql, DEFAULT_PAGE_SIZE, 0);
    }

    public List<Map<String, String>> queryAll(String sql, int pageSize, int maxRows) {
        List<Map<String, String>> allRows = new ArrayList<>();
        int page = 0;
        while (true) {
            QueryResult result = query(sql, pageSize, page);
            if (result.hasError()) throw new RuntimeException(result.getError());
            allRows.addAll(result.getRows());
            if (!result.hasNext()) break;
            if (maxRows > 0 && allRows.size() >= maxRows) break;
            page++;
        }
        if (maxRows > 0 && allRows.size() > maxRows) {
            return allRows.subList(0, maxRows);
        }
        return allRows;
    }

    static String encodeSql(String sql) {
        try {
            byte[] utf8 = sql.getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
                gzip.write(utf8);
            }
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode SQL", e);
        }
    }

    static String wrapPaginated(String sql, int offset, int fetchSize) {
        return "SELECT * FROM (\n" + sql.trim() + "\n) fusion_query_page\n"
                + "OFFSET " + offset + " ROWS FETCH NEXT " + fetchSize + " ROWS ONLY";
    }

    private byte[] executeRest(String encodedSql) throws IOException {
        String reportApiPath = reportPath.replace(".xdo", "");
        String urlStr = baseUrl + "/xmlpserver/services/rest/v1/reports"
                + reportApiPath + "/run";

        JsonObject item = new JsonObject();
        item.addProperty("name", "P_B64_CONTENT");
        JsonObject values = new JsonObject();
        JsonArray valArr = new JsonArray();
        valArr.add(encodedSql);
        values.add("item", valArr);
        item.add("values", values);

        JsonArray itemsArr = new JsonArray();
        itemsArr.add(item);

        JsonObject listOfParams = new JsonObject();
        listOfParams.add("item", itemsArr);

        JsonObject paramNameValues = new JsonObject();
        paramNameValues.add("listOfParamNameValues", listOfParams);

        JsonObject body = new JsonObject();
        body.addProperty("byPassCache", true);
        body.addProperty("flattenXML", false);
        body.addProperty("attributeFormat", "csv");
        body.add("parameterNameValues", paramNameValues);

        HttpURLConnection conn = openConnection(urlStr, "POST");
        conn.setRequestProperty("Content-Type", "application/json");

        byte[] payload = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(payload.length);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload);
        }

        int status = conn.getResponseCode();
        if (status != 200) {
            InputStream errIs = wrapIfGzip(conn.getErrorStream(), conn.getContentEncoding());
            String errBody = readStream(errIs);
            throw new IOException("HTTP " + status + ": " + errBody);
        }

        InputStream respIs = wrapIfGzip(conn.getInputStream(), conn.getContentEncoding());
        String responseStr = readStream(respIs);
        JsonObject resp = GSON.fromJson(responseStr, JsonObject.class);
        String reportBytes = resp.get("reportBytes").getAsString();
        return Base64.getDecoder().decode(reportBytes);
    }

    private QueryResult parseCsvResponse(byte[] csvBytes, int page, int pageSize, int offset) {
        String csv = new String(csvBytes, StandardCharsets.UTF_8).trim();
        if (csv.isEmpty()) {
            return new QueryResult(Collections.emptyList(), Collections.emptyList(),
                    page, pageSize, offset, false, null);
        }

        String[] lines = csv.split("\n");
        if (lines.length == 0) {
            return new QueryResult(Collections.emptyList(), Collections.emptyList(),
                    page, pageSize, offset, false, null);
        }

        String[] headers = parsePipeLine(lines[0]);
        List<String> columns = Arrays.asList(headers);
        List<Map<String, String>> rows = new ArrayList<>();

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            String[] vals = parsePipeLine(line);
            Map<String, String> row = new LinkedHashMap<>();
            for (int j = 0; j < headers.length; j++) {
                row.put(headers[j], j < vals.length ? vals[j] : null);
            }
            rows.add(row);
        }

        boolean hasNext = rows.size() >= pageSize;
        return new QueryResult(columns, rows, page, pageSize, offset, hasNext, null);
    }

    private static String[] parsePipeLine(String line) {
        String[] parts = line.split("\\|", -1);
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
            if (parts[i].startsWith("\"") && parts[i].endsWith("\"")) {
                parts[i] = parts[i].substring(1, parts[i].length() - 1);
            }
        }
        return parts;
    }

    private HttpURLConnection openConnection(String urlStr, String method) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setDoOutput(true);
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);

        String auth = username + ":" + password;
        String encoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        conn.setRequestProperty("Authorization", "Basic " + encoded);

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

    private FusionSoapClient getSoapClient() {
        if (soapClient == null) {
            soapClient = new FusionSoapClient(baseUrl, username, password, timeout);
        }
        return soapClient;
    }

    public String getBaseUrl()  { return baseUrl; }
    public String getUsername()  { return username; }
    public String getPassword() { return password; }
    public int getTimeoutMs()   { return timeout; }
    public boolean isUsingSoap() { return useSoap; }

    public void setReportPath(String path) { this.reportPath = path; }
    public void setUseSoap(boolean soap)   { this.useSoap = soap; }
}
