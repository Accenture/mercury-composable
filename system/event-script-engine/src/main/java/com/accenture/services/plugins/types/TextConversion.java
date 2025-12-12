package com.accenture.services.plugins.types;

import com.accenture.utils.TypeConversionUtils;
import com.accenture.models.SimplePlugin;
import com.accenture.models.PluginFunction;

import java.util.Arrays;

@SimplePlugin
public class TextConversion implements PluginFunction {

    @Override
    public String getName() {
        return "text";
    }

    @Override
    public Object calculate(Object... input) {
        if(input.length == 0){
            throw new IllegalArgumentException("Input is required for Text conversion");
        }
        if(input.length == 1){
            return TypeConversionUtils.getTextValue(input[0]);
        }

        return Arrays.stream(input)
                .map(TypeConversionUtils::getTextValue)
                .toList();
    }
}
