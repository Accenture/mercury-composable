package com.accenture.services.plugins.arithmetic;

import com.accenture.utils.SimplePluginUtils;
import org.platformlambda.core.annotations.SimplePlugin;
import org.platformlambda.core.models.PluginFunction;

import java.util.Arrays;

@SimplePlugin
public class AddNumbers implements PluginFunction {

    @Override
    public String getName() {
        return "add";
    }

    @Override
    public Object calculate(Object... input) {
        return SimplePluginUtils.promoteInput(input)
                .reduce(Long::sum)
                .orElseThrow(() -> new IllegalStateException("Could not add the input: " + Arrays.toString(input)));
    }
}
