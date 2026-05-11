package com.fusionquery.sqldev;

import oracle.dbtools.connections.db.AbstractConnectionCreator;
import oracle.dbtools.connections.db.DatabaseProvider;

import java.sql.SQLException;
import java.util.*;

public class FusionConnectionCreator extends AbstractConnectionCreator {

    public static final String PROP_HOST = "hostname";
    public static final String PROP_REPORT_PATH = "reportPath";
    public static final String PROP_TIMEOUT = "timeout";
    static final String SUBTYPE = "fusionCloud";
    static final String DRIVER_CLASS = "com.fusionquery.jdbc.FusionDriver";

    @Override
    public String getConnectionURL(Properties props) throws SQLException {
        String host = getPropertyOrThrow(props, PROP_HOST);
        StringBuilder url = new StringBuilder("jdbc:fusion://").append(host.trim());

        List<String> params = new ArrayList<>();
        String reportPath = props.getProperty(PROP_REPORT_PATH, "").trim();
        if (!reportPath.isEmpty()) params.add("reportPath=" + urlEncode(reportPath));
        String timeout = props.getProperty(PROP_TIMEOUT, "").trim();
        if (!timeout.isEmpty()) params.add("timeout=" + timeout);

        String user = props.getProperty("user", "").trim();
        if (!user.isEmpty()) params.add("user=" + urlEncode(user));
        String password = props.getProperty("password", "").trim();
        if (!password.isEmpty()) params.add("password=" + urlEncode(password));

        if (!params.isEmpty()) url.append("?").append(String.join("&", params));
        return url.toString();
    }

    @Override
    public String getDriverClassName(Properties props) {
        return DRIVER_CLASS;
    }

    @Override
    public boolean shouldEncrypt(String key) {
        return isPassword(key);
    }

    @Override
    public boolean isPassword(String key) {
        return "password".equalsIgnoreCase(key);
    }

    @Override
    public Collection<String> listAllowedProperties() {
        return Arrays.asList(
            PROP_HOST, "user", "password",
            PROP_REPORT_PATH, PROP_TIMEOUT,
            "ConnectionName", "SubConnectionType",
            "subtype", "RaptorConnectionType", "driver",
            "ConnName", "SavePassword", "customUrl"
        );
    }

    @Override
    public Collection<String> listRequiredProperties() {
        return Arrays.asList(PROP_HOST, "user", "password");
    }

    private static String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }
}
