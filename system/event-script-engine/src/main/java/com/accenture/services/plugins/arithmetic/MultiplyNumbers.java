package com.accenture.services.plugins.arithmetic;

import org.platformlambda.core.annotations.SimplePlugin;

import java.util.Arrays;

@SimplePlugin
public class MultiplyNumbers extends SimpleNumberPlugin {

    @Override
    public String getName() {
        return "multiply";
    }

    @Override
    public Object calculate(Object... input) {
        return promoteInput(input)
                .reduce((l1, l2) -> l1 * l2)
                .orElseThrow(() -> new IllegalStateException("Could not multiply the input: " + Arrays.toString(input)));
    }

}
