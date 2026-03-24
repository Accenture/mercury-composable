package com.accenture.services.plugins.logical;

import com.accenture.models.PluginFunction;
import com.accenture.models.SimplePlugin;

import java.util.List;

@SimplePlugin
public class IncludesOperator implements PluginFunction {

    @Override
    public String getName() {
        return "includes";
    }

    @Override
    public Object calculate(Object... input) {
        if (input.length == 2) {
            if (input[0] instanceof List<?> items) {
                return items.contains(input[1]);
            } else {
                // case insensitive comparison
                var source = String.valueOf(input[0]).toLowerCase();
                var text = String.valueOf(input[1]).toLowerCase();
                return source.contains(text.toLowerCase());
            }
        }
        return false;
    }
}
