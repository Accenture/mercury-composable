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

package com.accenture.minigraph.mock;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.serializers.MsgPack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Temp-file implementation of the suspend/resume state-store contract - the smallest
 * possible store, used by the engine's unit tests so the engine needs no external
 * store dependency in any scope.
 * <p>
 * Contract: headers type=put persists the request body (one file per cid, MsgPack
 * bytes, expiry stamped from the "ttl" seconds); headers type=get with body {cid}
 * returns the persisted map or an empty map when absent-or-expired, and CONSUMES the
 * record on read (delete-on-read - the file analog of Redis GETDEL, so a duplicate
 * resume cannot double-execute the continuation).
 */
@PreLoad(route = "v1.file.state.store", instances = 10)
public class FileStateStore implements TypedLambdaFunction<Map<String, Object>, Object> {
    private static final MsgPack msgPack = new MsgPack();
    private static final String STORE_DIR = "/tmp/suspend-resume";
    private static final String TYPE = "type";
    private static final String PUT = "put";
    private static final String GET = "get";
    private static final String CID = "cid";
    private static final String TTL = "ttl";
    private static final String DATA = "data";
    private static final String EXPIRES_AT = "expires_at";

    @Override
    public Object handleEvent(Map<String, String> headers, Map<String, Object> input, int instance)
                                throws IOException {
        var type = headers.get(TYPE);
        var dir = new File(STORE_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Unable to create " + STORE_DIR);
        }
        var cid = String.valueOf(input.get(CID));
        var file = new File(dir, safeFileName(cid));
        if (PUT.equals(type)) {
            var ttlSeconds = input.get(TTL) instanceof Number n? n.longValue() : 30;
            var wrapper = new HashMap<String, Object>();
            wrapper.put(EXPIRES_AT, System.currentTimeMillis() + ttlSeconds * 1000);
            wrapper.put(DATA, input);
            Files.write(file.toPath(), msgPack.pack(wrapper));
            return Map.of("stored", true);
        }
        if (GET.equals(type)) {
            if (!file.exists()) {
                return new HashMap<String, Object>();
            }
            var wrapper = (Map<?, ?>) msgPack.unpack(Files.readAllBytes(file.toPath()));
            Files.delete(file.toPath());
            var expiry = wrapper.get(EXPIRES_AT) instanceof Number n? n.longValue() : 0;
            if (System.currentTimeMillis() > expiry) {
                return new HashMap<String, Object>();
            }
            return wrapper.get(DATA);
        }
        throw new IllegalArgumentException("type must be put or get");
    }

    private String safeFileName(String cid) {
        var sb = new StringBuilder();
        for (var c : cid.toCharArray()) {
            sb.append(Character.isLetterOrDigit(c) || c == '-' || c == '_'? c : '_');
        }
        return sb.toString();
    }
}
