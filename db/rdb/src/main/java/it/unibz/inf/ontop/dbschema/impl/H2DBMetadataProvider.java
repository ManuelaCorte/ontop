package it.unibz.inf.ontop.dbschema.impl;

import it.unibz.inf.ontop.dbschema.QuotedID;
import it.unibz.inf.ontop.exception.MetadataExtractionException;
import it.unibz.inf.ontop.model.type.DBTypeFactory;

import java.sql.Connection;

public class H2DBMetadataProvider extends  DefaultDBMetadataProvider {

    private final QuotedID defaultSchema;

    H2DBMetadataProvider(Connection connection, DBTypeFactory dbTypeFactory) throws MetadataExtractionException {
        super(connection, dbTypeFactory);

        defaultSchema = retriveDefaultSchema("SELECT SCHEMA()");
        System.out.println("DEF " + defaultSchema);
    }

    @Override
    public QuotedID getDefaultSchema() {
        return defaultSchema;
    }
}
