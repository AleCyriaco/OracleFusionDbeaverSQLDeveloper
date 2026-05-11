package com.fusionquery.sqldev;

import oracle.dbtools.connections.db.DatabaseProvider;
import oracle.dbtools.raptor.navigator.AbstractThirdPartyAddin;

import java.sql.Driver;
import java.sql.DriverManager;

public class FusionAddin extends AbstractThirdPartyAddin {

    public static final String SUBTYPE = "fusionCloud";
    private static final String DRIVER_CLASS = "com.fusionquery.jdbc.FusionDriver";

    @Override
    public String getSubType() {
        return SUBTYPE;
    }

    @Override
    public void initialize() {
        super.initialize();
        DatabaseProvider.registerConnectionCreator(SUBTYPE, new FusionConnectionCreator());
        registerDriver();
    }

    private static void registerDriver() {
        try {
            Class<?> cls = Class.forName(DRIVER_CLASS, true, FusionAddin.class.getClassLoader());
            DriverManager.registerDriver((Driver) cls.getDeclaredConstructor().newInstance());
        } catch (Throwable t) {
            // Driver also self-registers via its static initializer when first referenced;
            // failure here is non-fatal because the Third Party JDBC fallback still works.
        }
    }
}
