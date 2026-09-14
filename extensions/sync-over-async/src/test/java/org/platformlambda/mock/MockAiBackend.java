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

package org.platformlambda.mock;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.redis.RedisConfig;
import org.platformlambda.sync.StreamResponder;
import org.platformlambda.sync.StreamSegment;
import org.platformlambda.sync.SyncRuntime;

import java.util.List;
import java.util.Map;

/**
 * Stand-in for a backend service talking point-to-point to an AI agent: on request (a bare event
 * carrying the rendezvous cid), it posts a fixed sequence of token segments plus the end-of-transmission
 * signal - sequentially, over one connection, which per design D7 is all the ordering contract there is.
 * It talks to Redis only, through the {@link StreamResponder} producer API - no coordinator, no broker.
 */
@PreLoad(route = MockAiBackend.ROUTE, instances = 10)
public class MockAiBackend implements TypedLambdaFunction<Map<String, Object>, Void> {

    public static final String ROUTE = "mock.ai.backend";
    public static final List<String> TOKENS = List.of("Streaming", " is", " composable", " by", " design");
    public static final String EOT_METADATA = "{\"tokens\":5}";

    // one responder per backend service, reused across requests (each instance owns a Redis connection).
    // Lazily built on first use: @PreLoad construction runs BEFORE @MainApplication bootstrap, so a
    // constructor-frozen configuration is the known trap (see preload-before-mainapp-lazy-config).
    // Plain field guarded by the class monitor: every write happens inside the synchronized ensure
    // method, and every reader calls it first on its own thread before touching the field.
    private static StreamResponder responder;

    private static synchronized void ensureResponder() {
        if (responder == null) {
            responder = new StreamResponder(RedisConfig.from(AppConfigReader.getInstance()));
        }
    }

    private static boolean post(String cid, String type, String body) {
        ensureResponder();
        return responder.post(cid, type, null, body);   // the chat stream posts unnamed segments only
    }

    /** Test-lifecycle hook: release the responder's Redis client at class teardown. */
    public static synchronized void closeResponder() {
        if (responder != null) {
            responder.close();
            responder = null;
        }
    }

    @Override
    public Void handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        String cid = headers.get(SyncRuntime.CID);
        for (String token : TOKENS) {
            if (!post(cid, StreamSegment.DATA, token)) {
                return null;   // orphan - the rendezvous is over, stop producing
            }
        }
        post(cid, StreamSegment.EOF, EOT_METADATA);
        return null;
    }
}
