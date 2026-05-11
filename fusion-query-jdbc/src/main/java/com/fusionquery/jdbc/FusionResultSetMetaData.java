package com.fusionquery.jdbc;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

public class FusionResultSetMetaData implements ResultSetMetaData {

    private final List<String> columns;

    public FusionResultSetMetaData(List<String> columns) {
        this.columns = columns;
    }

    @Override
    public int getColumnCount() { return columns.size(); }

    @Override
    public String getColumnLabel(int column) throws SQLException {
        return getColumnName(column);
    }

    @Override
    public String getColumnName(int column) throws SQLException {
        checkColumn(column);
        return columns.get(column - 1);
    }

    @Override
    public int getColumnType(int column) { return Types.VARCHAR; }

    @Override
    public String getColumnTypeName(int column) { return "VARCHAR2"; }

    @Override
    public String getColumnClassName(int column) { return String.class.getName(); }

    @Override
    public int getColumnDisplaySize(int column) { return 4000; }

    @Override
    public int getPrecision(int column) { return 0; }

    @Override
    public int getScale(int column) { return 0; }

    @Override
    public int isNullable(int column) { return columnNullable; }

    @Override
    public boolean isSigned(int column) { return false; }

    @Override
    public boolean isCurrency(int column) { return false; }

    @Override
    public boolean isAutoIncrement(int column) { return false; }

    @Override
    public boolean isCaseSensitive(int column) { return false; }

    @Override
    public boolean isSearchable(int column) { return true; }

    @Override
    public boolean isReadOnly(int column) { return true; }

    @Override
    public boolean isWritable(int column) { return false; }

    @Override
    public boolean isDefinitelyWritable(int column) { return false; }

    @Override
    public String getSchemaName(int column) { return ""; }

    @Override
    public String getTableName(int column) { return ""; }

    @Override
    public String getCatalogName(int column) { return "FUSION"; }

    @Override
    public boolean isWrapperFor(Class<?> iface) { return false; }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLException("Not a wrapper");
    }

    private void checkColumn(int column) throws SQLException {
        if (column < 1 || column > columns.size()) {
            throw new SQLException("Invalid column index: " + column);
        }
    }
}
