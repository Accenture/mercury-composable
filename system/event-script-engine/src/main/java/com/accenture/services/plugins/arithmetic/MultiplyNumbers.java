package com.accenture.services.plugins.arithmetic;

import com.accenture.utils.SimplePluginUtils;
import org.platformlambda.core.annotations.SimplePlugin;
import org.platformlambda.core.models.PluginFunction;

import java.util.Arrays;

@SimplePlugin
public class MultiplyNumbers implements PluginFunction {

    @Override
    public String getName() {
        return "multiply";
    }

    @Override
    public Object calculate(Object... input) {
        return SimplePluginUtils.promoteInput(input)
                .reduce((l1, l2) -> l1 * l2)
                .orElseThrow(() -> new IllegalStateException("Could not multiply the input: " + Arrays.toString(input)));
    }

}
