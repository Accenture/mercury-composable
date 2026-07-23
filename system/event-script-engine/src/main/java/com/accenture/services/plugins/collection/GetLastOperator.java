package com.accenture.services.plugins.collection;

import com.accenture.models.PluginFunction;
import com.accenture.models.SimplePlugin;

import java.util.List;

@SimplePlugin
public class GetLastOperator implements PluginFunction {

    @Override
    public String getName() {
        return "getLast";
    }

    @Override
    public Object calculate(Object... input) {
        if (input == null || input.length != 1) {
            throw new IllegalArgumentException("One input is required to get last item from list");
        }

        Object value = input[0];

        if (value == null) {
            throw new IllegalArgumentException("Input cannot be null to get last item from list");
        }

        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException("Input must be a list to get last item");
        }

        if (list.isEmpty()) {
            throw new IllegalArgumentException("Input cannot be empty to get last item from list");
        }

        return list.getLast();
    }
}
