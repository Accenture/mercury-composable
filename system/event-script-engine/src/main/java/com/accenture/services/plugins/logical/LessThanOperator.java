package com.accenture.services.plugins.logical;

import com.accenture.utils.SimplePluginUtils;
import org.platformlambda.core.annotations.SimplePlugin;
import org.platformlambda.core.models.PluginFunction;

@SimplePlugin
public class LessThanOperator implements PluginFunction {

    @Override
    public String getName() {
        return "lt";
    }

    @Override
    public Object calculate(Object... input) {
        if(input.length == 0){
            throw new IllegalArgumentException("Input is required to compare using 'Greater Than'");
        }

        Long first = SimplePluginUtils.promoteNumber(input[0]);
        Long second = SimplePluginUtils.promoteNumber(input[1]);

        return first < second;
    }
}
