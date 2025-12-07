package com.accenture.services.plugins.logical;

import org.platformlambda.core.annotations.SimplePlugin;
import org.platformlambda.core.models.PluginFunction;

@SimplePlugin
public class IsNullOperator implements PluginFunction {

    @Override
    public String getName() {
        return "isNull";
    }

    @Override
    public Object calculate(Object... input) {
        if(input.length == 1){
            return input[0] == null;
        }

        return false;
    }
}
