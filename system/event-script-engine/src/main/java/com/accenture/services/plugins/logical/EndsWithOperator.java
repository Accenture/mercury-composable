package com.accenture.services.plugins.logical;

import com.accenture.models.PluginFunction;
import com.accenture.models.SimplePlugin;

@SimplePlugin
public class EndsWithOperator implements PluginFunction {

    @Override
    public String getName() {
        return "endsWith";
    }

    @Override
    public Object calculate(Object... input) {
        if (input.length == 2) {
            // case insensitive comparison
            var source = String.valueOf(input[0]).toLowerCase();
            var text = String.valueOf(input[1]).toLowerCase();
            return source.endsWith(text.toLowerCase());
        }
        return false;
    }
}
