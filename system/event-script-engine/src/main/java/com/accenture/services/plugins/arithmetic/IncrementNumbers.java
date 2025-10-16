package com.accenture.services.plugins.arithmetic;

import org.platformlambda.core.annotations.SimplePlugin;

import java.util.Arrays;

@SimplePlugin
public class IncrementNumbers extends SimpleNumberPlugin {

    @Override
    public String getName() {
        return "increment";
    }

    @Override
    public Object calculate(Object... input) {
        return promoteInput(input)
                .map(l -> l + 1)
                .toList();
    }
}
