package com.accenture.services.plugins.types;

import com.accenture.utils.TypeConversionUtils;
import com.accenture.models.simplePlugin;
import com.accenture.models.PluginFunction;

import java.util.Arrays;
import java.util.List;

@SimplePlugin
public class FloatConversion implements PluginFunction {

    @Override
    public String getName() {
        return "float";
    }

    @Override
    public Object calculate(Object... input) {
        if(input.length == 0){
            throw new IllegalArgumentException("Input is required for Float conversion");
        }
        else if(input.length == 1){
            return TypeConversionUtils.convertFloat(input[0]);
        }

        return Arrays.stream(input)
                .map(TypeConversionUtils::convertFloat)
                .toList();
    }


}
