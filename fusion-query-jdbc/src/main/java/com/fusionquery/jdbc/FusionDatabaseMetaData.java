package com.fusionquery.jdbc;

import java.sql.*;
import java.util.*;

public class FusionDatabaseMetaData implements DatabaseMetaData {

    private final FusionConnection connection;

    public FusionDatabaseMetaData(FusionConnection connection) {
        this.connection = connection;
    }

    private ResultSet emptyResultSet(List<String> columns) {
        QueryResult empty = new QueryResult(columns, Collections.emptyList(), 0, 0, 0, false, null);
        try {
            return new FusionResultSet(
                    (FusionStatement) connection.createStatement(), empty, "");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private ResultSet queryResultSet(String sql) throws SQLException {
        Statement stmt = connection.createStatement();
        return stmt.executeQuery(sql);
    }

    // --- Driver / Database identification ---

    @Override public String getDriverName() { return "Fusion Query JDBC Driver"; }
    @Override public String getDriverVersion() { return "1.0.0"; }
    @Override public int getDriverMajorVersion() { return 1; }
    @Override public int getDriverMinorVersion() { return 0; }
    @Override public String getDatabaseProductName() { return "Oracle Fusion Cloud (via BI Publisher)"; }
    @Override public String getDatabaseProductVersion() { return "21c"; }
    @Override public int getDatabaseMajorVersion() { return 21; }
    @Override public int getDatabaseMinorVersion() { return 0; }
    @Override public int getJDBCMajorVersion() { return 4; }
    @Override public int getJDBCMinorVersion() { return 2; }
    @Override public String getURL() { return connection.getClient().getBaseUrl(); }
    @Override public String getUserName() { return connection.getClient().getUsername(); }

    // --- Capabilities ---

    @Override public boolean isReadOnly() { return true; }
    @Override public boolean supportsTransactions() { return false; }
    @Override public boolean supportsTransactionIsolationLevel(int level) { return false; }
    @Override public boolean supportsMultipleResultSets() { return false; }
    @Override public boolean supportsBatchUpdates() { return false; }
    @Override public boolean supportsSavepoints() { return false; }
    @Override public boolean supportsStoredProcedures() { return false; }
    @Override public boolean supportsUnion() { return true; }
    @Override public boolean supportsUnionAll() { return true; }
    @Override public boolean supportsGroupBy() { return true; }
    @Override public boolean supportsOuterJoins() { return true; }
    @Override public boolean supportsFullOuterJoins() { return true; }
    @Override public boolean supportsLimitedOuterJoins() { return true; }
    @Override public boolean supportsSubqueriesInComparisons() { return true; }
    @Override public boolean supportsSubqueriesInExists() { return true; }
    @Override public boolean supportsSubqueriesInIns() { return true; }
    @Override public boolean supportsSubqueriesInQuantifieds() { return true; }
    @Override public boolean supportsCorrelatedSubqueries() { return true; }
    @Override public boolean supportsOrderByUnrelated() { return true; }
    @Override public boolean supportsGroupByUnrelated() { return true; }
    @Override public boolean supportsGroupByBeyondSelect() { return true; }
    @Override public boolean supportsLikeEscapeClause() { return true; }
    @Override public boolean supportsExpressionsInOrderBy() { return true; }
    @Override public boolean supportsMinimumSQLGrammar() { return true; }
    @Override public boolean supportsCoreSQLGrammar() { return true; }
    @Override public boolean supportsExtendedSQLGrammar() { return false; }
    @Override public boolean supportsANSI92EntryLevelSQL() { return true; }
    @Override public boolean supportsANSI92IntermediateSQL() { return false; }
    @Override public boolean supportsANSI92FullSQL() { return false; }
    @Override public boolean supportsIntegrityEnhancementFacility() { return false; }
    @Override public boolean supportsMixedCaseIdentifiers() { return false; }
    @Override public boolean storesUpperCaseIdentifiers() { return true; }
    @Override public boolean storesLowerCaseIdentifiers() { return false; }
    @Override public boolean storesMixedCaseIdentifiers() { return false; }
    @Override public boolean supportsMixedCaseQuotedIdentifiers() { return true; }
    @Override public boolean storesUpperCaseQuotedIdentifiers() { return false; }
    @Override public boolean storesLowerCaseQuotedIdentifiers() { return false; }
    @Override public boolean storesMixedCaseQuotedIdentifiers() { return true; }
    @Override public boolean supportsAlterTableWithAddColumn() { return false; }
    @Override public boolean supportsAlterTableWithDropColumn() { return false; }
    @Override public boolean supportsColumnAliasing() { return true; }
    @Override public boolean nullPlusNonNullIsNull() { return true; }
    @Override public boolean supportsConvert() { return false; }
    @Override public boolean supportsConvert(int fromType, int toType) { return false; }
    @Override public boolean supportsTableCorrelationNames() { return true; }
    @Override public boolean supportsDifferentTableCorrelationNames() { return false; }
    @Override public boolean supportsMultipleOpenResults() { return false; }
    @Override public boolean supportsGetGeneratedKeys() { return false; }
    @Override public boolean supportsResultSetType(int type) { return type == ResultSet.TYPE_FORWARD_ONLY; }
    @Override public boolean supportsResultSetConcurrency(int type, int concurrency) { return concurrency == ResultSet.CONCUR_READ_ONLY; }
    @Override public boolean ownUpdatesAreVisible(int type) { return false; }
    @Override public boolean ownDeletesAreVisible(int type) { return false; }
    @Override public boolean ownInsertsAreVisible(int type) { return false; }
    @Override public boolean othersUpdatesAreVisible(int type) { return false; }
    @Override public boolean othersDeletesAreVisible(int type) { return false; }
    @Override public boolean othersInsertsAreVisible(int type) { return false; }
    @Override public boolean updatesAreDetected(int type) { return false; }
    @Override public boolean deletesAreDetected(int type) { return false; }
    @Override public boolean insertsAreDetected(int type) { return false; }
    @Override public boolean supportsPositionedDelete() { return false; }
    @Override public boolean supportsPositionedUpdate() { return false; }
    @Override public boolean supportsSelectForUpdate() { return false; }
    @Override public boolean supportsOpenCursorsAcrossCommit() { return false; }
    @Override public boolean supportsOpenCursorsAcrossRollback() { return false; }
    @Override public boolean supportsOpenStatementsAcrossCommit() { return true; }
    @Override public boolean supportsOpenStatementsAcrossRollback() { return true; }
    @Override public boolean supportsDataDefinitionAndDataManipulationTransactions() { return false; }
    @Override public boolean supportsDataManipulationTransactionsOnly() { return false; }
    @Override public boolean dataDefinitionCausesTransactionCommit() { return false; }
    @Override public boolean dataDefinitionIgnoredInTransactions() { return true; }
    @Override public boolean supportsResultSetHoldability(int holdability) { return true; }
    @Override public boolean supportsStatementPooling() { return false; }
    @Override public boolean supportsStoredFunctionsUsingCallSyntax() { return false; }
    @Override public boolean autoCommitFailureClosesAllResultSets() { return false; }
    @Override public boolean generatedKeyAlwaysReturned() { return false; }
    @Override public boolean supportsNamedParameters() { return false; }
    @Override public boolean supportsMultipleTransactions() { return false; }
    @Override public boolean supportsNonNullableColumns() { return true; }
    @Override public boolean supportsCatalogsInDataManipulation() { return false; }
    @Override public boolean supportsCatalogsInProcedureCalls() { return false; }
    @Override public boolean supportsCatalogsInTableDefinitions() { return false; }
    @Override public boolean supportsCatalogsInIndexDefinitions() { return false; }
    @Override public boolean supportsCatalogsInPrivilegeDefinitions() { return false; }
    @Override public boolean supportsSchemasInDataManipulation() { return true; }
    @Override public boolean supportsSchemasInProcedureCalls() { return false; }
    @Override public boolean supportsSchemasInTableDefinitions() { return true; }
    @Override public boolean supportsSchemasInIndexDefinitions() { return false; }
    @Override public boolean supportsSchemasInPrivilegeDefinitions() { return false; }
    @Override public boolean allProceduresAreCallable() { return false; }
    @Override public boolean allTablesAreSelectable() { return true; }
    @Override public boolean nullsAreSortedHigh() { return true; }
    @Override public boolean nullsAreSortedLow() { return false; }
    @Override public boolean nullsAreSortedAtStart() { return false; }
    @Override public boolean nullsAreSortedAtEnd() { return false; }
    @Override public boolean usesLocalFiles() { return false; }
    @Override public boolean usesLocalFilePerTable() { return false; }
    @Override public boolean isCatalogAtStart() { return true; }
    @Override public boolean locatorsUpdateCopy() { return false; }

    // --- Limits ---

    @Override public int getMaxBinaryLiteralLength() { return 0; }
    @Override public int getMaxCharLiteralLength() { return 0; }
    @Override public int getMaxColumnNameLength() { return 128; }
    @Override public int getMaxColumnsInGroupBy() { return 0; }
    @Override public int getMaxColumnsInIndex() { return 0; }
    @Override public int getMaxColumnsInOrderBy() { return 0; }
    @Override public int getMaxColumnsInSelect() { return 0; }
    @Override public int getMaxColumnsInTable() { return 0; }
    @Override public int getMaxConnections() { return 0; }
    @Override public int getMaxCursorNameLength() { return 0; }
    @Override public int getMaxIndexLength() { return 0; }
    @Override public int getMaxSchemaNameLength() { return 128; }
    @Override public int getMaxProcedureNameLength() { return 0; }
    @Override public int getMaxCatalogNameLength() { return 128; }
    @Override public int getMaxRowSize() { return 0; }
    @Override public boolean doesMaxRowSizeIncludeBlobs() { return false; }
    @Override public int getMaxStatementLength() { return 0; }
    @Override public int getMaxStatements() { return 0; }
    @Override public int getMaxTableNameLength() { return 128; }
    @Override public int getMaxTablesInSelect() { return 0; }
    @Override public int getMaxUserNameLength() { return 128; }
    @Override public int getDefaultTransactionIsolation() { return Connection.TRANSACTION_READ_COMMITTED; }
    @Override public int getResultSetHoldability() { return ResultSet.HOLD_CURSORS_OVER_COMMIT; }
    @Override public int getSQLStateType() { return sqlStateSQL; }

    // --- String info ---

    @Override public String getSQLKeywords() { return ""; }
    @Override public String getNumericFunctions() { return "ABS,CEIL,FLOOR,MOD,POWER,ROUND,TRUNC,SIGN,SQRT"; }
    @Override public String getStringFunctions() { return "CHR,CONCAT,INITCAP,LOWER,LPAD,LTRIM,REPLACE,RPAD,RTRIM,SUBSTR,UPPER,TRIM,LENGTH,INSTR"; }
    @Override public String getSystemFunctions() { return "SYSDATE,SYSTIMESTAMP,USER,UID"; }
    @Override public String getTimeDateFunctions() { return "ADD_MONTHS,LAST_DAY,MONTHS_BETWEEN,NEXT_DAY,SYSDATE,TRUNC"; }
    @Override public String getSearchStringEscape() { return "\\"; }
    @Override public String getExtraNameCharacters() { return "$#"; }
    @Override public String getIdentifierQuoteString() { return "\""; }
    @Override public String getCatalogSeparator() { return "."; }
    @Override public String getCatalogTerm() { return "database"; }
    @Override public String getSchemaTerm() { return "schema"; }
    @Override public String getProcedureTerm() { return "procedure"; }
    @Override public long getMaxLogicalLobSize() { return 0; }

    // --- Schema / table discovery (DBeaver uses these) ---

    @Override
    public ResultSet getSchemas() throws SQLException {
        return queryResultSet(
                "SELECT DISTINCT OWNER AS TABLE_SCHEM, 'FUSION' AS TABLE_CATALOG "
                        + "FROM ALL_TABLES ORDER BY OWNER");
    }

    @Override
    public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
        String sql = "SELECT DISTINCT OWNER AS TABLE_SCHEM, 'FUSION' AS TABLE_CATALOG "
                + "FROM ALL_TABLES";
        if (schemaPattern != null && !schemaPattern.equals("%")) {
            sql += " WHERE OWNER LIKE '" + schemaPattern.replace("'", "''") + "'";
        }
        sql += " ORDER BY OWNER";
        return queryResultSet(sql);
    }

    @Override
    public ResultSet getCatalogs() throws SQLException {
        List<String> cols = Arrays.asList("TABLE_CAT");
        List<Map<String, String>> rows = new ArrayList<>();
        Map<String, String> row = new LinkedHashMap<>();
        row.put("TABLE_CAT", "FUSION");
        rows.add(row);
        QueryResult result = new QueryResult(cols, rows, 0, 0, 0, false, null);
        return new FusionResultSet((FusionStatement) connection.createStatement(), result, "");
    }

    @Override
    public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern,
                               String[] types) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT OWNER AS TABLE_SCHEM, TABLE_NAME, 'TABLE' AS TABLE_TYPE, ")
                .append("'FUSION' AS TABLE_CAT, '' AS REMARKS, '' AS TYPE_CAT, '' AS TYPE_SCHEM, ")
                .append("'' AS TYPE_NAME, '' AS SELF_REFERENCING_COL_NAME, '' AS REF_GENERATION ")
                .append("FROM ALL_TABLES WHERE 1=1");

        if (schemaPattern != null && !schemaPattern.equals("%")) {
            sql.append(" AND OWNER LIKE '").append(schemaPattern.replace("'", "''")).append("'");
        }
        if (tableNamePattern != null && !tableNamePattern.equals("%")) {
            sql.append(" AND TABLE_NAME LIKE '").append(tableNamePattern.replace("'", "''")).append("'");
        }
        sql.append(" ORDER BY OWNER, TABLE_NAME");
        return queryResultSet(sql.toString());
    }

    @Override
    public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern,
                                String columnNamePattern) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT 'FUSION' AS TABLE_CAT, OWNER AS TABLE_SCHEM, TABLE_NAME, COLUMN_NAME, ")
                .append("CASE DATA_TYPE ")
                .append("  WHEN 'NUMBER' THEN 2 ")
                .append("  WHEN 'DATE' THEN 91 ")
                .append("  WHEN 'TIMESTAMP' THEN 93 ")
                .append("  WHEN 'CLOB' THEN 2005 ")
                .append("  WHEN 'BLOB' THEN 2004 ")
                .append("  ELSE 12 END AS DATA_TYPE, ")
                .append("DATA_TYPE AS TYPE_NAME, ")
                .append("NVL(DATA_LENGTH, 0) AS COLUMN_SIZE, ")
                .append("0 AS BUFFER_LENGTH, ")
                .append("NVL(DATA_SCALE, 0) AS DECIMAL_DIGITS, ")
                .append("10 AS NUM_PREC_RADIX, ")
                .append("CASE NULLABLE WHEN 'Y' THEN 1 ELSE 0 END AS NULLABLE, ")
                .append("'' AS REMARKS, ")
                .append("DATA_DEFAULT AS COLUMN_DEF, ")
                .append("0 AS SQL_DATA_TYPE, 0 AS SQL_DATETIME_SUB, ")
                .append("NVL(DATA_LENGTH, 0) AS CHAR_OCTET_LENGTH, ")
                .append("COLUMN_ID AS ORDINAL_POSITION, ")
                .append("CASE NULLABLE WHEN 'Y' THEN 'YES' ELSE 'NO' END AS IS_NULLABLE, ")
                .append("'' AS SCOPE_CATALOG, '' AS SCOPE_SCHEMA, '' AS SCOPE_TABLE, ")
                .append("0 AS SOURCE_DATA_TYPE, 'NO' AS IS_AUTOINCREMENT, 'NO' AS IS_GENERATEDCOLUMN ")
                .append("FROM ALL_TAB_COLUMNS WHERE 1=1");

        if (schemaPattern != null && !schemaPattern.equals("%")) {
            sql.append(" AND OWNER LIKE '").append(schemaPattern.replace("'", "''")).append("'");
        }
        if (tableNamePattern != null && !tableNamePattern.equals("%")) {
            sql.append(" AND TABLE_NAME LIKE '").append(tableNamePattern.replace("'", "''")).append("'");
        }
        if (columnNamePattern != null && !columnNamePattern.equals("%")) {
            sql.append(" AND COLUMN_NAME LIKE '").append(columnNamePattern.replace("'", "''")).append("'");
        }
        sql.append(" ORDER BY OWNER, TABLE_NAME, COLUMN_ID");
        return queryResultSet(sql.toString());
    }

    @Override
    public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT 'FUSION' AS TABLE_CAT, acc.OWNER AS TABLE_SCHEM, acc.TABLE_NAME, ")
                .append("acc.COLUMN_NAME, acc.POSITION AS KEY_SEQ, ac.CONSTRAINT_NAME AS PK_NAME ")
                .append("FROM ALL_CONS_COLUMNS acc ")
                .append("JOIN ALL_CONSTRAINTS ac ON acc.CONSTRAINT_NAME = ac.CONSTRAINT_NAME AND acc.OWNER = ac.OWNER ")
                .append("WHERE ac.CONSTRAINT_TYPE = 'P'");
        if (schema != null) sql.append(" AND acc.OWNER = '").append(schema.replace("'", "''")).append("'");
        if (table != null) sql.append(" AND acc.TABLE_NAME = '").append(table.replace("'", "''")).append("'");
        sql.append(" ORDER BY acc.POSITION");
        return queryResultSet(sql.toString());
    }

    @Override
    public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
        return emptyResultSet(Arrays.asList("PKTABLE_CAT", "PKTABLE_SCHEM", "PKTABLE_NAME",
                "PKCOLUMN_NAME", "FKTABLE_CAT", "FKTABLE_SCHEM", "FKTABLE_NAME", "FKCOLUMN_NAME",
                "KEY_SEQ", "UPDATE_RULE", "DELETE_RULE", "FK_NAME", "PK_NAME", "DEFERRABILITY"));
    }

    @Override
    public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
        return getImportedKeys(catalog, schema, table);
    }

    @Override
    public ResultSet getCrossReference(String parentCatalog, String parentSchema, String parentTable,
                                       String foreignCatalog, String foreignSchema, String foreignTable) throws SQLException {
        return getImportedKeys(null, null, null);
    }

    @Override
    public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate) throws SQLException {
        return emptyResultSet(Arrays.asList("TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "NON_UNIQUE",
                "INDEX_QUALIFIER", "INDEX_NAME", "TYPE", "ORDINAL_POSITION", "COLUMN_NAME",
                "ASC_OR_DESC", "CARDINALITY", "PAGES", "FILTER_CONDITION"));
    }

    @Override
    public ResultSet getTableTypes() throws SQLException {
        List<String> cols = Arrays.asList("TABLE_TYPE");
        List<Map<String, String>> rows = new ArrayList<>();
        for (String type : new String[]{"TABLE", "VIEW", "SYNONYM"}) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("TABLE_TYPE", type);
            rows.add(row);
        }
        QueryResult result = new QueryResult(cols, rows, 0, 0, 0, false, null);
        return new FusionResultSet((FusionStatement) connection.createStatement(), result, "");
    }

    @Override
    public ResultSet getTypeInfo() throws SQLException {
        return emptyResultSet(Arrays.asList("TYPE_NAME", "DATA_TYPE", "PRECISION", "LITERAL_PREFIX",
                "LITERAL_SUFFIX", "CREATE_PARAMS", "NULLABLE", "CASE_SENSITIVE", "SEARCHABLE",
                "UNSIGNED_ATTRIBUTE", "FIXED_PREC_SCALE", "AUTO_INCREMENT", "LOCAL_TYPE_NAME",
                "MINIMUM_SCALE", "MAXIMUM_SCALE", "SQL_DATA_TYPE", "SQL_DATETIME_SUB", "NUM_PREC_RADIX"));
    }

    @Override
    public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern) throws SQLException {
        return emptyResultSet(Arrays.asList("PROCEDURE_CAT", "PROCEDURE_SCHEM", "PROCEDURE_NAME",
                "REMARKS", "PROCEDURE_TYPE", "SPECIFIC_NAME"));
    }

    @Override
    public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern) throws SQLException {
        return emptyResultSet(Arrays.asList("PROCEDURE_CAT", "PROCEDURE_SCHEM", "PROCEDURE_NAME",
                "COLUMN_NAME", "COLUMN_TYPE", "DATA_TYPE", "TYPE_NAME"));
    }

    @Override
    public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
        return emptyResultSet(Arrays.asList("TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME",
                "GRANTOR", "GRANTEE", "PRIVILEGE", "IS_GRANTABLE"));
    }

    @Override
    public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) throws SQLException {
        return emptyResultSet(Arrays.asList("TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME",
                "COLUMN_NAME", "GRANTOR", "GRANTEE", "PRIVILEGE", "IS_GRANTABLE"));
    }

    @Override
    public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable) throws SQLException {
        return emptyResultSet(Arrays.asList("SCOPE", "COLUMN_NAME", "DATA_TYPE", "TYPE_NAME",
                "COLUMN_SIZE", "BUFFER_LENGTH", "DECIMAL_DIGITS", "PSEUDO_COLUMN"));
    }

    @Override
    public ResultSet getVersionColumns(String catalog, String schema, String table) throws SQLException {
        return emptyResultSet(Arrays.asList("SCOPE", "COLUMN_NAME", "DATA_TYPE", "TYPE_NAME",
                "COLUMN_SIZE", "BUFFER_LENGTH", "DECIMAL_DIGITS", "PSEUDO_COLUMN"));
    }

    @Override
    public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types) throws SQLException {
        return emptyResultSet(Arrays.asList("TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME",
                "CLASS_NAME", "DATA_TYPE", "REMARKS", "BASE_TYPE"));
    }

    @Override
    public ResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern) throws SQLException {
        return emptyResultSet(Arrays.asList("TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME",
                "SUPERTYPE_CAT", "SUPERTYPE_SCHEM", "SUPERTYPE_NAME"));
    }

    @Override
    public ResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
        return emptyResultSet(Arrays.asList("TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "SUPERTABLE_NAME"));
    }

    @Override
    public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern, String attributeNamePattern) throws SQLException {
        return emptyResultSet(Arrays.asList("TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "ATTR_NAME",
                "DATA_TYPE", "ATTR_TYPE_NAME", "ATTR_SIZE"));
    }

    @Override
    public ResultSet getClientInfoProperties() throws SQLException {
        return emptyResultSet(Arrays.asList("NAME", "MAX_LEN", "DEFAULT_VALUE", "DESCRIPTION"));
    }

    @Override
    public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern) throws SQLException {
        return emptyResultSet(Arrays.asList("FUNCTION_CAT", "FUNCTION_SCHEM", "FUNCTION_NAME",
                "REMARKS", "FUNCTION_TYPE", "SPECIFIC_NAME"));
    }

    @Override
    public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern) throws SQLException {
        return emptyResultSet(Arrays.asList("FUNCTION_CAT", "FUNCTION_SCHEM", "FUNCTION_NAME",
                "COLUMN_NAME", "COLUMN_TYPE", "DATA_TYPE", "TYPE_NAME"));
    }

    @Override
    public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
        return emptyResultSet(Arrays.asList("TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME",
                "COLUMN_NAME", "DATA_TYPE", "COLUMN_SIZE", "DECIMAL_DIGITS", "NUM_PREC_RADIX",
                "COLUMN_USAGE", "REMARKS", "CHAR_OCTET_LENGTH", "IS_NULLABLE"));
    }

    @Override
    public boolean supportsRefCursors() { return false; }

    @Override
    public boolean supportsSharding() { return false; }

    @Override public Connection getConnection() { return connection; }

    @Override public boolean isWrapperFor(Class<?> iface) { return false; }
    @Override public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLException("Not a wrapper"); }

    @Override
    public RowIdLifetime getRowIdLifetime() { return RowIdLifetime.ROWID_UNSUPPORTED; }
}
