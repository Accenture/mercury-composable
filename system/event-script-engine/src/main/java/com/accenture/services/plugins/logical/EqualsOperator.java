package com.accenture.services.plugins.logical;

import com.accenture.utils.TypeConversionUtils;
import com.accenture.models.SimplePlugin;
import com.accenture.models.PluginFunction;

import java.util.Arrays;
import java.util.Objects;

@SimplePlugin
public class EqualsOperator implements PluginFunction {

    @Override
    public String getName() {
        return "eq";
    }

    @Override
    public Object calculate(Object... input) {
        if(input.length == 0){
            throw new IllegalArgumentException("Input is required to check for equality");
        }

        return Arrays.stream(input)
                .reduce(Objects::equals)
                .orElse(false);
    }
}
