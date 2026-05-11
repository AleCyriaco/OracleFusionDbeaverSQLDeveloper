package com.fusionquery.jdbc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OracleErrorParser {

    private static final Pattern ORA_00904 = Pattern.compile(
            "ORA-00904:\\s*\"?([^\":\\n]+?)\"?\\.?\"?([^\":\\n]+?)\"?:\\s*invalid identifier",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern ORA_00904_SIMPLE = Pattern.compile(
            "ORA-00904:\\s*\"?([^\":\\n]+?)\"?:\\s*invalid identifier",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern ORA_00942 = Pattern.compile(
            "ORA-00942:[^\\n]*table or view does not exist",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern ORA_01722 = Pattern.compile(
            "ORA-01722:[^\\n]*invalid number",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern ORA_00907 = Pattern.compile(
            "ORA-00907:[^\\n]*missing right parenthesis",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern ORA_00933 = Pattern.compile(
            "ORA-00933:[^\\n]*SQL command not properly ended",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern ORA_00936 = Pattern.compile(
            "ORA-00936:[^\\n]*missing expression",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern ORA_01017 = Pattern.compile(
            "ORA-01017:[^\\n]*invalid username/password",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern TABLE_NAME_HINT = Pattern.compile(
            "FROM\\s+([\\w.]+)|JOIN\\s+([\\w.]+)",
            Pattern.CASE_INSENSITIVE);

    public static String parse(String rawError, String sql) {
        if (rawError == null) return "Unknown query error";

        // ORA-00904: table.column invalid identifier
        Matcher m904 = ORA_00904.matcher(rawError);
        if (m904.find()) {
            String table = m904.group(1).trim();
            String column = m904.group(2).trim();
            return "Column not found: \"" + column + "\" in table/alias \"" + table + "\"\n"
                    + "Check column names with: SELECT COLUMN_NAME FROM ALL_TAB_COLUMNS WHERE TABLE_NAME = '"
                    + table.toUpperCase() + "'";
        }

        // ORA-00904: simple (just column, no table qualifier)
        Matcher m904s = ORA_00904_SIMPLE.matcher(rawError);
        if (m904s.find()) {
            String identifier = m904s.group(1).trim();
            return "Invalid identifier: \"" + identifier + "\"\n"
                    + "The column or alias does not exist or is misspelled.";
        }

        // ORA-00942: table or view does not exist
        if (ORA_00942.matcher(rawError).find()) {
            String tableHint = extractTableFromSql(sql);
            String msg = "Table or view does not exist.";
            if (tableHint != null) {
                msg += " Check: \"" + tableHint + "\"\n"
                        + "Search available views with: SELECT VIEW_NAME FROM ALL_VIEWS WHERE VIEW_NAME LIKE '%"
                        + tableHint.toUpperCase() + "%'";
            }
            return msg;
        }

        // ORA-01722: invalid number
        if (ORA_01722.matcher(rawError).find()) {
            return "Invalid number (ORA-01722): a value could not be converted to a number.\n"
                    + "Check for type mismatches in your query columns or WHERE clause.";
        }

        // ORA-00907: missing right parenthesis
        if (ORA_00907.matcher(rawError).find()) {
            return "Syntax error: missing right parenthesis (ORA-00907).\n"
                    + "Check that all opening parentheses are properly closed.";
        }

        // ORA-00933: SQL command not properly ended
        if (ORA_00933.matcher(rawError).find()) {
            return "Syntax error: SQL command not properly ended (ORA-00933).\n"
                    + "Check for extra semicolons or invalid clauses at the end of your query.";
        }

        // ORA-00936: missing expression
        if (ORA_00936.matcher(rawError).find()) {
            return "Syntax error: missing expression (ORA-00936).\n"
                    + "Check for trailing commas or incomplete clauses in your query.";
        }

        // ORA-01017: invalid credentials
        if (ORA_01017.matcher(rawError).find()) {
            return "Authentication failed: invalid username or password (ORA-01017).";
        }

        // Fallback: extract just the ORA-XXXXX lines from the chain
        Pattern oraPattern = Pattern.compile("(ORA-\\d+:[^\\n]+)");
        Matcher oraMatcher = oraPattern.matcher(rawError);
        StringBuilder oraErrors = new StringBuilder();
        while (oraMatcher.find()) {
            String line = oraMatcher.group(1).trim();
            if (!oraErrors.toString().contains(line)) {
                if (oraErrors.length() > 0) oraErrors.append("\n");
                oraErrors.append(line);
            }
        }
        return oraErrors.length() > 0 ? oraErrors.toString() : rawError;
    }

    private static String extractTableFromSql(String sql) {
        if (sql == null) return null;
        Matcher m = TABLE_NAME_HINT.matcher(sql);
        if (m.find()) {
            String t = m.group(1) != null ? m.group(1) : m.group(2);
            if (t != null) return t.trim();
        }
        return null;
    }
}
