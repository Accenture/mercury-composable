package com.accenture.services.plugins.arithmetic;

import org.platformlambda.core.annotations.SimplePlugin;

import java.util.Arrays;

@SimplePlugin
public class AddNumbers extends SimpleNumberPlugin {

    @Override
    public String getName() {
        return "add";
    }

    @Override
    public Object calculate(Object... input) {
        return promoteInput(input)
                .reduce(Long::sum)
                .orElseThrow(() -> new IllegalStateException("Could not add the input: " + Arrays.toString(input)));
    }
}
