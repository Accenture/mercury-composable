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

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.platformlambda.core.util.Utility;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public abstract class JsonLogger extends AbstractAppender {
    private static final Utility util = Utility.getInstance();

    protected JsonLogger(String name, Filter filter,
                         Layout<? extends Serializable> layout,
                         boolean ignoreExceptions, Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
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
                return getMapContent(message, mutableEvent.getFormat(), message.getParameters());
            } else if (message instanceof ParameterizedMessage msg) {
                return getMapContent(message, msg.getFormat(), message.getParameters());
            } else if (message instanceof ObjectMessage obj) {
                return String.valueOf(obj.getParameter());
            } else {
                return message.getFormattedMessage();
            }
        } else {
            return "null";
        }
    }

    /**
     * Handle the use case to log JSON (map of key-values) using log.info("{}", keyValues)
     *
     * @param message object
     * @param format should be "{}"
     * @param objects are the log parameters
     * @return map of key-values
     */
    private Object getMapContent(Message message, String format, Object[] objects) {
        // Note that it is possible to have 2 objects where the second one is a Throwable
        if ("{}".equals(format) && objects != null && objects.length > 0 && objects[0] instanceof Map) {
            return objects[0];
        } else {
            return message.getFormattedMessage();
        }
    }
}
