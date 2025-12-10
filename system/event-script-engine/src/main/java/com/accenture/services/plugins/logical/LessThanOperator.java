package com.accenture.services.plugins.logical;

import com.accenture.utils.SimplePluginUtils;
import com.accenture.models.simplePlugin;
import com.accenture.models.PluginFunction;

@SimplePlugin
public class LessThanOperator implements PluginFunction {

    @Override
    public String getName() {
        return "lt";
    }

    @Override
    public Object calculate(Object... input) {
        if(input.length == 0){
            throw new IllegalArgumentException("Input is required to compare using 'Less Than'");
        }

        Long first = SimplePluginUtils.promoteNumber(input[0]);
        Long second = SimplePluginUtils.promoteNumber(input[1]);

        return first < second;
    }
}
