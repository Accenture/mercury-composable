package com.accenture.services.plugins.logical;

import com.accenture.models.SimplePlugin;
import com.accenture.models.PluginFunction;

@SimplePlugin
public class IsNotNullOperator implements PluginFunction {

    @Override
    public String getName() {
        return "notNull";
    }

    @Override
    public Object calculate(Object... input) {
        if(input.length == 1){
            return input[0] != null;
        }

        throw new IllegalArgumentException("Only one value is accepted");
    }
}
