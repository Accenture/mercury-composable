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

package org.platformlambda.core.system;

import org.junit.jupiter.api.Test;
import org.platformlambda.common.TestBase;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.models.TypedLambdaFunction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WorkerHandlerTest extends TestBase {

    @Test
    void monoResponseForwardsSpanId() throws ExecutionException, InterruptedException {
        // A flow chains tasks by copying each task's response span_id into the next task's parent_span_id.
        // A Mono-returning task completes on a different thread than the worker, so its span_id must still be
        // forwarded on the response - regression test for the orphaned next-task span (e.g. sync.await).
        String route = "test.mono.span.forward";
        TypedLambdaFunction<Object, Mono<Object>> f =
                (headers, input, instance) -> Mono.just(Map.of("ok", true));
        Platform platform = Platform.getInstance();
        platform.registerPrivate(route, f, 1);
        PostOffice po = PostOffice.trackable("unit.test", "mono-span-1", "TEST /mono/span");
        EventEnvelope res = po.eRequest(new EventEnvelope().setTo(route).setBody(Map.of()), 5000).get();
        platform.release(route);
        assertNotNull(res, "expect a response");
        assertEquals(Map.of("ok", true), res.getBody());
        assertNotNull(res.getSpanId(),
                "a Mono task's response must carry the function's span_id so the next flow task can parent to it");
    }

    @Test
    void fluxResponseForwardsSpanId() throws ExecutionException, InterruptedException {
        // Unlike Mono, a Flux task returns its response (the x-stream-id/x-ttl handle) synchronously on the
        // worker thread, so the span_id is forwarded normally. This guards the Flux path against regressing
        // into the same cross-thread trace gap that affected Mono.
        String route = "test.flux.span.forward";
        TypedLambdaFunction<Object, Flux<Object>> f =
                (headers, input, instance) -> Flux.just("a", "b");
        Platform platform = Platform.getInstance();
        platform.registerPrivate(route, f, 1);
        PostOffice po = PostOffice.trackable("unit.test", "flux-span-1", "TEST /flux/span");
        EventEnvelope res = po.eRequest(new EventEnvelope().setTo(route).setBody(Map.of()), 5000).get();
        platform.release(route);
        assertNotNull(res, "expect a response");
        assertNotNull(res.getSpanId(), "a Flux task's response must carry the function's span_id");
    }

    @Test
    void canCatchNoClassDefException() throws ExecutionException, InterruptedException {
        var route = "no.class.def";
        LambdaFunction f = (headers, input, instance) -> {
            throw new NoClassDefFoundError("Missing library dependency");
        };
        var platform = Platform.getInstance();
        var po = PostOffice.trackable("unit.test", "404", "TEST /no/class/def");
        platform.registerPrivate(route, f, 1);
        var result = po.eRequest(new EventEnvelope().setTo(route).setBody("ok"), 5000).get();
        assertEquals(500, result.getStatus());
        assertEquals("Missing library dependency", result.getError());
        assertEquals("NoClassDefFoundError", result.getException().getClass().getSimpleName());
        platform.release(route);
    }

    @Test
    void shouldReturn500OnNoClassDefError() throws InterruptedException, ExecutionException {
        long timeout = 5000;
        String demoFunction = "test.v1.error.noclassdef";
        TypedLambdaFunction<Object, Object> f = (headers, input, instance) -> {
            throw new NoClassDefFoundError();
        };
        Platform platform = Platform.getInstance();
        platform.registerPrivate(demoFunction, f, 1);
        PostOffice po = PostOffice.trackable("unit.test", "10", "TEST /function/" + demoFunction);
        EventEnvelope res = po.eRequest(new EventEnvelope().setTo(demoFunction)
                .setBody(Map.of()), timeout)
                .get();
        assertNotNull(res);
        assertEquals(500, res.getStatus());
        assertNotNull(res.getException());
        assertEquals(NoClassDefFoundError.class, res.getException().getClass());
    }

    @Test
    void shouldReturn500OnAssertionError() throws InterruptedException, ExecutionException {
        long timeout = 5000;
        String demoFunction = "test.v1.error.assertion.error";
        TypedLambdaFunction<Object, Object> f = (headers, input, instance) -> {
            throw new AssertionError();
        };
        Platform platform = Platform.getInstance();
        platform.registerPrivate(demoFunction, f, 1);
        PostOffice po = PostOffice.trackable("unit.test", "10", "TEST /function/" + demoFunction);
        EventEnvelope res = po.eRequest(new EventEnvelope().setTo(demoFunction).setBody(Map.of()), timeout).get();
        assertNotNull(res);
        assertEquals(500, res.getStatus());
        assertNotNull(res.getException());
        assertEquals(AssertionError.class, res.getException().getClass());
    }

    @Test
    void shouldReturn500OnAssertion() throws InterruptedException, ExecutionException {
        long timeout = 5000;
        String demoFunction = "test.v1.error";
        TypedLambdaFunction<Object, Object> f = (headers, input, instance) -> {
            assert false;
            return true;
        };
        Platform platform = Platform.getInstance();
        platform.registerPrivate(demoFunction, f, 1);
        PostOffice po = PostOffice.trackable("unit.test", "10", "TEST /function/" + demoFunction);
        EventEnvelope res = po.eRequest(new EventEnvelope().setTo(demoFunction).setBody(Map.of()), timeout).get();
        assertNotNull(res);
        assertEquals(500, res.getStatus());
        assertNotNull(res.getException());
        assertEquals(AssertionError.class, res.getException().getClass());
    }
}
