package com.accenture.services.plugins.arithmetic;

import com.accenture.models.SimplePlugin;
import com.accenture.utils.SimplePluginUtils;
import com.accenture.models.PluginFunction;

import java.util.Arrays;

@SimplePlugin
public class AddNumbers implements PluginFunction {

    @Override
    public String getName() {
        return "add";
    }

    @Override
    public Object calculate(Object... input) {
        if(input.length == 0){
            throw new IllegalArgumentException("Input is required for addition");
        }

        return SimplePluginUtils.promoteInput(input)
                .reduce(Long::sum)
                .orElseThrow(() -> new IllegalStateException("Could not add the input: " + Arrays.toString(input)));
    }
}
