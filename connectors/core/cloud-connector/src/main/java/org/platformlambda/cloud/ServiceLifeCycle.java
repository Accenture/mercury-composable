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

package org.platformlambda.cloud;

import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.PubSub;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This is reserved for system use.
 * DO NOT use this directly in your application code.
 */
public class ServiceLifeCycle extends Thread {
    private static final Logger log = LoggerFactory.getLogger(ServiceLifeCycle.class);

    public static final long INITIALIZE = -100;
    private static final String TYPE = "type";
    private static final String INIT_TAG = "init";
    private static final String TOKEN_TAG = "token";
    private static final String SEQUENCE = "seq";
    private static final long FIRST_POLL = 1500;
    private static final long INTERVAL = 3000;
    private final String topic;
    private final String token;
    private final int partition;
    
    /**
     * When offset is set to the special value INITIALIZE, initial load
     * will send an initialization token to the EventConsumer to make sure
     * the consumer is ready to read new events.
     *
     * @param topic that the consumer uses
     * @param partition for the topic
     * @param token of random value
     */
    public ServiceLifeCycle(String topic, int partition, String token) {
        this.topic = topic;
        this.partition = partition;
        this.token = token;
    }

    @Override
    public void run() {
        final Platform platform = Platform.getInstance();
        final EventEmitter po = EventEmitter.getInstance();
        final Utility util = Utility.getInstance();
        final PubSub ps = PubSub.getInstance();
        final String INIT_HANDLER = INIT_TAG + "." + (partition < 0? topic : topic + "." + partition);
        final List<String> task = new ArrayList<>();
        final AtomicBoolean done = new AtomicBoolean(false);
        LambdaFunction f = (headers, input, instance) -> {
            if (!done.get()) {
                if (INIT_TAG.equals(input)) {
                    int n = util.str2int(headers.get(SEQUENCE));
                    try {
                        Map<String, String> event = new HashMap<>();
                        event.put(TYPE, INIT_TAG);
                        event.put(TOKEN_TAG, token);
                        event.put(SEQUENCE, String.valueOf(n));
                        log.info("Contacting {}, partition {}, sequence {}", topic, partition, n);
                        ps.publish(topic, partition, event, INIT_TAG);
                        task.clear();
                        String handle = po.sendLater(new EventEnvelope().setTo(INIT_HANDLER).setBody(INIT_TAG)
                                .setHeader(SEQUENCE, n + 1), new Date(System.currentTimeMillis() + INTERVAL));
                        task.add(handle);
                    } catch (IOException e) {
                        log.error("Unable to send initToken to consumer - {}", e.getMessage());
                    }
                } else {
                    done.set(true);
                    if (!task.isEmpty()) {
                        po.cancelFutureEvent(task.getFirst());
                    }
                    log.info("{}, partition {} ready", topic, partition);
                    platform.getVertx().setTimer(1000, t -> platform.release(INIT_HANDLER));
                }
            }
            return true;
        };
        try {
            platform.registerPrivate(INIT_HANDLER, f, 1);
            po.sendLater(new EventEnvelope().setTo(INIT_HANDLER).setBody(INIT_TAG).setHeader(SEQUENCE, 1),
                    new Date(System.currentTimeMillis() + FIRST_POLL));
        } catch (IOException e) {
            log.error("Unable to register {} - {}", INIT_HANDLER, e.getMessage());
        }
    }
}
