package com.accenture.services.plugins.logical;

import com.accenture.utils.TypeConversionUtils;
import com.accenture.models.simplePlugin;
import com.accenture.models.PluginFunction;

import java.util.Arrays;

@SimplePlugin
public class TernaryOperator implements PluginFunction {

    @Override
    public String getName() {
        return "ternary";
    }

    @Override
    public Object calculate(Object... input) {

        if(input.length != 3){
            throw new IllegalArgumentException("Three parts are required for Ternary Operation - got: " + Arrays.toString(input));
        }

        boolean condition = TypeConversionUtils.convertBoolean(input[0]);
        return condition? input[1] : input[2];
    }
}
