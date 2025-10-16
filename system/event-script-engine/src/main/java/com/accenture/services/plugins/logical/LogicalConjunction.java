package com.accenture.services.plugins.logical;

import com.accenture.utils.TypeConversionUtils;
import org.platformlambda.core.annotations.SimplePlugin;
import org.platformlambda.core.models.PluginFunction;

import java.util.Arrays;

@SimplePlugin
public class LogicalConjunction implements PluginFunction {

    @Override
    public String getName() {
        return "and";
    }

    @Override
    public Object calculate(Object... input) {
        if(input.length == 0){
            return false;
        }

        return Arrays.stream(input)
                .allMatch(TypeConversionUtils::convertBoolean);
    }
}
