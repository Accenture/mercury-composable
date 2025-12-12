package com.accenture.services.plugins.arithmetic;

import com.accenture.utils.SimplePluginUtils;
import com.accenture.models.SimplePlugin;
import com.accenture.models.PluginFunction;

import java.util.Arrays;

@SimplePlugin
public class SubtractNumbers implements PluginFunction {

    @Override
    public String getName() {
        return "subtract";
    }

    @Override
    public Object calculate(Object... input) {
        if(input.length == 0){
            throw new IllegalArgumentException("Input is required for subtraction");
        }

        return SimplePluginUtils.promoteInput(input)
                .reduce((a,b) -> a - b)
                .orElseThrow(() -> new IllegalStateException("Could not add the input: " + Arrays.toString(input)));
    }
}
