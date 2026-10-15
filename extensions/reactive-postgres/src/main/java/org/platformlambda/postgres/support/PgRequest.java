/*

    Copyright 2018-2026 Accenture Technology

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */

package org.platformlambda.postgres.support;

import org.platformlambda.postgres.models.PgQueryStatement;
import org.platformlambda.postgres.models.PgTransactionStatement;
import org.platformlambda.postgres.models.PgUpdateStatement;
import org.platformlambda.postgres.services.PgService;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

public class PgRequest {
    private static final String ROW_UPDATED = "row_updated";
    private static final String UPDATED = "updated";
    // PostgreSQL accepts at most 65535 bind parameters in one statement, but Spring's DatabaseClient
    // copies its binding set on every bind() call, so a statement's cost grows with the square of its
    // parameter count. Measured on the embedded server for 13108 rows of five columns: 22 s at the
    // PostgreSQL limit, 5 s at 4000, 4.1 s at 1000. One statement still carries hundreds of rows.
    private static final int MAX_PARAMETERS = 1000;
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_]\\w*");
    private static final Pattern TABLE_NAME = Pattern.compile("[A-Za-z_]\\w*(\\.[A-Za-z_]\\w*)?");
    private final long timeout;

    public PgRequest(long timeout) {
        this.timeout = timeout;
    }

    /**
     * Perform a SQL query
     *
     * @param po PostOffice
     * @param sql statement
     * @param parameters if any
     * @return list of records. Empty if not found.
     * @throws ExecutionException in case of error
     * @throws InterruptedException in case of error
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> query(PostOffice po, String sql, Object... parameters)
                                                    throws ExecutionException, InterruptedException {
        if (po == null) {
            throw new AppException(400, "Missing PostOffice");
        }
        if (sql == null) {
            throw new AppException(400, "Missing SQL statement");
        }
        var req = new PgQueryStatement(sql);
        req.bindParameters(parameters);
        var result = po.request(new EventEnvelope().setTo(PgService.ROUTE).setBody(req), timeout).get();
        if (!result.hasError() && result.getBody() instanceof List) {
            return (List<Map<String, Object>>) result.getBody();
        }
        // in case of database exception
        var status = result.getStatus() == 200? 400 : result.getStatus();
        throw new AppException(status, String.valueOf(result.getBody()));
    }

    /**
     * Perform an insert, update or delete
     *
     * @param po PostOffice
     * @param sql statement
     * @param parameters if any
     * @return number of rows updated
     * @throws ExecutionException in case of error
     * @throws InterruptedException in case of error
     */
    public int update(PostOffice po, String sql, Object... parameters) throws ExecutionException, InterruptedException {
        if (po == null) {
            throw new AppException(400, "Missing PostOffice");
        }
        if (sql == null) {
            throw new AppException(400, "Missing SQL statement");
        }
        var req = new PgUpdateStatement(sql);
        req.bindParameters(parameters);
        var result = po.request(new EventEnvelope().setTo(PgService.ROUTE).setBody(req), timeout).get();
        if (!result.hasError() && result.getBody() instanceof Map<?, ?> map &&
                map.get(ROW_UPDATED) instanceof Integer count) {
            return count;
        }
        // in case of database exception
        var status = result.getStatus() == 200? 400 : result.getStatus();
        throw new AppException(status, String.valueOf(result.getBody()));
    }

    /**
     * Insert many rows with ONE multi-row INSERT statement - one round trip for all the rows
     * instead of one per row. See insertRows(po, table, columns, rows, onConflict) for details.
     *
     * @param po PostOffice
     * @param table name, optionally schema-qualified (e.g. "public.health_check")
     * @param columns names in the order the row values are given
     * @param rows list of rows, each a list of values in column order (a null value is allowed)
     * @return number of rows inserted
     * @throws ExecutionException in case of error
     * @throws InterruptedException in case of error
     */
    public int insertRows(PostOffice po, String table, List<String> columns, List<List<Object>> rows)
            throws ExecutionException, InterruptedException {
        return insertRows(po, table, columns, rows, null);
    }

    /**
     * Insert many rows with ONE multi-row INSERT statement - one round trip for all the rows
     * instead of one per row.
     * <p>
     * The statement is "INSERT INTO table (columns) VALUES ($1, ...), ($n, ...) [onConflict]".
     * A statement carries at most 1000 bind parameters (200 rows of five columns), because the
     * database client's cost grows with the square of a statement's parameter count. A larger set
     * of rows is split into such statements and executed in one transaction - all or none.
     * <p>
     * Table and column names are validated as plain SQL identifiers because they are written into
     * the statement. The row values are bound as parameters. A list or a map is not a column value
     * and is refused, because the statement classes rewrite a list parameter into the SQL text for
     * "IN (...)" clauses.
     *
     * @param po PostOffice
     * @param table name, optionally schema-qualified (e.g. "public.health_check")
     * @param columns names in the order the row values are given
     * @param rows list of rows, each a list of values in column order (a null value is allowed)
     * @param onConflict optional clause appended to the statement, e.g.
     *                   "ON CONFLICT (id) DO UPDATE SET updated = EXCLUDED.updated" or
     *                   "ON CONFLICT DO NOTHING" (a skipped row is not counted); null for none
     * @return number of rows inserted
     * @throws ExecutionException in case of error
     * @throws InterruptedException in case of error
     */
    public int insertRows(PostOffice po, String table, List<String> columns, List<List<Object>> rows,
                          String onConflict) throws ExecutionException, InterruptedException {
        if (po == null) {
            throw new AppException(400, "Missing PostOffice");
        }
        validateRows(table, columns, rows);
        int rowsPerStatement = Math.max(1, MAX_PARAMETERS / columns.size());
        if (rows.size() <= rowsPerStatement) {
            return update(po, multiRowInsert(table, columns, rows.size(), onConflict), flatten(rows));
        }
        var sqlList = new ArrayList<String>();
        var parameterList = new ArrayList<List<Object>>();
        for (int start = 0; start < rows.size(); start += rowsPerStatement) {
            var chunk = rows.subList(start, Math.min(rows.size(), start + rowsPerStatement));
            sqlList.add(multiRowInsert(table, columns, chunk.size(), onConflict));
            parameterList.add(Arrays.asList(flatten(chunk)));
        }
        int total = 0;
        for (int count : transaction(po, sqlList, parameterList)) {
            total += count;
        }
        return total;
    }

    /**
     * Build a multi-row INSERT statement with positional parameters
     *
     * @param table name
     * @param columns names
     * @param rowCount number of value tuples
     * @param onConflict optional clause or null
     * @return SQL statement
     */
    public static String multiRowInsert(String table, List<String> columns, int rowCount, String onConflict) {
        var sb = new StringBuilder("INSERT INTO ").append(table)
                    .append(" (").append(String.join(", ", columns)).append(") VALUES ");
        int n = 1;
        for (int r = 0; r < rowCount; r++) {
            sb.append(r == 0 ? "(" : ", (");
            for (int c = 0; c < columns.size(); c++) {
                sb.append(c == 0 ? "$" : ", $").append(n);
                n++;
            }
            sb.append(')');
        }
        if (onConflict != null && !onConflict.isBlank()) {
            sb.append(' ').append(onConflict.trim());
        }
        return sb.toString();
    }

    private static void validateRows(String table, List<String> columns, List<List<Object>> rows) {
        if (table == null || !TABLE_NAME.matcher(table).matches()) {
            throw new AppException(400, "Invalid table name - " + table);
        }
        if (columns == null || columns.isEmpty()) {
            throw new AppException(400, "Missing column names");
        }
        for (var column : columns) {
            if (column == null || !IDENTIFIER.matcher(column).matches()) {
                throw new AppException(400, "Invalid column name - " + column);
            }
        }
        if (rows == null || rows.isEmpty()) {
            throw new AppException(400, "Missing rows");
        }
        int n = 0;
        for (var row : rows) {
            n++;
            validateRow(n, row, columns.size());
        }
    }

    private static void validateRow(int n, List<Object> row, int width) {
        if (row == null || row.size() != width) {
            throw new AppException(400, "Row " + n + " must have " + width + " values");
        }
        for (var value : row) {
            if (value instanceof List || value instanceof Map) {
                throw new AppException(400, "Row " + n + " contains a list or map - not a column value");
            }
        }
    }

    private static Object[] flatten(List<List<Object>> rows) {
        var values = new ArrayList<>();
        rows.forEach(values::addAll);
        return values.toArray();
    }

    /**
     * A convenient method to allow execution of the same SQL statement for multiple insert, update or delete
     *
     * @param po PostOffice
     * @param sql SQL statement for multiple execution
     * @param parameterList, optional list if parameters for each statement.
     *                       null element to indicate no parameters for the specific statement.
     * @return list of numbers of row updated for each SQL statement in the list
     * @throws ExecutionException in case of error
     * @throws InterruptedException in case of error
     */
    public List<Integer> batch(PostOffice po, String sql, List<List<Object>> parameterList)
            throws ExecutionException, InterruptedException {
        if (po == null) {
            throw new AppException(400, "Missing PostOffice");
        }
        if (sql == null) {
            throw new AppException(400, "Missing SQL statement");
        }
        var pList = parameterList == null? List.of() : parameterList;
        if (pList.isEmpty()) {
            throw new AppException(400, "Missing list of lists of parameters");
        }
        var sqlList = new ArrayList<String>();
        for (int i=0; i < pList.size(); i++) {
            sqlList.add(sql);
        }
        return transaction(po, sqlList, parameterList);
    }

    /**
     * Perform a transaction with one or more SQL statement of insert, update or delete
     *
     * @param po PostOffice
     * @param sqlList list of SQL statement
     * @param parameterList, optional list if parameters for each statement.
     *                       null element to indicate no parameters for the specific statement.
     * @return list of numbers of row updated for each SQL statement in the list
     * @throws ExecutionException in case of error
     * @throws InterruptedException in case of error
     */
    @SuppressWarnings("unchecked")
    public List<Integer> transaction(PostOffice po, List<String> sqlList, List<List<Object>> parameterList)
            throws ExecutionException, InterruptedException {
        if (sqlList == null || sqlList.isEmpty()) {
            throw new AppException(400, "Missing SQL statements");
        }
        var statements = new PgTransactionStatement();
        int n = 0;
        for (var sql : sqlList) {
            var req = new PgUpdateStatement(sql);
            if (parameterList != null && parameterList.size() > n) {
                var param = parameterList.get(n);
                if (param != null) {
                    req.bindParameters(param.toArray());
                }
            }
            statements.addStatement(req);
            n++;
        }
        var result = po.request(new EventEnvelope().setTo(PgService.ROUTE).setBody(statements), timeout).get();
        if (!result.hasError() && result.getBody() instanceof Map<?, ?> map &&
                map.get(UPDATED) instanceof List<?> countList) {
            return (List<Integer>) countList;
        }
        // in case of database exception
        var status = result.getStatus() == 200? 400 : result.getStatus();
        throw new AppException(status, String.valueOf(result.getBody()));
    }
}
