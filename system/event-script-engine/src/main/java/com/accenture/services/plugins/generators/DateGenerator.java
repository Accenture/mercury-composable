package com.accenture.services.plugins.generators;

import com.accenture.models.simplePlugin;
import com.accenture.models.PluginFunction;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@SimplePlugin
public class DateGenerator implements PluginFunction {

    @Override
    public String getName() {
        return "dateTime";
    }

    @Override
    public Object calculate(Object... input) {
        DateTimeFormatter dateFormatter = (input.length == 1)? DateTimeFormatter.ofPattern(String.valueOf(input[0])) : DateTimeFormatter.ISO_DATE_TIME;
        ZoneId zone = (input.length == 2)? ZoneId.of(String.valueOf(input[1])) : ZoneId.systemDefault();


        return ZonedDateTime.now(zone).format(dateFormatter);
    }
}
