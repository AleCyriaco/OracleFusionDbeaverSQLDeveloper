package com.fusionquery.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

public class FusionPreparedStatement extends FusionStatement implements PreparedStatement {

    private final String sql;
    private final Map<Integer, Object> parameters = new LinkedHashMap<>();

    public FusionPreparedStatement(FusionConnection connection, String sql) {
        super(connection);
        this.sql = sql;
    }

    private String buildSql() {
        String resolved = sql;
        for (Map.Entry<Integer, Object> entry : parameters.entrySet()) {
            Object val = entry.getValue();
            String replacement;
            if (val == null) {
                replacement = "NULL";
            } else if (val instanceof Number) {
                replacement = val.toString();
            } else {
                replacement = "'" + val.toString().replace("'", "''") + "'";
            }
            resolved = resolved.replaceFirst("\\?", replacement);
        }
        return resolved;
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        return executeQuery(buildSql());
    }

    @Override
    public int executeUpdate() throws SQLException {
        throw new SQLException("Only SELECT statements are supported (read-only driver)");
    }

    @Override
    public boolean execute() throws SQLException {
        return execute(buildSql());
    }

    @Override
    public void clearParameters() { parameters.clear(); }

    @Override
    public void setNull(int parameterIndex, int sqlType) { parameters.put(parameterIndex, null); }

    @Override
    public void setBoolean(int i, boolean x) { parameters.put(i, x); }

    @Override
    public void setByte(int i, byte x) { parameters.put(i, x); }

    @Override
    public void setShort(int i, short x) { parameters.put(i, x); }

    @Override
    public void setInt(int i, int x) { parameters.put(i, x); }

    @Override
    public void setLong(int i, long x) { parameters.put(i, x); }

    @Override
    public void setFloat(int i, float x) { parameters.put(i, x); }

    @Override
    public void setDouble(int i, double x) { parameters.put(i, x); }

    @Override
    public void setBigDecimal(int i, BigDecimal x) { parameters.put(i, x); }

    @Override
    public void setString(int i, String x) { parameters.put(i, x); }

    @Override
    public void setDate(int i, Date x) { parameters.put(i, x); }

    @Override
    public void setTime(int i, Time x) { parameters.put(i, x); }

    @Override
    public void setTimestamp(int i, Timestamp x) { parameters.put(i, x); }

    @Override
    public void setDate(int i, Date x, Calendar cal) { parameters.put(i, x); }

    @Override
    public void setTime(int i, Time x, Calendar cal) { parameters.put(i, x); }

    @Override
    public void setTimestamp(int i, Timestamp x, Calendar cal) { parameters.put(i, x); }

    @Override
    public void setObject(int i, Object x) { parameters.put(i, x); }

    @Override
    public void setObject(int i, Object x, int targetSqlType) { parameters.put(i, x); }

    @Override
    public void setObject(int i, Object x, int targetSqlType, int scaleOrLength) { parameters.put(i, x); }

    @Override
    public void setBytes(int i, byte[] x) { parameters.put(i, x); }

    @Override
    public void setNull(int i, int sqlType, String typeName) { parameters.put(i, null); }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return currentResultSet != null ? currentResultSet.getMetaData() : null;
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void addBatch() throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void setAsciiStream(int i, InputStream x, int length) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void setUnicodeStream(int i, InputStream x, int length) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void setBinaryStream(int i, InputStream x, int length) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void setAsciiStream(int i, InputStream x, long length) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void setAsciiStream(int i, InputStream x) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void setBinaryStream(int i, InputStream x, long length) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void setBinaryStream(int i, InputStream x) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void setCharacterStream(int i, Reader reader, int length) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void setCharacterStream(int i, Reader reader, long length) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void setCharacterStream(int i, Reader reader) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void setNCharacterStream(int i, Reader reader, long length) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void setNCharacterStream(int i, Reader reader) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void setRef(int i, Ref x) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void setBlob(int i, Blob x) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void setBlob(int i, InputStream x, long length) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void setBlob(int i, InputStream x) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void setClob(int i, Clob x) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void setClob(int i, Reader reader, long length) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void setClob(int i, Reader reader) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void setNClob(int i, NClob x) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void setNClob(int i, Reader reader, long length) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void setNClob(int i, Reader reader) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void setArray(int i, Array x) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void setURL(int i, URL x) throws SQLException { parameters.put(i, x.toString()); }

    @Override
    public void setRowId(int i, RowId x) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void setNString(int i, String value) { parameters.put(i, value); }

    @Override
    public void setSQLXML(int i, SQLXML xmlObject) throws SQLException { throw new SQLFeatureNotSupportedException(); }
}
