package com.accenture.services.plugins.types;

import com.accenture.utils.TypeConversionUtils;
import org.platformlambda.core.annotations.SimplePlugin;
import org.platformlambda.core.models.PluginFunction;

import java.util.Arrays;
import java.util.List;

@SimplePlugin
public class BooleanConversion implements PluginFunction {

    @Override
    public String getName() {
        return "boolean";
    }

    @Override
    public Object calculate(Object... input) {
        boolean allBooleans = Arrays.stream(input).allMatch(TypeConversionUtils::isBoolean);

        return (allBooleans)? simpleBooleanConversion(input) : modelBooleanConversion(input);
    }

    protected Object simpleBooleanConversion(Object... input){
        var output = Arrays.stream(input)
                .map(TypeConversionUtils::convertBoolean)
                .toList();

        return (output.size() == 1) ? output.getFirst() : output;
    }

    protected Object modelBooleanConversion(Object... input){
        if(input.length != 2){
            throw new IllegalArgumentException("Cannot get boolean value using pos/neg matching, got: " +
                    Arrays.toString(input));
        }

        Object model = input[0];
        Object comparison = input[1];

        if(comparison instanceof String command){
            return TypeConversionUtils.getBooleanValue(model, command);
        }
        else{
            throw new IllegalArgumentException("Second paramter should be a string when using pos/neg matching, got: " +
                    Arrays.toString(input));
        }
    }
}
