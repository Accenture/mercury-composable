package com.accenture.services.plugins.arithmetic;

import com.accenture.utils.SimplePluginUtils;
import org.platformlambda.core.annotations.SimplePlugin;
import org.platformlambda.core.models.PluginFunction;

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

        return SimplePluginUtils.promoteInput(input)
                .reduce((l1, l2) -> l1 / l2)
                .orElseThrow(() -> new IllegalStateException("Could not divide the input: " + Arrays.toString(input)));
    }

}
