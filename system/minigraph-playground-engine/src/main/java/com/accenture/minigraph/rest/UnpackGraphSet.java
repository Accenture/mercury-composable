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

package com.accenture.minigraph.rest;

import com.accenture.minigraph.common.GraphSet;
import org.platformlambda.core.annotations.OptionalService;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.FluxConsumer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * POST /api/graph-set/unpack - read a graph set (ADR-0027): the body is the package, a .pack file sent as
 * application/octet-stream, and the answer is {"manifest": {...}, "graphs": {"<graph-id>": <model>, ...}} -
 * the manifest as the package holds it, the packager's 'format' and 'format_version' included, and each
 * graph as a file holds it. The Playground's "Package graphs" panel inspects a dropped package with it and
 * imports one of its graphs as the draft through POST /api/graph/import/{id}. Bytes that are not a canonical
 * package, and a set that breaks a rule - an entry not named <graph-id>.json, a root name that differs from
 * its graph id, a 'graph_id' that names no graph - are refused with the reason.
 */
@OptionalService("app.env=dev")
@PreLoad(route = "unpack.graph.set", instances = 10)
public class UnpackGraphSet implements TypedLambdaFunction<AsyncHttpRequest, EventEnvelope> {
    private static final String NOT_A_SET = "Not a graph set - ";

    @Override
    public EventEnvelope handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance)
            throws InterruptedException {
        var bytes = packageBytes(input);
        if (bytes.length == 0) {
            throw new IllegalArgumentException(
                    "The request body is the graph set (.pack) to read, sent as application/octet-stream");
        }
        final GraphSet.Contents contents;
        try {
            contents = GraphSet.read(bytes);
        } catch (GraphSet.RefusedException | IOException e) {
            throw new IllegalArgumentException(NOT_A_SET + e.getMessage());
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("manifest", contents.manifest());
        result.put("graphs", contents.graphs());
        return new EventEnvelope().setHeader("Content-Type", "application/json").setBody(result);
    }

    private static byte[] packageBytes(AsyncHttpRequest input) throws InterruptedException {
        if (input.getBody() instanceof byte[] bytes) {
            return bytes;
        }
        if (input.getStreamRoute() != null) {
            return readStream(input);
        }
        return new byte[0];
    }

    /**
     * A body sent without a content length arrives as a stream; collect it, the route's timeout as the limit.
     */
    private static byte[] readStream(AsyncHttpRequest input) throws InterruptedException {
        long ttl = Math.max(1, input.getTimeoutSeconds()) * 1000L;
        var out = new ByteArrayOutputStream();
        var done = new CompletableFuture<byte[]>();
        new FluxConsumer<byte[]>(input.getStreamRoute(), ttl).consume(out::writeBytes,
                done::completeExceptionally, () -> done.complete(out.toByteArray()));
        try {
            return done.get(ttl, TimeUnit.MILLISECONDS);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalArgumentException("Unable to read the request body - " + e.getMessage());
        }
    }
}
