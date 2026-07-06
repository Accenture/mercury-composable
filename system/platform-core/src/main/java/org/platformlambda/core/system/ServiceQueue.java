/*

    Copyright 2018-2026 Accenture Technology

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
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.ElasticQueue;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private static final int UUID_LENGTH = Utility.getInstance().getUuid().length();
    private static final AtomicBoolean DISPATCH_MODE_LOGGED = new AtomicBoolean(false);
    private static final String CALLBACK_PREFIX = "callback.";
    private static final String STREAM_PREFIX = "stream.";
    private static final String STREAM_IN = ".in";
    private static final int CALLBACK_LENGTH = CALLBACK_PREFIX.length() + UUID_LENGTH;
    private static final int STREAM_IN_LENGTH = STREAM_PREFIX.length() + UUID_LENGTH + STREAM_IN.length();
    private static final String DISPATCH_MAILBOX_SIZE = "elastic.queue.dispatch.mailbox.size";
    private static final int DEFAULT_DISPATCH_MAILBOX_SIZE = 1024;
    private final ElasticQueue elasticQueue;
    private final ServiceDef service;
    private final String readyPrefix;
    private final String streamRoute;
    private final boolean isControlRoute;
    private final EventBus system;
    private final ConcurrentLinkedQueue<String> fifo = new ConcurrentLinkedQueue<>();
    private final ConcurrentMap<String, Boolean> idx = new ConcurrentHashMap<>();
    private final List<WorkerQueues> workers = new ArrayList<>();
    private MessageConsumer<Object> consumer;
    private boolean buffering = true;
    private volatile boolean stopped = false;
    // Off-loop dispatch, used when the elastic store is virtual-thread-safe (the file store): the Vert.x
    // consumer enqueues to this bounded mailbox (blocking only when full for back-pressure) and a per-route
    // virtual thread runs the state machine + blocking spill I/O. In loop-dispatch mode (bdb) the mailbox is
    // null and the state machine runs inline on the event loop.
    private final BlockingQueue<Object> mailbox;
    private final AtomicBoolean mailboxBackPressureLogged = new AtomicBoolean(false);
    private Thread dispatchThread;

    public ServiceQueue(ServiceDef service) {
        this.service = service;
        String route = service.getRoute();
        this.isControlRoute = isControlRoute(service, route);
        this.readyPrefix = READY + ":" + route + HASH;
        this.streamRoute = service.isStream() ? route + HASH + 1 : null;
        this.elasticQueue = new ElasticQueue(route);
        this.system = Platform.getInstance().getEventSystem();
        // Dispatch mode is derived from the store: a virtual-thread-safe store (file) runs off the loop on a
        // per-route VT; a carrier-pinning store (bdb) runs inline on the loop. One knob (the store), two safe
        // modes — the unsafe vthread+bdb combo is unreachable.
        boolean vthreadDispatch = elasticQueue.supportsVirtualThreadDispatch();
        this.mailbox = vthreadDispatch ? new LinkedBlockingQueue<>(dispatchMailboxSize()) : null;
        initConsumer(route, vthreadDispatch, new ServiceHandler());
        setupWorkers(route);
    }

    static int dispatchMailboxSize() {
        int configured = Utility.getInstance().str2int(AppConfigReader.getInstance().getProperty(
                DISPATCH_MAILBOX_SIZE, String.valueOf(DEFAULT_DISPATCH_MAILBOX_SIZE)));
        int size = configured > 0 ? configured : DEFAULT_DISPATCH_MAILBOX_SIZE;
        return Math.max(ElasticQueue.MEMORY_BUFFER, size);
    }

    private static boolean isControlRoute(ServiceDef service, String route) {
        return service.isStream()
                || (route.startsWith(CALLBACK_PREFIX) && route.length() == CALLBACK_LENGTH)
                || (route.startsWith(STREAM_PREFIX) && route.length() == STREAM_IN_LENGTH && route.endsWith(STREAM_IN));
    }

    private void initConsumer(String route, boolean vthreadDispatch, ServiceHandler handler) {
        if (vthreadDispatch) {
            if (DISPATCH_MODE_LOGGED.compareAndSet(false, true)) {
                log.info("ServiceQueue dispatch = vthread (per-route virtual thread; store is virtual-thread-safe)");
            }
            // event loop enqueues; if the bounded mailbox fills, block here to apply back-pressure, not drops
            consumer = system.localConsumer(route, message -> {
                if (!stopped) {
                    enqueue(route, message.body());
                }
            });
            dispatchThread = Thread.ofVirtual().name("dispatch." + route).start(() -> drainLoop(handler));
        } else {
            // default: the state machine runs inline on the event-loop thread (unchanged behaviour)
            consumer = system.localConsumer(route, handler);
        }
    }

    int getDispatchMailboxRemainingCapacity() {
        return mailbox == null ? 0 : mailbox.remainingCapacity();
    }

    private void enqueue(String route, Object body) {
        if (mailbox.offer(body)) {
            return;
        }
        if (mailboxBackPressureLogged.compareAndSet(false, true)) {
            log.warn("{} dispatch mailbox full (capacity={}); applying back-pressure",
                    route, dispatchMailboxSize());
        }
        // block here to apply back-pressure until space frees or the route stops; an interrupt
        // (shutdown) restores the flag and abandons this event
        try {
            while (!stopped) {
                if (mailbox.offer(body, 100, TimeUnit.MILLISECONDS)) {
                    return;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void setupWorkers(String route) {
        if (service.isStream()) {
            workers.add(new StreamQueue(service, streamRoute));
            log.debug("{} {} started as stream function", service.isPrivate() ? PRIVATE : PUBLIC, route);
        } else {
            int instances = service.getConcurrency();
            for (int i = 0; i < instances; i++) {
                int n = i + 1;
                workers.add(new WorkerDispatcher(service, route + HASH + n, n));
            }
            var type = service.isKernelThread() ? "kernel" : "virtual";
            if (instances == 1) {
                startupLog(route, type);
            } else {
                startupLog(route, instances, type);
            }
        }
    }

    private void startupLog(String route, String type) {
        if (this.isControlRoute) {
            log.debug("{} {} started as {} thread", service.isPrivate() ? PRIVATE : PUBLIC, route, type);
        } else {
            log.info("{} {} started as {} thread", service.isPrivate() ? PRIVATE : PUBLIC, route, type);
        }
    }

    private void startupLog(String route, int instances, String type) {
        if (this.isControlRoute) {
            log.debug("{} {} with {} instances started as {} threads",
                    service.isPrivate() ? PRIVATE : PUBLIC, route, instances, type);
        } else {
            log.info("{} {} with {} instances started as {} threads",
                    service.isPrivate() ? PRIVATE : PUBLIC, route, instances, type);
        }
    }

    public String getRoute() {
        return service.getRoute();
    }

    public ServiceDef getService() {
        return service;
    }

    /**
     * Per-route dispatch loop used when the store is virtual-thread-safe: runs the state machine + blocking
     * spill I/O on a virtual thread, so a disk/OS stall parks this carrier instead of the shared event loop.
     * A caught exception (e.g. a transient spill I/O error) is logged without killing the route's dispatch.
     */
    private void drainLoop(ServiceHandler handler) {
        while (!stopped) {
            try {
                Object body = mailbox.take();
                if (!stopped) {
                    handler.process(body);
                }
            } catch (InterruptedException e) {
                // interrupt is the stop signal: restore the flag and let the loop exit
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("{} dispatch error - {}", service.getRoute(), e.getMessage());
            }
        }
    }

    public void stop() {
        if (consumer != null && consumer.isRegistered()) {
            stopped = true;
            // closing consumer (no further enqueues)
            consumer.unregister();
            // wake + let the per-route dispatch virtual thread finish before destroying the elastic queue
            if (dispatchThread != null) {
                dispatchThread.interrupt();
                try {
                    dispatchThread.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            // stopping worker
            for (WorkerQueues w: workers) {
                w.stop();
            }
            // completely close the associated elastic queue
            elasticQueue.destroy();
            consumer = null;
            if (this.isControlRoute) {
                log.debug("{} stopped", service.getRoute());
            } else {
                log.info("{} stopped", service.getRoute());
            }
        }
    }

    private class ServiceHandler implements Handler<Message<Object>> {

        @Override
        public void handle(Message<Object> message) {
            // loop-dispatch mode: run inline on the event-loop thread (vthread mode enqueues instead)
            process(message.body());
        }

        void process(Object body) {
            if (!stopped) {
                if (body instanceof String input) {
                    processReadySignal(input);
                }
                if (body instanceof byte[] event) {
                    processEvent(event);
                }
            }
        }

        private void processReadySignal(String input) {
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

        private void processEvent(byte[] event) {
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
