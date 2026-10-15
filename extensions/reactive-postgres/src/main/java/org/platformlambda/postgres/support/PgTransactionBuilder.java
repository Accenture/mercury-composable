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

import org.platformlambda.core.exception.AppException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Steps for one database transaction - multi-row inserts into one or more tables and any insert, update
 * or delete statements - executed all or none by PgRequest.transaction(po, builder).
 * <p>
 * Each call adds one step. A multi-row insert larger than the per-statement parameter cap becomes several
 * statements but stays one step, so the result of the transaction has one row count per step, in order.
 * <pre>
 * var tx = new PgTransactionBuilder()
 *             .insertRows("demo_order", ORDER_COLUMNS, orders)
 *             .insertRows("demo_order_history", HISTORY_COLUMNS, events)
 *             .update("UPDATE demo_profile SET updated = $1 WHERE id = $2", now, profileId);
 * List&lt;Integer&gt; counts = sql.transaction(po, tx);
 * </pre>
 */
public class PgTransactionBuilder {
    private final List<String> statements = new ArrayList<>();
    private final List<List<Object>> parameters = new ArrayList<>();
    private final List<Integer> statementsPerStep = new ArrayList<>();

    /**
     * Add a multi-row INSERT of the given rows into a table
     *
     * @param table name, optionally schema-qualified
     * @param columns names in the order the row values are given
     * @param rows list of rows, each a list of values in column order (a null value is allowed)
     * @return this builder
     */
    public PgTransactionBuilder insertRows(String table, List<String> columns, List<List<Object>> rows) {
        return insertRows(table, columns, rows, null);
    }

    /**
     * Add a multi-row INSERT of the given rows into a table, with an optional ON CONFLICT clause
     * (see PgRequest.insertRows for the rules on identifiers and values)
     *
     * @param table name, optionally schema-qualified
     * @param columns names in the order the row values are given
     * @param rows list of rows, each a list of values in column order (a null value is allowed)
     * @param onConflict optional clause appended to the statement, e.g. "ON CONFLICT (id) DO NOTHING"; null for none
     * @return this builder
     */
    public PgTransactionBuilder insertRows(String table, List<String> columns, List<List<Object>> rows,
                                           String onConflict) {
        var bound = PgRequest.insertStatements(table, columns, rows, onConflict);
        for (var statement : bound) {
            statements.add(statement.sql());
            parameters.add(statement.parameters());
        }
        statementsPerStep.add(bound.size());
        return this;
    }

    /**
     * Add one insert, update or delete statement with its parameters - positional values for $1, $2, ...
     * or one Map for :name parameters, exactly as PgRequest.update takes them
     *
     * @param sql statement
     * @param values parameters, if any
     * @return this builder
     */
    public PgTransactionBuilder update(String sql, Object... values) {
        if (sql == null) {
            throw new AppException(400, "Missing SQL statement");
        }
        statements.add(sql);
        parameters.add(values == null ? List.of() : Arrays.asList(values));
        statementsPerStep.add(1);
        return this;
    }

    /**
     * Add the same insert, update or delete statement once per parameter list - one step whose count
     * is the total of its statements
     *
     * @param sql statement
     * @param parameterList one list of parameters per execution
     * @return this builder
     */
    public PgTransactionBuilder batch(String sql, List<List<Object>> parameterList) {
        if (sql == null) {
            throw new AppException(400, "Missing SQL statement");
        }
        if (parameterList == null || parameterList.isEmpty()) {
            throw new AppException(400, "Missing list of lists of parameters");
        }
        for (var values : parameterList) {
            statements.add(sql);
            parameters.add(values == null ? List.of() : values);
        }
        statementsPerStep.add(parameterList.size());
        return this;
    }

    /**
     * @return number of steps added
     */
    public int size() {
        return statementsPerStep.size();
    }

    /**
     * @return true when no step has been added
     */
    public boolean isEmpty() {
        return statementsPerStep.isEmpty();
    }

    List<String> getStatements() {
        return statements;
    }

    List<List<Object>> getParameters() {
        return parameters;
    }

    /**
     * Fold the per-statement counts of the executed transaction into one count per step
     *
     * @param counts one per statement, in order
     * @return one per step, in order
     */
    List<Integer> countPerStep(List<Integer> counts) {
        var result = new ArrayList<Integer>();
        int n = 0;
        for (int statementCount : statementsPerStep) {
            int total = 0;
            for (int i = 0; i < statementCount; i++) {
                total += counts.get(n);
                n++;
            }
            result.add(total);
        }
        return result;
    }
}
