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

package com.accenture.services.plugins.types;

import com.accenture.models.PluginFunction;
import com.accenture.models.SimplePlugin;

@SimplePlugin
public class SetConfigParameter implements PluginFunction {

    @Override
    public String getName() {
        return "setConfig";
    }

    /**
     * Set or override configuration parameter using system property
     * <p>
     * input data mapping statement example:
     * 'f:setConfig(text(my.parameter), text(demo)) -> config_updated'
     * In this example, the value 'demo' is set as config parameter 'my.parameter'.
     * The second argument can be a model variable. It may be any object that
     * will be converted to text using String.valueOf(value).
     * <p>
     * Please be careful about life cycle for your application start up sequence.
     * If your MainApplication needs the updated parameter, you must run a flow
     * to invoke this plugin first. The typical use case is to retrieve secrets
     * from a cloud "secret manager" and then set the secrets in the base
     * configuration - since System properties would override a configuration
     * parameter at run-time.
     *
     * @param input key and value
     * @return value of true if success or false if syntax error or invalid data
     */
    @Override
    public Object calculate(Object... input) {
        if (input.length == 2 && input[0] instanceof String key && !key.isBlank() && input[1] != null) {
            System.setProperty(key, String.valueOf(input[1]));
            return true;
        } else {
            return false;
        }
    }
}
