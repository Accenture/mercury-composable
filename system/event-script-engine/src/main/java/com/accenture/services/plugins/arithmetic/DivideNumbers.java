package com.accenture.services.plugins.arithmetic;

import com.accenture.utils.SimplePluginUtils;
import com.accenture.models.simplePlugin;
import com.accenture.models.PluginFunction;

import java.util.Arrays;

@SimplePlugin
public class DivideNumbers implements PluginFunction {

    @Override
    public String getName() {
        return "div";
    }

    @Override
    public Object calculate(Object... input) {
        SimplePluginUtils.divideByZeroCheck(input);

        if(input.length == 0 ){
            throw new IllegalArgumentException("Expected at least two Whole Numbers to divide");
        }

        return SimplePluginUtils.promoteInput(input)
                .reduce((l1, l2) -> l1 / l2)
                .orElseThrow(() -> new IllegalStateException("Could not divide the input: " + Arrays.toString(input)));
    }

}
