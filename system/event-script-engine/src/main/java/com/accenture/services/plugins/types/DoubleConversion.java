package com.accenture.services.plugins.types;

import com.accenture.utils.TypeConversionUtils;
import com.accenture.models.SimplePlugin;
import com.accenture.models.PluginFunction;

import java.util.Arrays;
import java.util.List;

@SimplePlugin
public class DoubleConversion implements PluginFunction {

    @Override
    public String getName() {
        return "double";
    }

    @Override
    public Object calculate(Object... input) {
        if(input.length == 0){
            throw new IllegalArgumentException("Input is required for Double conversion");
        }
        else if(input.length == 1){
            return TypeConversionUtils.convertDouble(input[0]);
        }

        return Arrays.stream(input)
                .map(TypeConversionUtils::convertDouble)
                .toList();
    }


}
