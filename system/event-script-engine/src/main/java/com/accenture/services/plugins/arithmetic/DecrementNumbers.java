package com.accenture.services.plugins.arithmetic;

import com.accenture.utils.SimplePluginUtils;
import com.accenture.models.SimplePlugin;
import com.accenture.models.PluginFunction;

@SimplePlugin
public class DecrementNumbers implements PluginFunction {

    @Override
    public String getName() {
        return "decrement";
    }

    @Override
    public Object calculate(Object... input) {
        if(input.length == 1){
            return SimplePluginUtils.promoteNumber(input[0]) - 1L;
        }

        throw new IllegalArgumentException("Expected exactly one Whole Number to decrement");
    }
}
