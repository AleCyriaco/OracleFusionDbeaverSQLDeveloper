package com.fusionquery.jdbc;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class FusionSoapClient {

    private static final String REPORT_SERVICE_PATH = "/xmlpserver/services/v2/ReportService";
    private static final Pattern REPORT_BYTES_PATTERN =
            Pattern.compile("<reportBytes>([^<]+)</reportBytes>");
    private static final Pattern FAULT_PATTERN =
            Pattern.compile("<faultstring>([^<]+)</faultstring>");

    private final String baseUrl;
    private final String username;
    private final String password;
    private final int timeout;

    public FusionSoapClient(String baseUrl, String username, String password, int timeoutMs) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.username = username;
        this.password = password;
        this.timeout = timeoutMs;
    }

    public byte[] runReport(String reportPath, String encodedSql) throws IOException {
        String soapBody = buildRunReportEnvelope(reportPath, encodedSql);
        String urlStr = baseUrl + REPORT_SERVICE_PATH;

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
        conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        conn.setRequestProperty("SOAPAction", "");
        conn.setRequestProperty("Accept-Encoding", "gzip, deflate");

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

        if (status >= 400) {
            Matcher faultMatcher = FAULT_PATTERN.matcher(response);
            String msg;
            if (faultMatcher.find()) {
                msg = "SOAP Fault: " + faultMatcher.group(1);
            } else {
                msg = "SOAP HTTP " + status + ": "
                        + (response.length() > 500 ? response.substring(0, 500) : response);
            }
            throw new IOException(msg);
        }

        Matcher m = REPORT_BYTES_PATTERN.matcher(response);
        if (!m.find()) {
            throw new IOException("No reportBytes in SOAP response");
        }
        return Base64.getDecoder().decode(m.group(1));
    }

    private String buildRunReportEnvelope(String reportPath, String encodedSql) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\""
                + " xmlns:v2=\"http://xmlns.oracle.com/oxp/service/v2\">\n"
                + "  <soapenv:Body>\n"
                + "    <v2:runReport>\n"
                + "      <v2:reportRequest>\n"
                + "        <v2:reportAbsolutePath>" + escapeXml(reportPath) + "</v2:reportAbsolutePath>\n"
                + "        <v2:attributeFormat>csv</v2:attributeFormat>\n"
                + "        <v2:byPassCache>true</v2:byPassCache>\n"
                + "        <v2:flattenXML>false</v2:flattenXML>\n"
                + "        <v2:parameterNameValues>\n"
                + "          <v2:listOfParamNameValues>\n"
                + "            <v2:item>\n"
                + "              <v2:name>P_B64_CONTENT</v2:name>\n"
                + "              <v2:values>\n"
                + "                <v2:item>" + escapeXml(encodedSql) + "</v2:item>\n"
                + "              </v2:values>\n"
                + "            </v2:item>\n"
                + "          </v2:listOfParamNameValues>\n"
                + "        </v2:parameterNameValues>\n"
                + "      </v2:reportRequest>\n"
                + "      <v2:userID>" + escapeXml(username) + "</v2:userID>\n"
                + "      <v2:password>" + escapeXml(password) + "</v2:password>\n"
                + "    </v2:runReport>\n"
                + "  </soapenv:Body>\n"
                + "</soapenv:Envelope>";
    }

    private static InputStream wrapIfGzip(InputStream is, String contentEncoding) throws IOException {
        if (is == null) return new ByteArrayInputStream(new byte[0]);
        if ("gzip".equalsIgnoreCase(contentEncoding)) {
            return new GZIPInputStream(is);
        }
        return is;
    }

    private static String escapeXml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
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
}
