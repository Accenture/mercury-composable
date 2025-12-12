package com.accenture.services.plugins.logical;

import com.accenture.models.PluginFunction;
import com.accenture.models.SimplePlugin;

@SimplePlugin
public class IsNullOperator implements PluginFunction {

    @Override
    public String getName() {
        return "isNull";
    }

    @Override
    public Object calculate(Object... input) {
        if(input == null){
            return true;
        }
        else if(input.length == 1){
            return input[0] == null;
        }

        throw new IllegalArgumentException("Only one value is accepted");
    }
}
