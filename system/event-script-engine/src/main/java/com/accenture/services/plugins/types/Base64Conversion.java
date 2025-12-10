package com.accenture.services.plugins.types;

import com.accenture.utils.TypeConversionUtils;
import com.accenture.models.simplePlugin;
import com.accenture.models.PluginFunction;

import java.util.Arrays;

@SimplePlugin
public class Base64Conversion implements PluginFunction {

    @Override
    public String getName() {
        return "b64";
    }

    @Override
    public Object calculate(Object... input) {
        if(input.length == 0){
            throw new IllegalArgumentException("Input is required for Base64 conversion");
        }
        else if(input.length == 1){
            return TypeConversionUtils.getB64(input[0]);
        }

        return Arrays.stream(input)
                .map(TypeConversionUtils::getB64)
                .toList();
    }
}
