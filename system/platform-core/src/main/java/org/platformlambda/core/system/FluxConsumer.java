/*

    Copyright 2018-2025 Accenture Technology

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

import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.CustomSerializer;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.Kv;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.Utility;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Consumer to process an incoming event stream using Flux style consumers
 *
 * @param <T> object type of messages for the event stream
 */
public class FluxConsumer<T> {
    private static final String TYPE = "type";
    private static final String DATA = "data";
    private static final String READ = "read";
    private static final String EXCEPTION = "exception";
    private static final String END_OF_STREAM = "eof";
    private static final String CLOSE = "close";
    private final String callback = "callback."+ Utility.getInstance().getUuid();
    private final AtomicBoolean eof = new AtomicBoolean(false);
    private final AtomicBoolean expired = new AtomicBoolean(false);
    private final AtomicBoolean consumed = new AtomicBoolean(false);
    private final String inStream;
    private final long ttl;

    public FluxConsumer(String inStream, long ttl) {
        this.inStream = inStream;
        this.ttl = ttl;
    }

    public String getStreamId() {
        return inStream;
    }

    /**
     * Consume the event stream when the payload is not a PoJo
     * (Support payload as Map and Java Primitive. Other types
     *  may result in casting exception)
     *
     * @param consumer for handling messages
     * @param errorConsumer for handling exception message
     * @param completeConsumer for handling stream completion
     * @throws IOException in case of routing error
     */
    public void consume(Consumer<T> consumer,
                        Consumer<Throwable> errorConsumer,
                        Runnable completeConsumer) throws IOException {
        consume(consumer, errorConsumer, completeConsumer, null, null);
    }

    /**
     * Consume the event stream when the payload can be mapped as PoJo
     * (Using standard serializer)
     *
     * @param consumer for handling messages
     * @param errorConsumer for handling exception message
     * @param completeConsumer for handling stream completion
     * @param pojoClass if type (T) is a PoJo. Otherwise, put in a null
     * @throws IOException in case of routing error
     */
    public void consume(Consumer<T> consumer,
                        Consumer<Throwable> errorConsumer,
                        Runnable completeConsumer, Class<T> pojoClass) throws IOException {
        consume(consumer, errorConsumer, completeConsumer, pojoClass, null);
    }

    /**
     * Consume the event stream when the payload can be mapped as PoJo
     * (Optional custom serializer)
     *
     * @param consumer for handling messages
     * @param errorConsumer for handling exception message
     * @param completeConsumer for handling stream completion
     * @param pojoClass if type (T) is a PoJo. Otherwise, put in a null
     * @param serializer custom serializer or null
     * @throws IOException in case of routing error
     */
    @SuppressWarnings("unchecked")
    public void consume(Consumer<T> consumer,
                        Consumer<Throwable> errorConsumer,
                        Runnable completeConsumer,
                        Class<T> pojoClass, CustomSerializer serializer) throws IOException {
        if (consumed.get()) {
            throw new IllegalArgumentException("Consumer already assigned");
        } else {
            consumed.set(true);
            var platform = Platform.getInstance();
            var po = EventEmitter.getInstance();
            var mapper = SimpleMapper.getInstance().getMapper();
            final long timer = Platform.getInstance().getVertx().setTimer(ttl, t -> {
                expired.set(true);
                if (!eof.get()) {
                    eof.set(true);
                    if (platform.hasRoute(callback)) {
                        try {
                            var error = new EventEnvelope().setException(new AppException(408, "Consumer expired"));
                            po.send(new EventEnvelope().setTo(callback)
                                    .setHeader(TYPE, EXCEPTION).setBody(error.toBytes()));
                        } catch (IOException e) {
                            // ok to ignore
                        }
                    }
                }
            });
            // adding routing suffix in case the publisher and consumer are in different containers
            final EventEnvelope fetch = new EventEnvelope().setTo(inStream).setHeader(TYPE, READ)
                                                            .setReplyTo(callback + "@" + platform.getOrigin());
            final LambdaFunction f = (headers, body, instance) -> {
                String type = headers.get(TYPE);
                if (END_OF_STREAM.equals(type)) {
                    eof.set(true);
                    po.send(inStream, new Kv(TYPE, CLOSE));
                    if (completeConsumer != null) {
                        completeConsumer.run();
                    }
                }
                if (DATA.equals(type) && body != null && consumer != null) {
                    if (body instanceof Map && pojoClass != null) {
                        if (serializer != null) {
                            consumer.accept(serializer.toPoJo(body, pojoClass));
                        } else {
                            consumer.accept(mapper.readValue(body, pojoClass));
                        }
                    } else {
                        consumer.accept((T) body);
                    }
                }
                if (EXCEPTION.equals(type) && body instanceof byte[] b) {
                    EventEnvelope result = new EventEnvelope(b);
                    Throwable ex = result.getException();
                    if (ex != null) {
                        if (errorConsumer != null) {
                            errorConsumer.accept(ex);
                        }
                        eof.set(true);
                        po.send(inStream, new Kv(TYPE, CLOSE));
                    }
                }
                if (eof.get()) {
                    if (!expired.get()) {
                        expired.set(true);
                        Platform.getInstance().getVertx().cancelTimer(timer);
                    }
                    if (platform.hasRoute(callback)) {
                        platform.release(callback);
                    }
                } else {
                    po.send(fetch);
                }
                return null;
            };
            platform.registerPrivate(callback, f, 1);
            po.send(fetch);
        }
    }
}
