package org.platformlambda.core.system;

import org.junit.jupiter.api.Test;
import org.platformlambda.common.TestBase;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class WorkerHandlerTest extends TestBase {


    @Test
    void shouldReturn500OnNoClassDefError() throws InterruptedException, ExecutionException {
        long timeout = 5000;
        String demoFunction = "test.v1.error.noclassdef";

        TypedLambdaFunction<Object, Object> f = (headers, input, instance) -> {
            throw new NoClassDefFoundError();
        };

        Platform platform = Platform.getInstance();
        platform.registerPrivate(demoFunction, f, 1);

        PostOffice po = new PostOffice("unit.test", "10", "TEST /function/" + demoFunction);
        EventEnvelope res = po.request(new EventEnvelope().setTo(demoFunction)
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

        PostOffice po = new PostOffice("unit.test", "10", "TEST /function/" + demoFunction);
        EventEnvelope res = po.request(new EventEnvelope().setTo(demoFunction)
                        .setBody(Map.of()), timeout)
                .get();

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

        PostOffice po = new PostOffice("unit.test", "10", "TEST /function/" + demoFunction);
        EventEnvelope res = po.request(new EventEnvelope().setTo(demoFunction)
                        .setBody(Map.of()), timeout)
                .get();

        assertNotNull(res);
        assertEquals(500, res.getStatus());
        assertNotNull(res.getException());
        assertEquals(AssertionError.class, res.getException().getClass());
    }

}
