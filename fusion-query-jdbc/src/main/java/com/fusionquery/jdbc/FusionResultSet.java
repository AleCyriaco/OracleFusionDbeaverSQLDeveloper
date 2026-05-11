package com.fusionquery.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.sql.Date;
import java.util.*;

public class FusionResultSet implements ResultSet {

    private final FusionStatement statement;
    private final String originalSql;
    private List<String> columns;
    private List<Map<String, String>> rows;
    private int cursor = -1;
    private boolean closed = false;
    private boolean wasNull = false;

    private QueryResult currentPage;
    private final FusionQueryClient client;

    public FusionResultSet(FusionStatement statement, QueryResult result, String sql) {
        this.statement = statement;
        this.originalSql = sql;
        this.currentPage = result;
        this.columns = result.getColumns();
        this.rows = new ArrayList<>(result.getRows());
        this.client = statement.connection.getClient();
    }

    @Override
    public boolean next() throws SQLException {
        checkClosed();
        cursor++;
        if (cursor < rows.size()) return true;

        if (currentPage.hasNext()) {
            int nextPage = currentPage.getPage() + 1;
            currentPage = client.query(originalSql, currentPage.getPageSize(), nextPage);
            if (currentPage.hasError()) {
                String friendly = OracleErrorParser.parse(currentPage.getError(), originalSql);
                throw new SQLException(friendly);
            }
            rows.addAll(currentPage.getRows());
            if (columns.isEmpty() && !currentPage.getColumns().isEmpty()) {
                columns = currentPage.getColumns();
            }
            return cursor < rows.size();
        }
        return false;
    }

    @Override
    public void close() throws SQLException { closed = true; }

    @Override
    public boolean isClosed() { return closed; }

    private String getRawValue(int columnIndex) throws SQLException {
        checkClosed();
        if (cursor < 0 || cursor >= rows.size()) throw new SQLException("Invalid cursor position");
        if (columnIndex < 1 || columnIndex > columns.size()) throw new SQLException("Invalid column index: " + columnIndex);
        String val = rows.get(cursor).get(columns.get(columnIndex - 1));
        wasNull = (val == null || val.isEmpty());
        return val;
    }

    private String getRawValue(String columnLabel) throws SQLException {
        checkClosed();
        if (cursor < 0 || cursor >= rows.size()) throw new SQLException("Invalid cursor position");
        String val = rows.get(cursor).get(columnLabel.toUpperCase());
        if (val == null) {
            val = rows.get(cursor).get(columnLabel);
        }
        wasNull = (val == null || val.isEmpty());
        return val;
    }

    @Override
    public String getString(int columnIndex) throws SQLException { return getRawValue(columnIndex); }

    @Override
    public String getString(String columnLabel) throws SQLException { return getRawValue(columnLabel); }

    @Override
    public boolean getBoolean(int i) throws SQLException {
        String v = getRawValue(i);
        return v != null && ("true".equalsIgnoreCase(v) || "1".equals(v) || "Y".equalsIgnoreCase(v));
    }

    @Override
    public boolean getBoolean(String col) throws SQLException {
        String v = getRawValue(col);
        return v != null && ("true".equalsIgnoreCase(v) || "1".equals(v) || "Y".equalsIgnoreCase(v));
    }

    @Override
    public byte getByte(int i) throws SQLException { String v = getRawValue(i); return v == null ? 0 : Byte.parseByte(v); }

    @Override
    public short getShort(int i) throws SQLException { String v = getRawValue(i); return v == null ? 0 : Short.parseShort(v); }

    @Override
    public int getInt(int i) throws SQLException { String v = getRawValue(i); return v == null || v.isEmpty() ? 0 : Integer.parseInt(v.trim()); }

    @Override
    public long getLong(int i) throws SQLException { String v = getRawValue(i); return v == null || v.isEmpty() ? 0 : Long.parseLong(v.trim()); }

    @Override
    public float getFloat(int i) throws SQLException { String v = getRawValue(i); return v == null || v.isEmpty() ? 0 : Float.parseFloat(v.trim()); }

    @Override
    public double getDouble(int i) throws SQLException { String v = getRawValue(i); return v == null || v.isEmpty() ? 0 : Double.parseDouble(v.trim()); }

    @Override
    public BigDecimal getBigDecimal(int i, int scale) throws SQLException { String v = getRawValue(i); return v == null || v.isEmpty() ? null : new BigDecimal(v.trim()); }

    @Override
    public BigDecimal getBigDecimal(int i) throws SQLException { String v = getRawValue(i); return v == null || v.isEmpty() ? null : new BigDecimal(v.trim()); }

    @Override
    public BigDecimal getBigDecimal(String col) throws SQLException { String v = getRawValue(col); return v == null || v.isEmpty() ? null : new BigDecimal(v.trim()); }

    @Override
    public BigDecimal getBigDecimal(String col, int scale) throws SQLException { return getBigDecimal(col); }

    @Override
    public byte getByte(String col) throws SQLException { String v = getRawValue(col); return v == null ? 0 : Byte.parseByte(v); }

    @Override
    public short getShort(String col) throws SQLException { String v = getRawValue(col); return v == null ? 0 : Short.parseShort(v); }

    @Override
    public int getInt(String col) throws SQLException { String v = getRawValue(col); return v == null || v.isEmpty() ? 0 : Integer.parseInt(v.trim()); }

    @Override
    public long getLong(String col) throws SQLException { String v = getRawValue(col); return v == null || v.isEmpty() ? 0 : Long.parseLong(v.trim()); }

    @Override
    public float getFloat(String col) throws SQLException { String v = getRawValue(col); return v == null || v.isEmpty() ? 0 : Float.parseFloat(v.trim()); }

    @Override
    public double getDouble(String col) throws SQLException { String v = getRawValue(col); return v == null || v.isEmpty() ? 0 : Double.parseDouble(v.trim()); }

    @Override
    public Date getDate(int i) throws SQLException {
        String v = getRawValue(i);
        return v == null || v.isEmpty() ? null : Date.valueOf(v.trim());
    }

    @Override
    public Date getDate(String col) throws SQLException {
        String v = getRawValue(col);
        return v == null || v.isEmpty() ? null : Date.valueOf(v.trim());
    }

    @Override
    public Date getDate(int i, Calendar cal) throws SQLException { return getDate(i); }

    @Override
    public Date getDate(String col, Calendar cal) throws SQLException { return getDate(col); }

    @Override
    public Time getTime(int i) throws SQLException {
        String v = getRawValue(i);
        return v == null || v.isEmpty() ? null : Time.valueOf(v.trim());
    }

    @Override
    public Time getTime(String col) throws SQLException {
        String v = getRawValue(col);
        return v == null || v.isEmpty() ? null : Time.valueOf(v.trim());
    }

    @Override
    public Time getTime(int i, Calendar cal) throws SQLException { return getTime(i); }

    @Override
    public Time getTime(String col, Calendar cal) throws SQLException { return getTime(col); }

    @Override
    public Timestamp getTimestamp(int i) throws SQLException {
        String v = getRawValue(i);
        return v == null || v.isEmpty() ? null : Timestamp.valueOf(v.trim());
    }

    @Override
    public Timestamp getTimestamp(String col) throws SQLException {
        String v = getRawValue(col);
        return v == null || v.isEmpty() ? null : Timestamp.valueOf(v.trim());
    }

    @Override
    public Timestamp getTimestamp(int i, Calendar cal) throws SQLException { return getTimestamp(i); }

    @Override
    public Timestamp getTimestamp(String col, Calendar cal) throws SQLException { return getTimestamp(col); }

    @Override
    public Object getObject(int i) throws SQLException { return getString(i); }

    @Override
    public Object getObject(String col) throws SQLException { return getString(col); }

    @Override
    public Object getObject(int i, Map<String, Class<?>> map) throws SQLException { return getString(i); }

    @Override
    public Object getObject(String col, Map<String, Class<?>> map) throws SQLException { return getString(col); }

    @Override
    public <T> T getObject(int i, Class<T> type) throws SQLException {
        return type.cast(getString(i));
    }

    @Override
    public <T> T getObject(String col, Class<T> type) throws SQLException {
        return type.cast(getString(col));
    }

    @Override
    public boolean wasNull() { return wasNull; }

    @Override
    public int findColumn(String columnLabel) throws SQLException {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).equalsIgnoreCase(columnLabel)) return i + 1;
        }
        throw new SQLException("Column not found: " + columnLabel);
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return new FusionResultSetMetaData(columns);
    }

    @Override
    public int getRow() { return cursor + 1; }

    @Override
    public int getType() { return ResultSet.TYPE_FORWARD_ONLY; }

    @Override
    public int getConcurrency() { return ResultSet.CONCUR_READ_ONLY; }

    @Override
    public int getHoldability() { return ResultSet.HOLD_CURSORS_OVER_COMMIT; }

    @Override
    public Statement getStatement() { return statement; }

    @Override
    public SQLWarning getWarnings() { return null; }

    @Override
    public void clearWarnings() {}

    @Override
    public boolean rowUpdated() { return false; }

    @Override
    public boolean rowInserted() { return false; }

    @Override
    public boolean rowDeleted() { return false; }

    @Override
    public String getCursorName() { return null; }

    @Override
    public void setFetchDirection(int direction) {}

    @Override
    public int getFetchDirection() { return ResultSet.FETCH_FORWARD; }

    @Override
    public void setFetchSize(int rows) {}

    @Override
    public int getFetchSize() { return 1000; }

    @Override
    public boolean isBeforeFirst() { return cursor == -1; }

    @Override
    public boolean isAfterLast() { return cursor >= rows.size(); }

    @Override
    public boolean isFirst() { return cursor == 0; }

    @Override
    public boolean isLast() { return cursor == rows.size() - 1 && !currentPage.hasNext(); }

    // --- Navigation (forward-only, so most throw) ---

    @Override
    public void beforeFirst() throws SQLException { throw unsupported(); }

    @Override
    public void afterLast() throws SQLException { throw unsupported(); }

    @Override
    public boolean first() throws SQLException { throw unsupported(); }

    @Override
    public boolean last() throws SQLException { throw unsupported(); }

    @Override
    public boolean absolute(int row) throws SQLException { throw unsupported(); }

    @Override
    public boolean relative(int rows) throws SQLException { throw unsupported(); }

    @Override
    public boolean previous() throws SQLException { throw unsupported(); }

    // --- Unsupported update methods ---

    @Override
    public void updateNull(int i) throws SQLException { throw unsupported(); }
    @Override
    public void updateBoolean(int i, boolean x) throws SQLException { throw unsupported(); }
    @Override
    public void updateByte(int i, byte x) throws SQLException { throw unsupported(); }
    @Override
    public void updateShort(int i, short x) throws SQLException { throw unsupported(); }
    @Override
    public void updateInt(int i, int x) throws SQLException { throw unsupported(); }
    @Override
    public void updateLong(int i, long x) throws SQLException { throw unsupported(); }
    @Override
    public void updateFloat(int i, float x) throws SQLException { throw unsupported(); }
    @Override
    public void updateDouble(int i, double x) throws SQLException { throw unsupported(); }
    @Override
    public void updateBigDecimal(int i, BigDecimal x) throws SQLException { throw unsupported(); }
    @Override
    public void updateString(int i, String x) throws SQLException { throw unsupported(); }
    @Override
    public void updateBytes(int i, byte[] x) throws SQLException { throw unsupported(); }
    @Override
    public void updateDate(int i, Date x) throws SQLException { throw unsupported(); }
    @Override
    public void updateTime(int i, Time x) throws SQLException { throw unsupported(); }
    @Override
    public void updateTimestamp(int i, Timestamp x) throws SQLException { throw unsupported(); }
    @Override
    public void updateNull(String col) throws SQLException { throw unsupported(); }
    @Override
    public void updateBoolean(String col, boolean x) throws SQLException { throw unsupported(); }
    @Override
    public void updateByte(String col, byte x) throws SQLException { throw unsupported(); }
    @Override
    public void updateShort(String col, short x) throws SQLException { throw unsupported(); }
    @Override
    public void updateInt(String col, int x) throws SQLException { throw unsupported(); }
    @Override
    public void updateLong(String col, long x) throws SQLException { throw unsupported(); }
    @Override
    public void updateFloat(String col, float x) throws SQLException { throw unsupported(); }
    @Override
    public void updateDouble(String col, double x) throws SQLException { throw unsupported(); }
    @Override
    public void updateBigDecimal(String col, BigDecimal x) throws SQLException { throw unsupported(); }
    @Override
    public void updateString(String col, String x) throws SQLException { throw unsupported(); }
    @Override
    public void updateBytes(String col, byte[] x) throws SQLException { throw unsupported(); }
    @Override
    public void updateDate(String col, Date x) throws SQLException { throw unsupported(); }
    @Override
    public void updateTime(String col, Time x) throws SQLException { throw unsupported(); }
    @Override
    public void updateTimestamp(String col, Timestamp x) throws SQLException { throw unsupported(); }
    @Override
    public void updateObject(int i, Object x) throws SQLException { throw unsupported(); }
    @Override
    public void updateObject(int i, Object x, int scale) throws SQLException { throw unsupported(); }
    @Override
    public void updateObject(String col, Object x) throws SQLException { throw unsupported(); }
    @Override
    public void updateObject(String col, Object x, int scale) throws SQLException { throw unsupported(); }
    @Override
    public void insertRow() throws SQLException { throw unsupported(); }
    @Override
    public void updateRow() throws SQLException { throw unsupported(); }
    @Override
    public void deleteRow() throws SQLException { throw unsupported(); }
    @Override
    public void refreshRow() throws SQLException { throw unsupported(); }
    @Override
    public void cancelRowUpdates() throws SQLException { throw unsupported(); }
    @Override
    public void moveToInsertRow() throws SQLException { throw unsupported(); }
    @Override
    public void moveToCurrentRow() throws SQLException { throw unsupported(); }

    @Override
    public void updateAsciiStream(int i, InputStream x, int length) throws SQLException { throw unsupported(); }
    @Override
    public void updateBinaryStream(int i, InputStream x, int length) throws SQLException { throw unsupported(); }
    @Override
    public void updateCharacterStream(int i, Reader x, int length) throws SQLException { throw unsupported(); }
    @Override
    public void updateAsciiStream(String col, InputStream x, int length) throws SQLException { throw unsupported(); }
    @Override
    public void updateBinaryStream(String col, InputStream x, int length) throws SQLException { throw unsupported(); }
    @Override
    public void updateCharacterStream(String col, Reader x, int length) throws SQLException { throw unsupported(); }
    @Override
    public void updateAsciiStream(int i, InputStream x, long length) throws SQLException { throw unsupported(); }
    @Override
    public void updateAsciiStream(int i, InputStream x) throws SQLException { throw unsupported(); }
    @Override
    public void updateAsciiStream(String col, InputStream x, long length) throws SQLException { throw unsupported(); }
    @Override
    public void updateAsciiStream(String col, InputStream x) throws SQLException { throw unsupported(); }
    @Override
    public void updateBinaryStream(int i, InputStream x, long length) throws SQLException { throw unsupported(); }
    @Override
    public void updateBinaryStream(int i, InputStream x) throws SQLException { throw unsupported(); }
    @Override
    public void updateBinaryStream(String col, InputStream x, long length) throws SQLException { throw unsupported(); }
    @Override
    public void updateBinaryStream(String col, InputStream x) throws SQLException { throw unsupported(); }
    @Override
    public void updateCharacterStream(int i, Reader x, long length) throws SQLException { throw unsupported(); }
    @Override
    public void updateCharacterStream(int i, Reader x) throws SQLException { throw unsupported(); }
    @Override
    public void updateCharacterStream(String col, Reader x, long length) throws SQLException { throw unsupported(); }
    @Override
    public void updateCharacterStream(String col, Reader x) throws SQLException { throw unsupported(); }
    @Override
    public void updateNCharacterStream(int i, Reader x, long length) throws SQLException { throw unsupported(); }
    @Override
    public void updateNCharacterStream(int i, Reader x) throws SQLException { throw unsupported(); }
    @Override
    public void updateNCharacterStream(String col, Reader x, long length) throws SQLException { throw unsupported(); }
    @Override
    public void updateNCharacterStream(String col, Reader x) throws SQLException { throw unsupported(); }
    @Override
    public void updateRef(int i, Ref x) throws SQLException { throw unsupported(); }
    @Override
    public void updateRef(String col, Ref x) throws SQLException { throw unsupported(); }
    @Override
    public void updateBlob(int i, Blob x) throws SQLException { throw unsupported(); }
    @Override
    public void updateBlob(String col, Blob x) throws SQLException { throw unsupported(); }
    @Override
    public void updateBlob(int i, InputStream x, long length) throws SQLException { throw unsupported(); }
    @Override
    public void updateBlob(int i, InputStream x) throws SQLException { throw unsupported(); }
    @Override
    public void updateBlob(String col, InputStream x, long length) throws SQLException { throw unsupported(); }
    @Override
    public void updateBlob(String col, InputStream x) throws SQLException { throw unsupported(); }
    @Override
    public void updateClob(int i, Clob x) throws SQLException { throw unsupported(); }
    @Override
    public void updateClob(String col, Clob x) throws SQLException { throw unsupported(); }
    @Override
    public void updateClob(int i, Reader x, long length) throws SQLException { throw unsupported(); }
    @Override
    public void updateClob(int i, Reader x) throws SQLException { throw unsupported(); }
    @Override
    public void updateClob(String col, Reader x, long length) throws SQLException { throw unsupported(); }
    @Override
    public void updateClob(String col, Reader x) throws SQLException { throw unsupported(); }
    @Override
    public void updateNClob(int i, NClob x) throws SQLException { throw unsupported(); }
    @Override
    public void updateNClob(String col, NClob x) throws SQLException { throw unsupported(); }
    @Override
    public void updateNClob(int i, Reader x, long length) throws SQLException { throw unsupported(); }
    @Override
    public void updateNClob(int i, Reader x) throws SQLException { throw unsupported(); }
    @Override
    public void updateNClob(String col, Reader x, long length) throws SQLException { throw unsupported(); }
    @Override
    public void updateNClob(String col, Reader x) throws SQLException { throw unsupported(); }
    @Override
    public void updateSQLXML(int i, SQLXML x) throws SQLException { throw unsupported(); }
    @Override
    public void updateSQLXML(String col, SQLXML x) throws SQLException { throw unsupported(); }
    @Override
    public void updateNString(int i, String x) throws SQLException { throw unsupported(); }
    @Override
    public void updateNString(String col, String x) throws SQLException { throw unsupported(); }
    @Override
    public void updateArray(int i, Array x) throws SQLException { throw unsupported(); }
    @Override
    public void updateArray(String col, Array x) throws SQLException { throw unsupported(); }
    @Override
    public void updateRowId(int i, RowId x) throws SQLException { throw unsupported(); }
    @Override
    public void updateRowId(String col, RowId x) throws SQLException { throw unsupported(); }

    // --- Stream/LOB getters (return null) ---

    @Override
    public InputStream getAsciiStream(int i) throws SQLException { return null; }
    @Override
    public InputStream getAsciiStream(String col) throws SQLException { return null; }
    @Override
    public InputStream getUnicodeStream(int i) throws SQLException { return null; }
    @Override
    public InputStream getUnicodeStream(String col) throws SQLException { return null; }
    @Override
    public InputStream getBinaryStream(int i) throws SQLException { return null; }
    @Override
    public InputStream getBinaryStream(String col) throws SQLException { return null; }
    @Override
    public Reader getCharacterStream(int i) throws SQLException { return null; }
    @Override
    public Reader getCharacterStream(String col) throws SQLException { return null; }
    @Override
    public Reader getNCharacterStream(int i) throws SQLException { return null; }
    @Override
    public Reader getNCharacterStream(String col) throws SQLException { return null; }
    @Override
    public byte[] getBytes(int i) throws SQLException { return null; }
    @Override
    public byte[] getBytes(String col) throws SQLException { return null; }
    @Override
    public Ref getRef(int i) throws SQLException { return null; }
    @Override
    public Ref getRef(String col) throws SQLException { return null; }
    @Override
    public Blob getBlob(int i) throws SQLException { return null; }
    @Override
    public Blob getBlob(String col) throws SQLException { return null; }
    @Override
    public Clob getClob(int i) throws SQLException { return null; }
    @Override
    public Clob getClob(String col) throws SQLException { return null; }
    @Override
    public NClob getNClob(int i) throws SQLException { return null; }
    @Override
    public NClob getNClob(String col) throws SQLException { return null; }
    @Override
    public Array getArray(int i) throws SQLException { return null; }
    @Override
    public Array getArray(String col) throws SQLException { return null; }
    @Override
    public URL getURL(int i) throws SQLException { return null; }
    @Override
    public URL getURL(String col) throws SQLException { return null; }
    @Override
    public RowId getRowId(int i) throws SQLException { return null; }
    @Override
    public RowId getRowId(String col) throws SQLException { return null; }
    @Override
    public SQLXML getSQLXML(int i) throws SQLException { return null; }
    @Override
    public SQLXML getSQLXML(String col) throws SQLException { return null; }
    @Override
    public String getNString(int i) throws SQLException { return getString(i); }
    @Override
    public String getNString(String col) throws SQLException { return getString(col); }

    @Override
    public boolean isWrapperFor(Class<?> iface) { return false; }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException { throw unsupported(); }

    private void checkClosed() throws SQLException {
        if (closed) throw new SQLException("ResultSet is closed");
    }

    private static SQLException unsupported() {
        return new SQLFeatureNotSupportedException("Not supported by Fusion Query JDBC driver");
    }
}
