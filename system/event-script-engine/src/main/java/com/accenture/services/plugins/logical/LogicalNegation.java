package com.accenture.services.plugins.logical;

import com.accenture.utils.TypeConversionUtils;
import org.platformlambda.core.annotations.SimplePlugin;
import org.platformlambda.core.models.PluginFunction;

import java.util.Arrays;

@SimplePlugin
public class LogicalNegation implements PluginFunction {

    @Override
    public String getName() {
        return "not";
    }

    @Override
    public Object calculate(Object... input) {
        if(input.length != 1){
            throw new IllegalArgumentException("One single input is required for negation - got: " + Arrays.toString(input));
        }

        return ! TypeConversionUtils.convertBoolean(input[0]);
    }
}
