package com.accenture.services.plugins.types;

import com.accenture.utils.TypeConversionUtils;
import com.accenture.models.SimplePlugin;
import com.accenture.models.PluginFunction;

import java.util.Arrays;
import java.util.stream.Collectors;

@SimplePlugin
public class ConcatenateStringsPlugin implements PluginFunction {

    @Override
    public String getName() {
        return "concat";
    }

    @Override
    public Object calculate(Object... input) {
        if(input.length == 0){
            throw new IllegalArgumentException("Input is required for String Concatenation");
        }

        return Arrays.stream(input)
                .map(TypeConversionUtils::getTextValue)
                .collect(Collectors.joining(""));
    }
}
