package com.fusionquery.jdbc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Properties;

public class QueryTest {
    public static void main(String[] args) throws Exception {
        String host = args[0];
        String user = args[1];
        String pass = args[2];
        String sqlFile = args.length > 3 ? args[3] : null;

        String sql;
        if (sqlFile != null) {
            sql = Files.readString(Path.of(sqlFile)).trim();
        } else {
            sql = "SELECT SYSDATE FROM DUAL";
        }

        System.out.println("SQL (" + sql.length() + " chars):");
        System.out.println(sql.substring(0, Math.min(200, sql.length())) + "...");
        System.out.println();

        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", pass);

        String url = "jdbc:fusion://" + host;

        try (Connection conn = DriverManager.getConnection(url, props)) {
            System.out.println("Connected. Warnings: "
                    + (conn.getWarnings() != null ? conn.getWarnings().getMessage() : "none"));

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                System.out.println("Columns: " + cols);
                for (int i = 1; i <= cols; i++) {
                    System.out.print(meta.getColumnName(i) + "\t");
                }
                System.out.println();

                int count = 0;
                while (rs.next()) {
                    for (int i = 1; i <= cols; i++) {
                        System.out.print(rs.getString(i) + "\t");
                    }
                    System.out.println();
                    count++;
                    if (count >= 5) {
                        System.out.println("... (showing first 5 rows)");
                        break;
                    }
                }
                System.out.println("Total rows shown: " + count);
            }
        } catch (SQLException e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
