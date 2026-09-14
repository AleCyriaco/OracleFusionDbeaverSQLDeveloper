package com.fusionquery.jdbc;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;
import java.util.zip.*;
import org.junit.*;
import org.w3c.dom.Document;
import static org.junit.Assert.*;

/** Exercises the actual catalog/report HTTP clients against an in-memory Publisher server. */
public class AutoDeployTest {
    private HttpServer server;
    private String baseUrl;
    private final Set<String> objects = new HashSet<>();
    private final Map<String, byte[]> uploads = new HashMap<>();
    private final List<String> calls = new ArrayList<>();
    private final List<String> uploadTypes = new ArrayList<>();
    private boolean requireArchiveTypes;
    private boolean denyArchiveUpload;
    private String failOperation;
    private String failPath;
    private int faultStatus = 500;
    private String faultMessage = "Permission denied";
    private boolean raceOnCreate;
    private boolean dropReportUpload;
    private boolean malformedExists;
    private String csv = "FUSION_QUERY_CHECK\n1\n";
    private String lastReportPath;
    private Throwable serverFailure;
    private static final String ROOT = "/~tester/FusionQuery";
    private static final String MODEL = ROOT + "/v1/dm.xdm";
    private static final String REPORT = ROOT + "/v1/csv.xdo";

    @Before public void start() throws Exception {
        objects.add("/~tester");
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/xmlpserver/services/v2/", this::handle);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @After public void stop() {
        if (server != null) server.stop(0);
        if (serverFailure != null) throw new AssertionError("Mock Publisher rejected request", serverFailure);
    }

    private FusionConnection connect(String path) throws SQLException {
        Properties properties = new Properties();
        if (path != null) properties.setProperty("reportPath", path);
        return new FusionConnection(new FusionQueryClient(baseUrl, "tester", "test-secret", path, 5, true),
                baseUrl, properties);
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            Document request = SoapXml.parse(new String(FusionCatalogService.readBytes(exchange.getRequestBody()),
                    StandardCharsets.UTF_8));
            assertEquals("tester", SoapXml.text(request, "userID"));
            assertEquals("test-secret", SoapXml.text(request, "password"));
            String operation = null;
            for (String name : Arrays.asList("objectExist", "createFolder", "uploadObject", "runReport")) {
                if (request.getElementsByTagNameNS("*", name).getLength() > 0) operation = name;
            }
            assertNotNull("Unknown SOAP operation", operation);
            String path = SoapXml.text(request, "reportObjectAbsolutePath");
            if (path == null) path = SoapXml.text(request, "folderAbsolutePath");
            if (path == null) path = SoapXml.text(request, "reportObjectAbsolutePathURL");
            if (path == null) path = SoapXml.text(request, "reportAbsolutePath");
            calls.add(operation + " " + path);
            if (operation.equals("uploadObject")) uploadTypes.add(SoapXml.text(request, "objectType"));
            if (operation.equals(failOperation) && (failPath == null || failPath.equals(path))) {
                fault(exchange, faultStatus, faultMessage);
                return;
            }
            String value;
            if (operation.equals("objectExist")) {
                value = malformedExists ? "not-a-boolean" : String.valueOf(objects.contains(path));
            } else if (operation.equals("createFolder")) {
                assertTrue("Missing parent for " + path, objects.contains(path.substring(0, path.lastIndexOf('/'))));
                objects.add(path);
                if (raceOnCreate) {
                    fault(exchange, 500, "Folder already exists");
                    return;
                }
                value = path;
            } else if (operation.equals("uploadObject")) {
                assertTrue(objects.contains(path.substring(0, path.lastIndexOf('/'))));
                String type = SoapXml.text(request, "objectType");
                String baseType = path.endsWith(".xdm") ? "xdm" : "xdo";
                if (requireArchiveTypes && type.equals(baseType)) {
                    fault(exchange, faultStatus, "PublicReportServiceImpl::executeUploadReport Failure: "
                            + "due to unsupported Report Object's type - [" + type
                            + "]. Only support types - xdoz / xdmz / xssz /  / xmaz / xsbzxdrz.");
                    return;
                }
                assertEquals(requireArchiveTypes ? baseType + "z" : baseType, type);
                if (denyArchiveUpload) {
                    fault(exchange, 500, "Permission denied for archive upload");
                    return;
                }
                byte[] archive = Base64.getDecoder().decode(SoapXml.text(request, "objectZippedData"));
                assertTrue(unzip(archive).size() > 0);
                uploads.put(path, archive);
                if (!dropReportUpload || !path.endsWith(".xdo")) objects.add(path);
                value = path;
            } else {
                lastReportPath = path;
                assertEquals("P_B64_CONTENT", SoapXml.text(request, "name"));
                assertTrue("Missing report", objects.contains(path));
                String encoded = request.getElementsByTagNameNS("*", "values").item(0).getTextContent().trim();
                byte[] zipped = Base64.getDecoder().decode(encoded);
                String sql = new String(FusionCatalogService.readBytes(new GZIPInputStream(
                        new ByteArrayInputStream(zipped))), StandardCharsets.UTF_8);
                assertTrue(sql.contains("SELECT 1 AS FUSION_QUERY_CHECK FROM DUAL"));
                value = "<p:reportBytes>" + Base64.getEncoder().encodeToString(csv.getBytes(StandardCharsets.UTF_8))
                        + "</p:reportBytes>";
            }
            respond(exchange, 200, "<p:" + operation + "Response><p:" + operation + "Return>"
                    + (operation.equals("runReport") ? value : SoapXml.escape(value))
                    + "</p:" + operation + "Return></p:" + operation + "Response>");
        } catch (Throwable e) {
            serverFailure = e;
            fault(exchange, 500, "Mock request validation failed");
        } finally { exchange.close(); }
    }

    private void fault(HttpExchange exchange, int status, String message) throws IOException {
        respond(exchange, status, "<s:Fault><faultcode>s:Server</faultcode><faultstring>"
                + SoapXml.escape(message) + "</faultstring></s:Fault>");
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = ("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\""
                + " xmlns:p=\"http://xmlns.oracle.com/oxp/service/v2\"><s:Body>" + body
                + "</s:Body></s:Envelope>").getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/xml");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private static Map<String, String> unzip(byte[] bytes) throws IOException {
        Map<String, String> entries = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries.put(entry.getName(), new String(FusionCatalogService.readBytes(zip), StandardCharsets.UTF_8));
            }
        }
        return entries;
    }

    private SQLException expectFailure() throws Exception {
        try { connect(null); fail("Connection must fail during preparation"); return null; }
        catch (SQLException error) { assertEquals("08001", error.getSQLState()); return error; }
    }

    @Test public void firstConnectionCreatesBothObjectsAndRunsValidation() throws Exception {
        try (FusionConnection connection = connect(null)) {
            assertEquals(REPORT, lastReportPath);
            assertEquals(2, uploads.size());
            assertEquals(Arrays.asList("xdm", "xdo"), uploadTypes);
            assertNotNull(connection.getWarnings());
            Map<String, String> report = unzip(uploads.get(REPORT));
            assertTrue(report.get("_report.xdo").contains("url=\"" + MODEL + "\""));
            assertTrue(report.get("~metadata.meta").contains("%2F%7Etester%2FFusionQuery"));
            assertTrue(unzip(uploads.get(MODEL)).get("~metadata.meta").contains("%2F%7Etester%2FFusionQuery"));
            assertFalse(calls.toString().contains("/Custom/"));
        }
    }

    @Test public void reconnectReusesObjectsWithoutWriting() throws Exception {
        connect(null).close();
        calls.clear(); uploads.clear();
        try (FusionConnection connection = connect(null)) {
            assertTrue(uploads.isEmpty());
            assertFalse(calls.toString().contains("createFolder"));
            assertNull(connection.getWarnings());
            assertEquals(REPORT, lastReportPath);
        }
    }

    @Test public void completesInterruptedModelOnlyDeployment() throws Exception {
        objects.add(ROOT); objects.add(ROOT + "/v1"); objects.add(MODEL);
        connect(null).close();
        assertEquals(Collections.singleton(REPORT), uploads.keySet());
    }

    @Test public void completesInterruptedReportOnlyDeployment() throws Exception {
        objects.add(ROOT); objects.add(ROOT + "/v1"); objects.add(REPORT);
        connect(null).close();
        assertEquals(Collections.singleton(MODEL), uploads.keySet());
    }

    @Test public void explicitPathIsValidatedWithoutCatalogChanges() throws Exception {
        String path = "/Custom/Approved/csv.xdo";
        objects.add(path);
        connect(path).close();
        assertEquals(Collections.singletonList("runReport " + path), calls);
    }

    @Test public void blankPathTriggersDeployment() throws Exception {
        connect("   ").close();
        assertEquals(REPORT, lastReportPath);
    }

    @Test public void catalogDenialFailsConnectionWithoutRunningSharedReport() throws Exception {
        failOperation = "uploadObject"; failPath = MODEL;
        SQLException error = expectFailure();
        assertTrue(error.getMessage().contains("Uploading " + MODEL));
        assertTrue(error.getMessage().contains("Permission denied"));
        assertNull(lastReportPath);
        assertFalse(calls.toString().contains("/Custom/"));
        assertEquals(Collections.singletonList("xdm"), uploadTypes);
    }

    @Test public void archiveOnlyServerCreatesModelAndReport() throws Exception {
        requireArchiveTypes = true;
        connect(null).close();
        assertEquals(Arrays.asList("xdm", "xdmz", "xdo", "xdoz"), uploadTypes);
        assertEquals(2, uploads.size());
        assertEquals(REPORT, lastReportPath);
        assertTrue(unzip(uploads.get(REPORT)).get("_report.xdo").contains("url=\"" + MODEL + "\""));
        uploadTypes.clear();
        connect(null).close();
        assertTrue("Reconnect must not upload existing objects", uploadTypes.isEmpty());
    }

    @Test public void archiveOnlyHttp200FaultRetriesSupportedType() throws Exception {
        requireArchiveTypes = true;
        faultStatus = 200;
        connect(null).close();
        assertEquals(Arrays.asList("xdm", "xdmz", "xdo", "xdoz"), uploadTypes);
        assertEquals(REPORT, lastReportPath);
    }

    @Test public void permissionFailureOnArchiveRetryStopsImmediately() throws Exception {
        requireArchiveTypes = true;
        denyArchiveUpload = true;
        assertTrue(expectFailure().getMessage().contains("Permission denied for archive upload"));
        assertEquals(Arrays.asList("xdm", "xdmz"), uploadTypes);
        assertNull(lastReportPath);
    }

    @Test public void unsupportedTypeWithoutSupportedArchiveDoesNotRetry() throws Exception {
        failOperation = "uploadObject";
        faultMessage = "unsupported Report Object's type - [xdm]. Only support types - xdoz";
        assertTrue(expectFailure().getMessage().contains("unsupported Report Object's type"));
        assertEquals(Collections.singletonList("xdm"), uploadTypes);
    }

    @Test public void http200SoapFaultIsNotSuccess() throws Exception {
        failOperation = "createFolder"; faultStatus = 200;
        faultMessage = "Create denied & missing role for test-secret";
        SQLException error = expectFailure();
        assertTrue(error.getMessage().contains("Create denied & missing role"));
        assertFalse(error.getMessage().contains("test-secret"));
        assertNull(lastReportPath);
    }

    @Test public void inaccessibleCatalogIsNotTreatedAsMissing() throws Exception {
        failOperation = "objectExist";
        expectFailure();
        assertEquals(1, calls.size());
        assertTrue(uploads.isEmpty());
    }

    @Test public void missingBooleanIsNotTreatedAsMissingObject() throws Exception {
        malformedExists = true;
        assertTrue(expectFailure().getMessage().contains("objectExistReturn"));
        assertEquals(1, calls.size());
    }

    @Test public void concurrentFolderCreationIsRechecked() throws Exception {
        raceOnCreate = true;
        connect(null).close();
        assertEquals(REPORT, lastReportPath);
    }

    @Test public void silentUploadFailureFailsBeforeQuery() throws Exception {
        dropReportUpload = true;
        assertTrue(expectFailure().getMessage().contains("did not confirm both"));
        assertNull(lastReportPath);
    }

    @Test public void runPermissionIsCheckedBeforeConnectSucceeds() throws Exception {
        failOperation = "runReport"; faultStatus = 200;
        faultMessage = "User cannot run report & read data model";
        SQLException error = expectFailure();
        assertTrue(error.getMessage().contains("proxy validation failed"));
        assertTrue(error.getMessage().contains("User cannot run report & read data model"));
        assertEquals(2, uploads.size());
    }

    @Test public void emptyOutputFailsValidation() throws Exception {
        csv = "";
        assertTrue(expectFailure().getMessage().contains("expected result"));
    }

    @Test public void driverDoesNotAdvertiseSharedReportAsDefault() {
        assertNull(new FusionDriver().getPropertyInfo(null, null)[2].value);
    }

    @Test public void reportReferenceEscapesXmlCharacters() throws Exception {
        connect(null).close();
        byte[] patched = FusionCatalogService.patchReportDmPath(uploads.get(REPORT), "/~a&b/FusionQuery/v1/dm.xdm");
        String xml = unzip(patched).get("_report.xdo");
        assertTrue(xml.contains("a&amp;b"));
        SoapXml.parse(xml);
    }
}
