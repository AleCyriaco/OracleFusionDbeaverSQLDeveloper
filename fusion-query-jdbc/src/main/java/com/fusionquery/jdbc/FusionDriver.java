package com.fusionquery.jdbc;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * JDBC Driver for Oracle Fusion Cloud via BI Publisher.
 *
 * URL format: jdbc:fusion://host.fa.us2.oraclecloud.com
 * Properties: user, password, reportPath (optional), timeout (optional)
 */
public class FusionDriver implements Driver {

    public static final String URL_PREFIX = "jdbc:fusion://";
    private static final int MAJOR_VERSION = 1;
    private static final int MINOR_VERSION = 0;

    static {
        try {
            DriverManager.registerDriver(new FusionDriver());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to register FusionDriver", e);
        }
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) return null;
        if (info == null) info = new Properties();

        String host = url.substring(URL_PREFIX.length()).replaceAll("/+$", "");

        // Optional user:password@host syntax
        int at = host.indexOf('@');
        if (at >= 0) {
            String creds = host.substring(0, at);
            host = host.substring(at + 1);
            int colon = creds.indexOf(':');
            if (colon >= 0) {
                if (!info.containsKey("user"))     info.setProperty("user", urlDecode(creds.substring(0, colon)));
                if (!info.containsKey("password")) info.setProperty("password", urlDecode(creds.substring(colon + 1)));
            } else if (!info.containsKey("user")) {
                info.setProperty("user", urlDecode(creds));
            }
        }

        if (host.contains("?")) {
            String query = host.substring(host.indexOf('?') + 1);
            host = host.substring(0, host.indexOf('?'));
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2 && !info.containsKey(kv[0])) {
                    info.setProperty(kv[0], urlDecode(kv[1]));
                }
            }
        }

        String baseUrl = "https://" + host;
        String user = info.getProperty("user");
        String password = info.getProperty("password");
        String reportPath = info.getProperty("reportPath");
        int timeout = Integer.parseInt(info.getProperty("timeout", "120"));

        if (user == null || password == null) {
            throw new SQLException("Properties 'user' and 'password' are required");
        }

        FusionQueryClient client = new FusionQueryClient(baseUrl, user, password, reportPath, timeout, true);
        return new FusionConnection(client, baseUrl, info);
    }

    @Override
    public boolean acceptsURL(String url) {
        return url != null && url.startsWith(URL_PREFIX);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
        DriverPropertyInfo userProp = new DriverPropertyInfo("user", info.getProperty("user"));
        userProp.required = true;
        userProp.description = "Oracle Fusion Cloud username";

        DriverPropertyInfo passProp = new DriverPropertyInfo("password", null);
        passProp.required = true;
        passProp.description = "Oracle Fusion Cloud password";

        DriverPropertyInfo reportProp = new DriverPropertyInfo("reportPath",
                info.getProperty("reportPath", "/Custom/FusionQuery/Proxy/v1/csv.xdo"));
        reportProp.required = false;
        reportProp.description = "BI Publisher proxy report path";

        DriverPropertyInfo timeoutProp = new DriverPropertyInfo("timeout",
                info.getProperty("timeout", "120"));
        timeoutProp.required = false;
        timeoutProp.description = "HTTP timeout in seconds";

        return new DriverPropertyInfo[]{userProp, passProp, reportProp, timeoutProp};
    }

    @Override
    public int getMajorVersion() { return MAJOR_VERSION; }

    @Override
    public int getMinorVersion() { return MINOR_VERSION; }

    @Override
    public boolean jdbcCompliant() { return false; }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    private static String urlDecode(String s) {
        try { return java.net.URLDecoder.decode(s, "UTF-8"); }
        catch (Exception e) { return s; }
    }
}
