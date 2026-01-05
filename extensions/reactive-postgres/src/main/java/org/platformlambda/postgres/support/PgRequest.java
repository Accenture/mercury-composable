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
import org.platformlambda.postgres.models.PgUpdateStatement;
import org.platformlambda.postgres.services.PgService;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class PgRequest {
    private final long timeout;

    public PgRequest(long timeout) {
        this.timeout = timeout;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> query(PostOffice po, String sql, Object... parameters)
                                                    throws ExecutionException, InterruptedException {
        var req = new PgQueryStatement(sql);
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
        var result = po.request(new EventEnvelope().setTo(PgService.ROUTE).setBody(req), timeout).get();
        if (!result.hasError() && result.getBody() instanceof List) {
            return (List<Map<String, Object>>) result.getBody();
        }
        // in case of database exception
        var status = result.getStatus() == 200? 400 : result.getStatus();
        throw new AppException(status, String.valueOf(result.getBody()));
    }

    @SuppressWarnings("unchecked")
    public int update(PostOffice po, String sql, Object... parameters) throws ExecutionException, InterruptedException {
        var req = new PgUpdateStatement(sql);
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
        var result = po.request(new EventEnvelope().setTo(PgService.ROUTE).setBody(req), timeout).get();
        if (!result.hasError() && result.getBody() instanceof Map<?, ?> map && map.containsKey("row_updated")) {
            return (int) map.get("row_updated");
        }
        // in case of database exception
        var status = result.getStatus() == 200? 400 : result.getStatus();
        throw new AppException(status, String.valueOf(result.getBody()));
    }
}
