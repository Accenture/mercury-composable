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
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ObjectMessage;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.Utility;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * This is reserved for system use.
 * DO NOT use this directly in your application code.
 */
@Plugin(name = "JsonLogger", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class JsonAppender extends AbstractAppender {

    private static final Gson serializer = SimpleMapper.getInstance().getPrettyGson();
    private static final Utility util = Utility.getInstance();

    protected JsonAppender(String name, Filter filter,
                           Layout<? extends Serializable> layout,
                           boolean ignoreExceptions, Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
        // disable other use of System.out
        System.setOut(new SystemOutFilter(System.out));
        System.setErr(new SystemOutFilter(System.err));
    }

    @PluginFactory
    public static JsonAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Filter") Filter filter,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Properties") Property[] properties) {
        return new JsonAppender(name, filter, layout, true, properties);
    }

    @Override
    public void append(LogEvent event) {
        if (event != null) {
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("time", util.getLocalTimestamp());
                data.put("level", String.valueOf(event.getLevel()));
                data.put("source", String.valueOf(event.getSource()));
                Message message = event.getMessage();
                if (message instanceof ObjectMessage obj) {
                    data.put("message", String.valueOf(obj.getParameter()));
                } else if (message != null) {
                    /*
                     * Support logging of map for the following use case
                     * log.info("{}", map);
                     */
                    var text = message.getFormattedMessage().trim();
                    Object[] objects = message.getParameters();
                    if (objects != null && objects.length == 1 && objects[0] instanceof Map &&
                            text.startsWith("{") && text.endsWith("}")) {
                        data.put("message", objects[0]);
                    } else {
                        data.put("message", text);
                    }
                }
                Throwable ex = event.getThrown();
                if (ex != null) {
                    data.put("stack", getStackTrace(ex));
                }
                System.out.print(serializer.toJson(data));
            } catch (Exception e) {
                // nothing we can do
            }
        }
    }

    private String getStackTrace(Throwable ex) {
        try (StringWriter out = new StringWriter(); PrintWriter writer = new PrintWriter(out)) {
            ex.printStackTrace(writer);
            return out.toString();
        } catch (IOException e) {
            // best effort is to keep the exception message
            return ex.getMessage();
        }
    }
}
