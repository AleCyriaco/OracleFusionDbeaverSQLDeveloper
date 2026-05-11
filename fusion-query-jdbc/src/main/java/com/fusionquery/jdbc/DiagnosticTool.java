package com.fusionquery.jdbc;

import java.sql.*;
import java.util.Properties;

public class DiagnosticTool {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java -cp fusion-query-jdbc-1.0.0.jar com.fusionquery.jdbc.DiagnosticTool <host> <user> <password>");
            System.exit(1);
        }

        String host = args[0];
        String user = args[1];
        String pass = args[2];
        String url = "jdbc:fusion://" + host;

        System.out.println("=== Fusion Query JDBC Diagnostic ===");
        System.out.println("Host: " + host);
        System.out.println("User: " + user);
        System.out.println("URL:  " + url);
        System.out.println();

        // Step 1: Auto-deploy check
        System.out.println("[1/4] Checking proxy report deployment...");
        String baseUrl = "https://" + host;
        boolean isOcs = host.contains(".ocs.");
        System.out.println("  OCS instance: " + isOcs);

        FusionCatalogService catalog = new FusionCatalogService(baseUrl, user, pass, 120000);
        if (isOcs) {
            catalog.setUseSoap(true);
        }

        FusionCatalogService.DeployResult deployResult = catalog.ensureDeployed();
        System.out.println("  Found: " + deployResult.found);
        System.out.println("  Was installed now: " + deployResult.wasInstalled);
        System.out.println("  Report path: " + deployResult.reportPath);
        if (deployResult.error != null) {
            System.out.println("  ERROR: " + deployResult.error);
        }
        System.out.println();

        if (!deployResult.found) {
            System.out.println("FAILED: Proxy report not deployed. Fix the error above first.");
            System.exit(1);
        }

        // Step 2: Test SOAP directly
        System.out.println("[2/4] Testing SOAP report execution...");
        try {
            FusionSoapClient soap = new FusionSoapClient(baseUrl, user, pass, 120000);
            String testSql = "SELECT 1 AS OK FROM DUAL";
            String encoded = FusionQueryClient.encodeSql(
                    FusionQueryClient.wrapPaginated(testSql, 0, 10));
            byte[] result = soap.runReport(deployResult.reportPath, encoded);
            String csv = new String(result, java.nio.charset.StandardCharsets.UTF_8);
            System.out.println("  SOAP OK! Response: " + csv.trim());
        } catch (Exception e) {
            System.out.println("  SOAP FAILED: " + e.getMessage());
        }
        System.out.println();

        // Step 3: Test via FusionQueryClient
        System.out.println("[3/4] Testing via FusionQueryClient...");
        try {
            FusionQueryClient client = new FusionQueryClient(baseUrl, user, pass,
                    deployResult.reportPath, 120, true);
            if (isOcs) client.setUseSoap(true);

            QueryResult qr = client.query("SELECT SYSDATE AS CURRENT_DATE FROM DUAL");
            if (qr.hasError()) {
                System.out.println("  QUERY ERROR: " + qr.getError());
            } else {
                System.out.println("  Columns: " + qr.getColumns());
                System.out.println("  Rows: " + qr.getRows().size());
                if (!qr.getRows().isEmpty()) {
                    System.out.println("  First row: " + qr.getRows().get(0));
                }
            }
        } catch (Exception e) {
            System.out.println("  CLIENT FAILED: " + e.getMessage());
        }
        System.out.println();

        // Step 4: Test via JDBC
        System.out.println("[4/4] Testing via JDBC Connection...");
        try {
            Properties props = new Properties();
            props.setProperty("user", user);
            props.setProperty("password", pass);
            props.setProperty("reportPath", deployResult.reportPath);

            Connection conn = DriverManager.getConnection(url, props);
            System.out.println("  Connection OK!");
            System.out.println("  Warnings: " + (conn.getWarnings() != null ? conn.getWarnings().getMessage() : "none"));

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT SYSDATE AS CURRENT_DATE FROM DUAL");
            ResultSetMetaData meta = rs.getMetaData();
            System.out.println("  Columns: " + meta.getColumnCount());
            while (rs.next()) {
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    System.out.println("  " + meta.getColumnName(i) + " = " + rs.getString(i));
                }
            }
            rs.close();
            stmt.close();
            conn.close();
            System.out.println("  ALL OK!");
        } catch (Exception e) {
            System.out.println("  JDBC FAILED: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println();
        System.out.println("=== Diagnostic complete ===");
    }
}
