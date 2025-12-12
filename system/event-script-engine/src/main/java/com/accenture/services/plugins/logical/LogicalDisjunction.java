package com.accenture.services.plugins.logical;

import com.accenture.utils.TypeConversionUtils;
import com.accenture.models.SimplePlugin;
import com.accenture.models.PluginFunction;

import java.util.Arrays;

@SimplePlugin
public class LogicalDisjunction implements PluginFunction {

    @Override
    public String getName() {
        return "or";
    }

    @Override
    public Object calculate(Object... input) {
        if(input.length == 0){
            throw new IllegalArgumentException("Input is required to 'OR' values");
        }

        return Arrays.stream(input)
                .anyMatch(TypeConversionUtils::convertBoolean);
    }
}
