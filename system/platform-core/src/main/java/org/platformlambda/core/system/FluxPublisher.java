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
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Publisher to process a Flux stream object for delivery of messages over an event stream
 *
 * @param <T> object type of messages for the event stream
 */
public class FluxPublisher<T> {
    private static final Logger log = LoggerFactory.getLogger(FluxPublisher.class);
    private static final String TYPE = "type";
    private static final String DATA = "data";
    private static final String EXCEPTION = "exception";
    private static final String END_OF_STREAM = "eof";
    private final Flux<T> flux;
    private final ObjectStreamIO stream;
    private final long timer;
    private final AtomicBoolean eof = new AtomicBoolean(false);
    private final AtomicBoolean expired = new AtomicBoolean(false);
    private Disposable disposable = null;
    private CustomSerializer serializer = null;

    /**
     * Create a publisher to process a Flux stream object
     *
     * @param flux object
     * @param ttl time-to-live
     */
    public FluxPublisher(Flux<T> flux, long ttl) {
        this.flux = flux;
        this.stream = new ObjectStreamIO((int) ttl / 1000);
        long expiry = this.stream.getExpirySeconds() * 1000L;
        this.timer = Platform.getInstance().getVertx().setTimer(expiry, t -> {
            expired.set(true);
            String outStream = stream.getOutputStreamId();
            var error = new EventEnvelope().setException(new AppException(408, "Event stream expired"));
            try {
                EventEmitter.getInstance().send(outStream, error.toBytes(), new Kv(TYPE, EXCEPTION));
            } catch (IllegalArgumentException e) {
                // ok to ignore
            }
            if (disposable != null && !disposable.isDisposed()) {
                Utility util = Utility.getInstance();
                log.warn("Flux publisher timeout after {} for {}",
                        util.elapsedTime(expiry), util.getSimpleRoute(stream.getInputStreamId()));
                disposable.dispose();
            }
        });
    }

    /**
     * Set custom serializer if necessary
     *
     * @param serializer for custom conversion
     */
    public void setCustomSerializer(CustomSerializer serializer) {
        this.serializer = serializer;
    }

    /**
     * Begin publishing of the given Flux object
     *
     * @return stream ID of the event stream
     */
    public String publish() {
        Utility util = Utility.getInstance();
        String outStream = stream.getOutputStreamId();
        disposable = flux.subscribeOn(Schedulers.fromExecutor(Platform.getInstance().getVirtualThreadExecutor()))
                .doFinally(signal -> {
                    if (!expired.get()) {
                        expired.set(true);
                        Platform.getInstance().getVertx().cancelTimer(timer);
                    }
                    if (!eof.get()) {
                        eof.set(true);
                        try {
                            EventEmitter.getInstance().send(outStream, new Kv(TYPE, END_OF_STREAM));
                        } catch (IllegalArgumentException e) {
                            log.error("Unable to publish completion signal to {} - {}",
                                    util.getSimpleRoute(outStream), e.getMessage());
                        }
                    }
                })
                .subscribe(data -> {
                    try {
                        Object payload = serializer == null? data : serializer.toMap(data);
                        EventEmitter.getInstance().send(outStream, payload, new Kv(TYPE, DATA));
                    } catch (IllegalArgumentException e) {
                        log.error("Unable to publish data to {} - {}", util.getSimpleRoute(outStream), e.getMessage());
                    }
                }, e -> {
                    try {
                        var error = new EventEnvelope().setException(e);
                        EventEmitter.getInstance().send(outStream, error.toBytes(), new Kv(TYPE, EXCEPTION));
                    } catch (IllegalArgumentException ex) {
                        log.error("Unable to publish exception to {} - {}",
                                util.getSimpleRoute(outStream), ex.getMessage());
                    }
                });
        return stream.getInputStreamId();
    }
}
