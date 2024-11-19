package org.platformlambda.core.mock;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.PostOffice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.Map;

@PreLoad(route = "v1.reactive.flux.function")
public class FluxUserFunction implements TypedLambdaFunction<Map<String, Object>, Flux<Map<String, Object>>> {
    private static final Logger log = LoggerFactory.getLogger(FluxUserFunction.class);

    private static final String EXCEPTION = "exception";
    @Override
    public Flux<Map<String, Object>> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        PostOffice po = new PostOffice(headers, instance);
        po.annotateTrace("reactive", "test");
        log.info("GOT {} {}", headers, input);
        return Flux.create(emitter -> {
            if (headers.containsKey(EXCEPTION)) {
                emitter.error(new AppException(400, headers.get(EXCEPTION)));
            } else {
                // just generate two messages
                emitter.next(Map.of("first", "message"));
                emitter.next(input);
                emitter.complete();
            }
        });
    }
}
