package org.platformlambda.services;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.FluxConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@PreLoad(route="hello.upload")
public class FileUploadDemo implements TypedLambdaFunction<AsyncHttpRequest, Object> {
    private static final Logger log = LoggerFactory.getLogger(FileUploadDemo.class);
            
    @Override
    public Object handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        if (input.isValidStreams()) {
            // Multipart content may contain one or more files
            final List<String> streams = input.getStreamRoutes();
            final List<String> fileNames = input.getFileNames();
            final List<String> contentTypes = input.getFileContentTypes();
            final List<Integer> fileSizes = input.getFileSizes();
            return Mono.create(emitter -> {
                final AtomicInteger received = new AtomicInteger(0);
                final AtomicInteger index = new AtomicInteger(0);
                for (String id: streams) {
                    final int i = index.getAndIncrement();
                    final ByteArrayOutputStream out = new ByteArrayOutputStream();
                    final AtomicInteger blocks = new AtomicInteger(0);
                    final FluxConsumer<byte[]> consumer = new FluxConsumer<>(id, 5000);
                    consumer.consume(data -> saveFileBlock(blocks, data, out, i+1, fileNames.get(i)),
                    emitter::error,
                    () -> {
                        int fileCount = received.incrementAndGet();
                        // here is one of the uploaded file and you can decide what to do with it
                        byte[] fileBytes = out.toByteArray();
                        // this is a demo so we just print out a log  message
                        log.info("File-{} {} contains {} bytes", fileCount, fileNames.get(i), fileBytes.length);
                        // all files received?
                        if (fileCount == streams.size()) {
                            emitter.success(responseBody(fileNames, fileSizes, contentTypes, input.getContentLength()));
                        }
                    });
                }
            });
        } else {
            throw new IllegalArgumentException("Input is not a multi-part file upload");
        }
    }

    private void saveFileBlock(AtomicInteger blocks, byte[] data, ByteArrayOutputStream out, int n, String fileName) {
        int blk = blocks.incrementAndGet();
        try {
            out.write(data);
            log.info("File-{}, {}, block-{}, {} bytes", n, fileName, blk, data.length);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private Map<String, Object> responseBody(List<String> fileNames, List<Integer> fileSizes,
                                             List<String> contentTypes, int total) {
        var response = new HashMap<String, Object>();
        response.put("file_name", fileNames);
        response.put("file_size", fileSizes);
        response.put("content_type", contentTypes);
        response.put("total", Map.of("files", fileNames.size(), "length", total));
        response.put("message", "Upload completed");
        return response;
    }
}
