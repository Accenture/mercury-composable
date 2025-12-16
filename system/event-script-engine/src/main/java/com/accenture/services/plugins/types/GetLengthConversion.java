package com.accenture.services.plugins.types;

import com.accenture.utils.TypeConversionUtils;
import com.accenture.models.SimplePlugin;
import com.accenture.models.PluginFunction;

@SimplePlugin
public class GetLengthConversion implements PluginFunction {

    @Override
    public String getName() {
        return "length";
    }

    @Override
    public Object calculate(Object... input) {
        if(input.length == 1){
            return TypeConversionUtils.getLength(input[0]);
        }

        throw new IllegalArgumentException("Expected exactly one argument in order to get Length");
    }
}
