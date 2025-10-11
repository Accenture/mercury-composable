package com.accenture.services.plugins.types;

import com.accenture.services.plugins.arithmetic.SimpleNumberPlugin;
import com.accenture.utils.TypeConversionUtils;
import org.platformlambda.core.annotations.SimplePlugin;
import org.platformlambda.core.models.PluginFunction;

import java.util.Arrays;

@SimplePlugin
public class TextConversion implements PluginFunction {

    @Override
    public String getName() {
        return "text";
    }

    @Override
    public Object calculate(Object... input) {
        if(input.length == 1){
            return TypeConversionUtils.getTextValue(input[0]);
        }

        return Arrays.stream(input)
                .map(TypeConversionUtils::getTextValue)
                .toList();
    }
}
