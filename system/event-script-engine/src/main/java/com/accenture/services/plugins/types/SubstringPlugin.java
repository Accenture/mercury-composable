package com.accenture.services.plugins.types;

import com.accenture.utils.TypeConversionUtils;
import org.platformlambda.core.annotations.SimplePlugin;
import org.platformlambda.core.models.PluginFunction;

import java.util.Arrays;

@SimplePlugin
public class SubstringPlugin implements PluginFunction {

    @Override
    public String getName() {
        return "substring";
    }

    @Override
    public Object calculate(Object... input) {
        String value = TypeConversionUtils.getTextValue(input[0]);

        int start = (input.length > 1)?  TypeConversionUtils.convertInteger(input[1]) : -1;
        int end = (input.length > 2)?  TypeConversionUtils.convertInteger(input[2]) : -1;

        if(isOutOfBounds(value, start, end)){
            throw new IllegalArgumentException("Substring indexes are out of bounds: [" + start + ", " + end + "]");
        }
        if(start >= 0 && end >= 0){ // Start and end indexes are in bound
            return value.substring(start, end);
        }
        else if(start >= 0 && start < value.length()){ //Start index is in bound
            return value.substring(start);
        }

        return value;
    }

    public boolean isOutOfBounds(String value, int start, int end){
        return (end >= 0 && end > value.length()) || // End is out of bounds
                (start >= 0 && start > value.length()) || // Start is out of bounds
                (start > end && start >= 0 && end >= 0); // start and end are flipped
    }
}
