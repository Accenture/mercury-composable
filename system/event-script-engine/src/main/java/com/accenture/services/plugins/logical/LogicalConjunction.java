package com.accenture.services.plugins.logical;

import com.accenture.utils.TypeConversionUtils;
import com.accenture.models.simplePlugin;
import com.accenture.models.PluginFunction;

import java.util.Arrays;

@SimplePlugin
public class LogicalConjunction implements PluginFunction {

    @Override
    public String getName() {
        return "and";
    }

    @Override
    public Object calculate(Object... input) {
        if(input.length == 0){
            throw new IllegalArgumentException("Input is required to 'AND' values");
        }

        return Arrays.stream(input)
                .allMatch(TypeConversionUtils::convertBoolean);
    }
}
