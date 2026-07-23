package com.accenture.services.plugins.collection;

import com.accenture.models.PluginFunction;
import com.accenture.models.SimplePlugin;

import java.util.List;

@SimplePlugin
public class GetFirstOperator implements PluginFunction {

    @Override
    public String getName() {
        return "getFirst";
    }

    @Override
    public Object calculate(Object... input) {
        if (input == null || input.length != 1) {
            throw new IllegalArgumentException("One input is required to get first item from list");
        }

        Object value = input[0];

        if (value == null) {
            throw new IllegalArgumentException("Input cannot be null to get first item from list");
        }

        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException("Input must be a list to get first item");
        }

        if (list.isEmpty()) {
            throw new IllegalArgumentException("Input cannot be empty to get first item from list");
        }

        return list.getFirst();
    }
}
