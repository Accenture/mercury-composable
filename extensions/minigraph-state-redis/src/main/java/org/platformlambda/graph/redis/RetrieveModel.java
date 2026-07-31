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

package org.platformlambda.graph.redis;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.serializers.MsgPack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.platformlambda.graph.redis.RedisStateConnection.KEY_PREFIX;

/**
 * Redis implementation of the suspend/resume state-store RETRIEVE contract, invoked by
 * the graph.resume skill through the node's "task" property.
 * <p>
 * Contract: headers type=get; body {cid}. Returns the persisted record, or an empty map
 * when absent-or-expired (a fresh transaction is the normal case, not an error). The
 * record is CONSUMED atomically on retrieval, so a duplicate resume request cannot
 * execute the continuation twice - via native GETDEL on Redis 6.2+, or a MULTI/EXEC
 * GET+DEL transaction on older servers (the strategy is detected per connection, since
 * enterprise deployments rarely control their managed Redis version).
 */
@PreLoad(route = "v1.redis.retrieve.model", instances = 50,
         envInstances = "worker.instances.v1.redis.retrieve.model")
public class RetrieveModel implements TypedLambdaFunction<Map<String, Object>, Object> {
    private static final Logger log = LoggerFactory.getLogger(RetrieveModel.class);
    private static final MsgPack msgPack = new MsgPack();
    private static final String TYPE = "type";
    private static final String GET = "get";
    private static final String CID = "cid";

    @Override
    public Object handleEvent(Map<String, String> headers, Map<String, Object> input, int instance)
                                throws IOException {
        if (!GET.equals(headers.get(TYPE))) {
            throw new IllegalArgumentException("Type must be get");
        }
        var cid = input.get(CID) instanceof String value && !value.isBlank()? value : null;
        if (cid == null) {
            throw new IllegalArgumentException("Missing cid");
        }
        var data = RedisStateConnection.consume(KEY_PREFIX + cid);
        if (data == null) {
            return new HashMap<String, Object>();
        }
        log.info("Restored workflow state for cid {}", cid);
        return msgPack.unpack(data);
    }
}
