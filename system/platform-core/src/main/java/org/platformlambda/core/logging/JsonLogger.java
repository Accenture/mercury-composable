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

package org.platformlambda.core.logging;

import com.google.gson.Gson;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.Utility;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class JsonLogger extends AbstractAppender {
    protected static final ConcurrentLinkedQueue<Map<String, Object>> queue = new ConcurrentLinkedQueue<>();
    private static final Gson prettySerializer = SimpleMapper.getInstance().getPrettyGson();
    private static final Gson compactSerializer = SimpleMapper.getInstance().getCompactGson();
    private static final Utility util = Utility.getInstance();
    private static final AtomicInteger counter = new AtomicInteger(0);
    private static boolean running = true;

    protected JsonLogger(String name, Filter filter,
                         Layout<? extends Serializable> layout,
                         boolean ignoreExceptions, Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
        if (counter.incrementAndGet() == 1) {
            // perform asynchronous logging and do System.out orderly
            Platform.getInstance().getVirtualThreadExecutor().submit(() -> {
                Runtime.getRuntime().addShutdownHook(new Thread(JsonLogger::shutdown));
                while (running) {
                    var message = queue.poll();
                    if (message == null) {
                        try {
                            // Thread.sleep is non-blocking in a virtual thread
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            // just ignore it
                        }
                    } else {
                        // enforce data contract
                        var pretty = message.get("0");
                        var data = message.get("1");
                        if (pretty instanceof Boolean prettyPrint && data instanceof Map) {
                            try {
                                if (prettyPrint) {
                                    System.out.println(prettySerializer.toJson(data));
                                } else {
                                    System.out.println(compactSerializer.toJson(data));
                                }
                            } catch (Exception e) {
                                // guarantee printing even when serializer fails
                                System.out.println(data);
                            }
                        }
                    }
                }
            });
        }
    }

    private static void shutdown() {
        running = false;
    }

    protected Map<String, Object> getJson(LogEvent event) {
        Map<String, Object> data = new HashMap<>();
        data.put("time", util.getLocalTimestamp(event.getTimeMillis()));
        data.put("level", String.valueOf(event.getLevel()));
        data.put("source", String.valueOf(event.getSource()));
        data.put("message", getMessage(event));
        data.put("thread", event.getThreadId());
        Throwable ex = event.getThrown();
        if (ex != null) {
            data.putAll(util.stackTraceToMap(util.getStackTrace(ex)));
        }
        return data;
    }

    private Object getMessage(LogEvent event) {
        Message message = event.getMessage();
        if (message != null) {
            // variances of log event
            if (event instanceof MutableLogEvent mutableEvent) {
                var content = getMapContent(mutableEvent.getFormat(), message.getParameters());
                if (content != null) {
                    return content;
                }
            } else if (message instanceof ParameterizedMessage msg) {
                var content = getMapContent(msg.getFormat(), message.getParameters());
                if (content != null) {
                    return content;
                }
            } else if (message instanceof ObjectMessage obj) {
                return String.valueOf(obj.getParameter());
            }
            return message.getFormattedMessage();
        } else {
            return "null";
        }
    }

    /**
     * Handle the use case to log JSON (map of key-values) using log.info("{}", keyValues)
     *
     * @param format should be "{}"
     * @param objects are the log parameters
     * @return map of key-values
     */
    private Object getMapContent(String format, Object[] objects) {
        if ("{}".equals(format)) {
            // Note that it is possible to have 2 objects where the second one is a Throwable
            if (objects != null && objects.length > 0 && objects[0] instanceof Map) {
                return objects[0];
            }
        }
        return null;
    }
}
