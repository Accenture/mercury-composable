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
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.platformlambda.core.serializers.SimpleMapper;

import java.io.Serializable;

/**
 * This is reserved for system use.
 * DO NOT use this directly in your application code.
 */
@Plugin(name = "JsonLogger", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class JsonAppender extends JsonLogger {
    private static final Gson serializer = SimpleMapper.getInstance().getPrettyGson();

    protected JsonAppender(String name, Filter filter,
                           Layout<? extends Serializable> layout,
                           boolean ignoreExceptions, Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
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
            System.out.print(serializer.toJson(getJson(event)) + "\n");
        }
    }
}
