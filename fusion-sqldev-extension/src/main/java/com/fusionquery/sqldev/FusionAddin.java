package com.fusionquery.sqldev;

import oracle.dbtools.connections.db.DatabaseProvider;
import oracle.dbtools.raptor.navigator.AbstractThirdPartyAddin;

public class FusionAddin extends AbstractThirdPartyAddin {

    public static final String SUBTYPE = "fusionCloud";

    @Override
    public String getSubType() {
        return SUBTYPE;
    }

    @Override
    public void initialize() {
        super.initialize();
        DatabaseProvider.registerConnectionCreator(SUBTYPE, new FusionConnectionCreator());
    }
}
