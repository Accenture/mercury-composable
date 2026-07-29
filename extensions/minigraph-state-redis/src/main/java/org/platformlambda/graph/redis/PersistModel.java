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
import java.util.Map;

import static org.platformlambda.graph.redis.RedisStateConnection.KEY_PREFIX;

/**
 * Redis implementation of the suspend/resume state-store PERSIST contract, invoked by
 * the graph.suspend skill through the node's "task" property.
 * <p>
 * Contract: headers type=put; body {cid, node, ttl, model, seen, run}. The record is
 * stored opaquely (MsgPack bytes) under the business correlation ID with the requested
 * time-to-live (Redis SETEX - expiry is native, no sweeper needed). A 2xx reply is the
 * durability acknowledgement the suspend skill requires before the graph completes.
 */
@PreLoad(route = "v1.redis.persist.model", instances = 50,
         envInstances = "worker.instances.v1.redis.persist.model")
public class PersistModel implements TypedLambdaFunction<Map<String, Object>, Object> {
    private static final Logger log = LoggerFactory.getLogger(PersistModel.class);
    private static final MsgPack msgPack = new MsgPack();
    private static final String TYPE = "type";
    private static final String PUT = "put";
    private static final String CID = "cid";
    private static final String TTL = "ttl";

    @Override
    public Object handleEvent(Map<String, String> headers, Map<String, Object> input, int instance)
                                throws IOException {
        if (!PUT.equals(headers.get(TYPE))) {
            throw new IllegalArgumentException("Type must be put");
        }
        var cid = input.get(CID) instanceof String value && !value.isBlank()? value : null;
        if (cid == null) {
            throw new IllegalArgumentException("Missing cid");
        }
        var ttlSeconds = input.get(TTL) instanceof Number n? n.longValue() : 0;
        if (ttlSeconds < 1) {
            throw new IllegalArgumentException("Invalid ttl");
        }
        RedisStateConnection.commands().setex(KEY_PREFIX + cid, ttlSeconds, msgPack.pack(input));
        log.info("Persisted workflow state for cid {}, ttl={}s", cid, ttlSeconds);
        return Map.of("stored", true);
    }
}
