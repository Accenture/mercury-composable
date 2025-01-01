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

import org.platformlambda.core.annotations.EventInterceptor;
import org.platformlambda.core.annotations.ZeroTracing;
import org.platformlambda.core.models.*;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Foundation module for event Streaming to support the following classes
 * <p>
 *     FluxPublisher,
 *     FluxConsumer,
 *     EventPublisher
 */
public class ObjectStreamIO {
    private static final Logger log = LoggerFactory.getLogger(ObjectStreamIO.class);

    private static final ConcurrentMap<String, StreamInfo> streams = new ConcurrentHashMap<>();
    private static final AtomicInteger initCounter = new AtomicInteger(0);
    private static final AtomicBoolean housekeeperNotRunning = new AtomicBoolean(true);
    private static final long HOUSEKEEPING_INTERVAL = 30 * 1000L;    // 30 seconds
    private static final String TYPE = "type";
    private static final String READ = "read";
    private static final String CLOSE = "close";
    private static final String DATA = "data";
    private static final String EXCEPTION = "exception";
    private static final String END_OF_STREAM = "eof";
    private static final String STREAM_PREFIX = "stream.";
    private static final String IN = ".in";
    private static final String OUT = ".out";
    private String inputStreamId;
    private String outputStreamId;
    private String streamRoute;
    private final int expirySeconds;
    private final AtomicBoolean eof = new AtomicBoolean(false);
    private final ConcurrentLinkedQueue<String> callbacks = new ConcurrentLinkedQueue<>();

    /**
     * Create an object stream with given expiry timer in seconds
     *
     * @param expirySeconds - the expiry timer
     */
    public ObjectStreamIO(int expirySeconds) {
        this.expirySeconds = Math.max(1, expirySeconds);
        this.createStream();
    }

    /**
     * Get expiry timer
     *
     * @return expiry timer in seconds
     */
    public int getExpirySeconds() {
        return expirySeconds;
    }

    private void createStream() {
        Platform platform = Platform.getInstance();
        if (initCounter.incrementAndGet() == 1) {
            platform.getVertx().setPeriodic(HOUSEKEEPING_INTERVAL, t -> removeExpiredStreams());
            log.info("Housekeeper started");
        }
        if (initCounter.get() > 10000) {
            initCounter.set(10);
        }
        Utility util = Utility.getInstance();
        String id = util.getUuid();
        String in = STREAM_PREFIX+id+IN;
        String out = STREAM_PREFIX+id+OUT;
        this.inputStreamId = in + "@" + platform.getOrigin();
        this.outputStreamId = out + "@" + platform.getOrigin();
        StreamPublisher publisher = new StreamPublisher();
        StreamConsumer consumer = new StreamConsumer(publisher, in, out);
        try {
            platform.registerPrivate(in, consumer, 1);
            streams.put(in, new StreamInfo(expirySeconds));
            platform.registerPrivateStream(out, publisher);
            String timer = util.elapsedTime(expirySeconds * 1000L);
            log.info("Stream {} created, idle expiry {}", id, timer);
        } catch (IOException e) {
            // this should not happen
            log.error("Unable to create stream {} - {}", id, e.getMessage());
        }
    }

    /**
     * Get input stream ID
     *
     * @return stream ID
     */
    public String getInputStreamId() {
        return inputStreamId;
    }

    /**
     * Get output stream ID
     *
     * @return stream ID
     */
    public String getOutputStreamId() {
        return outputStreamId;
    }

    /**
     * Get number of running streams
     *
     * @return stream count
     */
    public static int getStreamCount() {
        return streams.size();
    }

    /**
     * Get information for all streams
     * (This function is reserved for unit test. Do not use it in production)
     *
     * @return stream info
     */
    public static Map<String, Object> getStreamInfo() {
        Utility util = Utility.getInstance();
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, StreamInfo> kv: streams.entrySet()) {
            StreamInfo info = kv.getValue();
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("created", util.date2str(new Date(info.created)));
            metadata.put("last_read", util.date2str(new Date(info.updated)));
            metadata.put("expiry_seconds", info.expiryMills / 1000);
            result.put(kv.getKey(), metadata);
        }
        result.put("count", streams.size());
        return result;
    }

    /**
     * Tell the system that the stream is active
     *
     * @param id of the stream
     */
    public static void touch(String id) {
        StreamInfo info = streams.get(id);
        if (info != null) {
            info.updated = System.currentTimeMillis();
        }
    }

    /**
     * Remote expired streams
     */
    public static void removeExpiredStreams() {
        if (housekeeperNotRunning.compareAndSet(true, false)) {
            Platform.getInstance().getVirtualThreadExecutor().submit(ObjectStreamIO::checkExpiredStreams);
        }
    }

    /**
     * Close expired streams if any
     */
    public static void checkExpiredStreams() {
        EventEmitter po = EventEmitter.getInstance();
        Utility util = Utility.getInstance();
        long now = System.currentTimeMillis();
        List<String> list = new ArrayList<>(streams.keySet());
        for (String id : list) {
            StreamInfo info = streams.get(id);
            // test null pointer to avoid racing condition
            if (info != null && now - info.updated > info.expiryMills) {
                try {
                    String createdTime = util.date2str(new Date(info.created));
                    String updatedTime = util.date2str(new Date(info.updated));
                    String idle = util.elapsedTime(info.expiryMills);
                    log.warn("{} expired. Inactivity for {} ({} - {})", id, idle, createdTime, updatedTime);
                    po.send(id, new Kv(TYPE, CLOSE));
                } catch (Exception e) {
                    log.error("Unable to remove expired {} - {}", id, e.getMessage());
                } finally {
                    streams.remove(id);
                }
            }
        }
    }

    @ZeroTracing
    private class StreamPublisher implements StreamFunction {

        @Override
        public void init(String manager) {
            streamRoute = manager;
        }

        @Override
        public String getManager() {
            return streamRoute;
        }

        @Override
        public void handleEvent(Map<String, String> headers, Object input) {
            String type = headers.get(TYPE);
            if (DATA.equals(type) || EXCEPTION.equals(type)) {
                if (!eof.get()) {
                    String cb = callbacks.poll();
                    if (cb != null) {
                        sendReply(cb, input, type);
                    }
                }
            } else if (END_OF_STREAM.equals(type) && !eof.get()) {
                eof.set(true);
                String cb = callbacks.poll();
                if (cb != null) {
                    sendReply(cb, input, END_OF_STREAM);
                }
            }
        }

        private void sendReply(String cb, Object input, String type) {
            try {
                EventEmitter po = EventEmitter.getInstance();
                if (cb.contains("|")) {
                    int sep = cb.indexOf('|');
                    String callback = cb.substring(0, sep);
                    String extra = cb.substring(sep + 1);
                    EventEnvelope eventExtra = new EventEnvelope();
                    eventExtra.setTo(callback).setExtra(extra).setHeader(TYPE, type).setBody(input);
                    po.send(eventExtra);
                } else {
                    po.send(cb, input, new Kv(TYPE, type));
                }
            } catch(IOException e) {
                log.error("Unable to callback - {}", e.getMessage());
            }
        }
    }

    @EventInterceptor
    @ZeroTracing
    private class StreamConsumer implements LambdaFunction {
        private final StreamPublisher publisher;
        private final String in;
        private final String out;

        public StreamConsumer(StreamPublisher publisher, String in, String out) {
            this.publisher = publisher;
            this.in = in;
            this.out = out;
        }

        @Override
        public Object handleEvent(Map<String, String> headers, Object input, int instance) throws Exception {
            Platform platform = Platform.getInstance();
            EventEmitter po = EventEmitter.getInstance();
            EventEnvelope event = (EventEnvelope) input;
            String type = event.getHeaders().get(TYPE);
            String cb = event.getReplyTo();
            String extra = event.getExtra();
            if (READ.equals(type) && cb != null) {
                if (extra != null) {
                    callbacks.add(cb + "|" + extra);
                } else {
                    callbacks.add(cb);
                }
                publisher.get();
                touch(in);
            }
            if (CLOSE.equals(type)) {
                platform.release(in);
                platform.release(out);
                streams.remove(in);
                if (cb != null) {
                    if (extra != null) {
                        EventEnvelope eventExtra = new EventEnvelope();
                        eventExtra.setTo(cb).setExtra(extra).setBody(true);
                        po.send(eventExtra);
                    } else {
                        po.send(cb, true);
                    }
                }
            }
            return null;
        }
    }

}
