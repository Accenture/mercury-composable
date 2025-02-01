package org.platformlambda.services;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.FluxConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@PreLoad(route="hello.upload")
public class FileUploadDemo implements TypedLambdaFunction<AsyncHttpRequest, Object> {
    private static final Logger log = LoggerFactory.getLogger(FileUploadDemo.class);
            
    @Override
    public Object handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        String streamId = input.getStreamRoute();
        String filename = input.getFileName();
        int size = input.getContentLength();
        if (streamId != null && filename != null) {
            return Mono.create(emitter -> {
                AtomicInteger length = new AtomicInteger(0);
                FluxConsumer<byte[]> consumer = new FluxConsumer<>(streamId, 5000);
                try {
                    consumer.consume(data -> {
                        var n = length.addAndGet(data.length);
                        log.info("Received {} bytes", n);
                    }, emitter::error, () -> {
                        var result = new HashMap<String, Object>();
                        result.put("filename", filename);
                        result.put("expected_size", size);
                        result.put("actual_size", length.get());
                        result.put("message", "Upload completed");
                        emitter.success(result);
                    });
                } catch (IOException e) {
                    emitter.error(e);
                }
            });
        }
        throw new IllegalArgumentException("Input is not a multi-part file upload");
    }
}
