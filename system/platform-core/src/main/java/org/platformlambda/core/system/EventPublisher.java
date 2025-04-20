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
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.Kv;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simple publisher to send messages to an event stream
 * (compatible with FluxConsumer)
 */
public class EventPublisher {
    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private static final String TYPE = "type";
    private static final String DATA = "data";
    private static final String EXCEPTION = "exception";
    private static final String END_OF_STREAM = "eof";

    private final ObjectStreamIO stream;
    private final String outStream;
    private final long timer;
    private final long ttl;
    private final AtomicBoolean eof = new AtomicBoolean(false);
    private final AtomicBoolean expired = new AtomicBoolean(false);

    public EventPublisher(long ttl) {
        this.ttl = ttl;
        this.stream = new ObjectStreamIO((int) ttl / 1000);
        this.outStream = this.stream.getOutputStreamId();
        long expiry = this.stream.getExpirySeconds() * 1000L;
        this.timer = Platform.getInstance().getVertx().setTimer(expiry, t -> {
            expired.set(true);
            EventEnvelope error = new EventEnvelope().setException(new AppException(408, "Event stream expired"));
            try {
                EventEmitter.getInstance().send(stream.getOutputStreamId(), error.toBytes(), new Kv(TYPE, EXCEPTION));
            } catch (IOException ex) {
                // nothing we can do
            }
        });
    }

    public String getStreamId() {
        return stream.getInputStreamId();
    }

    public long getTimeToLive() {
        return ttl;
    }

    public void publish(Object data) {
        try {
            EventEmitter.getInstance().send(outStream, data, new Kv(TYPE, DATA));
        } catch (IOException e) {
            log.error("Unable to publish data to {} - {}",
                    Utility.getInstance().getSimpleRoute(outStream), e.getMessage());
        }
    }

    public void publish(byte[] payload, int start, int end) throws IOException {
        if (start > end) {
            publishException(new IOException("Invalid byte range. Actual: start/end=" + start + "/" + end));
        } else if (end > payload.length) {
            publishException(new IOException("end pointer must not be larger than payload buffer size"));
        } else {
            // always create a new byte array
            byte[] b = start == end ? new byte[0] : Arrays.copyOfRange(payload, start, end);
            EventEmitter.getInstance().send(outStream, b, new Kv(TYPE, DATA));
        }
    }

    public void publishException(Throwable e) {
        try {
            var error = new EventEnvelope().setException(e);
            EventEmitter.getInstance().send(outStream, error.toBytes(), new Kv(TYPE, EXCEPTION));
        } catch (IOException ex) {
            log.error("Unable to publish exception to {} - {}",
                    Utility.getInstance().getSimpleRoute(outStream), ex.getMessage());
        }
        publishCompletion();
    }

    public void publishCompletion() {
        if (!eof.get()) {
            eof.set(true);
            try {
                EventEmitter.getInstance().send(outStream, new Kv(TYPE, END_OF_STREAM));
            } catch (IOException e) {
                log.error("Unable to publish completion signal to {} - {}",
                        Utility.getInstance().getSimpleRoute(outStream), e.getMessage());
            }
        }
        if (!expired.get()) {
            expired.set(true);
            Platform.getInstance().getVertx().cancelTimer(timer);
        }
    }
}
