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

import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import org.platformlambda.core.util.ElasticQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

/**
 * This is reserved for system use.
 * DO NOT use this directly in your application code.
 */
public class ServiceQueue {
    private static final Logger log = LoggerFactory.getLogger(ServiceQueue.class);
    private static final String READY = "ready";
    private static final String HASH = "#";
    private static final String PUBLIC = "PUBLIC";
    private static final String PRIVATE = "PRIVATE";
    private final ElasticQueue elasticQueue;
    private final ServiceDef service;
    private final String readyPrefix;
    private final String streamRoute;
    private final EventBus system;
    private final ConcurrentLinkedQueue<String> fifo = new ConcurrentLinkedQueue<>();
    private final ConcurrentMap<String, Boolean> idx = new ConcurrentHashMap<>();
    private final List<WorkerQueues> workers = new ArrayList<>();
    private MessageConsumer<Object> consumer;
    private boolean buffering = true;
    private boolean stopped = false;

    public ServiceQueue(ServiceDef service) {
        this.service = service;
        String route = service.getRoute();
        this.readyPrefix = READY+":" + route + HASH;
        this.elasticQueue = new ElasticQueue(route);
        // create consumer
        system = Platform.getInstance().getEventSystem();
        consumer = system.localConsumer(route, new ServiceHandler());
        if (service.isStream()) {
            streamRoute = route + HASH + 1;
            StreamQueue worker = new StreamQueue(service, streamRoute);
            workers.add(worker);
            log.info("{} {} started as stream function", service.isPrivate() ? PRIVATE : PUBLIC, route);
        } else {
            streamRoute = null;
            int instances = service.getConcurrency();
            for (int i = 0; i < instances; i++) {
                int n = i + 1;
                WorkerQueue worker = new WorkerQueue(service, route + HASH + n, n);
                workers.add(worker);
            }
            if (service.isKotlin()) {
                if (instances == 1) {
                    log.info("{} {} started as suspend function", service.isPrivate() ? PRIVATE : PUBLIC, route);
                } else {
                    log.info("{} {} with {} instances started as suspend function",
                            service.isPrivate() ? PRIVATE : PUBLIC, route, instances);
                }
            } else if (service.isKernelThread()) {
                if (instances == 1) {
                    log.info("{} {} started as kernel thread", service.isPrivate() ? PRIVATE : PUBLIC, route);
                } else {
                    log.info("{} {} with {} instances started as kernel threads",
                            service.isPrivate() ? PRIVATE : PUBLIC, route, instances);
                }
            } else {
                if (instances == 1) {
                    log.info("{} {} started as virtual thread", service.isPrivate() ? PRIVATE : PUBLIC, route);
                } else {
                    log.info("{} {} with {} instances started as virtual threads",
                            service.isPrivate() ? PRIVATE : PUBLIC, route, instances);
                }
            }
        }
    }

    public String getRoute() {
        return service.getRoute();
    }

    public ServiceDef getService() {
        return service;
    }

    public int getFreeWorkers() {
        return idx.size();
    }

    public long getReadCounter() {
        return elasticQueue.getReadCounter();
    }

    public long getWriteCounter() {
        return elasticQueue.getWriteCounter();
    }

    public void stop() {
        if (consumer != null && consumer.isRegistered()) {
            // closing consumer
            consumer.unregister();
            // stopping worker
            for (WorkerQueues w: workers) {
                w.stop();
            }
            // completely close the associated elastic queue
            elasticQueue.destroy();
            consumer = null;
            stopped = true;
            log.info("{} stopped", service.getRoute());
        }
    }

    private class ServiceHandler implements Handler<Message<Object>> {

        @Override
        public void handle(Message<Object> message) {
            Object body = message.body();
            if (!stopped) {
                if (body instanceof String input) {
                    String worker = getWorker(input);
                    if (worker != null) {
                        // Just for the safe side, this guarantees that a unique worker is inserted
                        idx.computeIfAbsent(worker, d -> {
                            fifo.add(worker);
                            return true;
                        });
                        if (buffering) {
                            byte[] event = elasticQueue.read();
                            if (event.length == 0) {
                                // Close elastic queue when all messages are cleared
                                buffering = false;
                                elasticQueue.close();
                            } else {
                                // Guarantees that there is an available worker
                                String nextWorker = fifo.poll();
                                if (nextWorker != null) {
                                    idx.remove(nextWorker);
                                    system.send(nextWorker, event);
                                }
                            }
                        }
                    }
                }
                if (body instanceof byte[] event) {
                    if (buffering) {
                        // Once elastic queue is started, we will continue buffering.
                        elasticQueue.write(event);
                    } else {
                        // Check if a next worker is available
                        String nextWorker = fifo.peek();
                        if (nextWorker == null) {
                            // Start persistent queue when no workers are available
                            buffering = true;
                            elasticQueue.write(event);
                        } else {
                            // Deliver event to the next worker
                            nextWorker = fifo.poll();
                            if (nextWorker != null) {
                                idx.remove(nextWorker);
                                system.send(nextWorker, event);
                            }
                        }
                    }
                }
            }
        }

        private String getWorker(String input) {
            if (input.startsWith(readyPrefix)) {
                return input.substring(READY.length()+1);
            } else if (READY.equals(input) && streamRoute != null) {
                return streamRoute;
            }
            return null;
        }
    }
}
