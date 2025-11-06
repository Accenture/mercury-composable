package com.accenture.services.plugins.arithmetic;

import org.platformlambda.core.annotations.SimplePlugin;

import java.util.Arrays;

@SimplePlugin
public class SubtractNumbers extends SimpleNumberPlugin {

    @Override
    public String getName() {
        return "subtract";
    }

    @Override
    public Object calculate(Object... input) {
        return promoteInput(input)
                .reduce((a,b) -> a - b)
                .orElseThrow(() -> new IllegalStateException("Could not add the input: " + Arrays.toString(input)));
    }
}
