package com.fusionquery.jdbc;

import java.util.*;

public class QueryResult {

    private final List<String> columns;
    private final List<Map<String, String>> rows;
    private final int page;
    private final int pageSize;
    private final int offset;
    private final boolean hasNext;
    private final String error;

    public QueryResult(List<String> columns, List<Map<String, String>> rows,
                       int page, int pageSize, int offset, boolean hasNext, String error) {
        this.columns = columns != null ? columns : Collections.emptyList();
        this.rows = rows != null ? rows : Collections.emptyList();
        this.page = page;
        this.pageSize = pageSize;
        this.offset = offset;
        this.hasNext = hasNext;
        this.error = error;
    }

    public static QueryResult error(String message) {
        return new QueryResult(Collections.emptyList(), Collections.emptyList(),
                0, 0, 0, false, message);
    }

    public List<String> getColumns()            { return columns; }
    public List<Map<String, String>> getRows()   { return rows; }
    public int getPage()                         { return page; }
    public int getPageSize()                     { return pageSize; }
    public int getOffset()                       { return offset; }
    public boolean hasNext()                     { return hasNext; }
    public String getError()                     { return error; }
    public boolean hasError()                    { return error != null; }
}
