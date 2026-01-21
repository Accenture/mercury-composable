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

package com.accenture.db2.support;

import com.accenture.db2.models.Db2QueryStatement;
import com.accenture.db2.models.Db2TransactionStatement;
import com.accenture.db2.models.Db2UpdateStatement;
import com.accenture.db2.services.Db2MockService;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.db.SqlPreparedStatement;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class Db2Request {
    private static final String MOCK_SERVICE = Db2MockService.ROUTE;
    private static final String ROW_UPDATED = "row_updated";
    private static final String UPDATED = "updated";
    private final long timeout;

    public Db2Request(long timeout) {
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
        var req = new Db2QueryStatement(sql);
        bindParameters(req, parameters);
        req.convertNamedParamsToIndex();
        var result = po.request(new EventEnvelope().setTo(MOCK_SERVICE).setBody(req), timeout).get();
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
        var req = new Db2UpdateStatement(sql);
        bindParameters(req, parameters);
        req.convertNamedParamsToIndex();
        var result = po.request(new EventEnvelope().setTo(MOCK_SERVICE).setBody(req), timeout).get();
        if (!result.hasError() && result.getBody() instanceof Map<?, ?> map &&
                map.get(ROW_UPDATED) instanceof Integer count) {
            return count;
        }
        // in case of database exception
        var status = result.getStatus() == 200? 400 : result.getStatus();
        throw new AppException(status, String.valueOf(result.getBody()));
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
        var statements = new Db2TransactionStatement();
        int n = 0;
        for (var sql : sqlList) {
            var req = new Db2UpdateStatement(sql);
            if (parameterList != null && parameterList.size() > n) {
                var param = parameterList.get(n);
                if (param != null) {
                    bindParameters(req, param.toArray());
                }
            }
            req.convertNamedParamsToIndex();
            statements.addStatement(req);
            n++;
        }
        var result = po.request(new EventEnvelope().setTo(MOCK_SERVICE).setBody(statements), timeout).get();
        if (!result.hasError() && result.getBody() instanceof Map<?, ?> map &&
                map.get(UPDATED) instanceof List<?> countList) {
            return (List<Integer>) countList;
        }
        // in case of database exception
        var status = result.getStatus() == 200? 400 : result.getStatus();
        throw new AppException(status, String.valueOf(result.getBody()));
    }

    @SuppressWarnings("unchecked")
    private void bindParameters(SqlPreparedStatement req, Object... parameters) {
        if (parameters.length == 1 && parameters[0] instanceof Map) {
            // named parameters
            var namedParams = (Map<String, Object>) parameters[0];
            for (var entry : namedParams.entrySet()) {
                req.bindParameter(entry.getKey(), entry.getValue());
            }
        } else {
            // positional parameters
            req.bindParameters(parameters);
        }
    }
}
