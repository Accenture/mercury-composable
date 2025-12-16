package com.accenture.services.plugins.arithmetic;

import com.accenture.utils.SimplePluginUtils;
import com.accenture.models.SimplePlugin;
import com.accenture.models.PluginFunction;

import java.util.Arrays;

@SimplePlugin
public class MultiplyNumbers implements PluginFunction {

    @Override
    public String getName() {
        return "multiply";
    }

    @Override
    public Object calculate(Object... input) {
        if(input.length == 0){
            throw new IllegalArgumentException("Input is required for multiplication");
        }

        return SimplePluginUtils.promoteInput(input)
                .reduce((l1, l2) -> l1 * l2)
                .orElseThrow(() -> new IllegalStateException("Could not multiply the input: " + Arrays.toString(input)));
    }

}
