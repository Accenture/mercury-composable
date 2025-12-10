package com.accenture.services.plugins.types;

import com.accenture.utils.TypeConversionUtils;
import com.accenture.models.simplePlugin;
import com.accenture.models.PluginFunction;

import java.util.Arrays;

@SimplePlugin
public class BooleanConversion implements PluginFunction {

    @Override
    public String getName() {
        return "boolean";
    }

    @Override
    public Object calculate(Object... input) {
        //If size of input is one - then convert directly
        if(input.length == 0){
            throw new IllegalArgumentException("Input is required to convert Boolean");
        }
        else if(input.length == 1){
            return TypeConversionUtils.convertBoolean(input[0]);
        }

        return Arrays.stream(input)
                .map(TypeConversionUtils::convertBoolean)
                .toList();
    }
}
