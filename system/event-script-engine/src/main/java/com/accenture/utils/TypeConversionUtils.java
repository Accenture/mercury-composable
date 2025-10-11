package com.accenture.utils;

import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.Utility;

import java.util.List;
import java.util.Map;

public class TypeConversionUtils {

    static final Utility util = Utility.getInstance();


    public static String getUUID(){
        return util.getUuid4();
    }

    public static Boolean convertBoolean(Object input){
        return switch (input){
            case Boolean i -> i;
            case String s -> Boolean.parseBoolean(s);
            default -> throw new IllegalArgumentException("Cannot convert input to boolean: " + input);
        };
    }

    public static Double convertDouble(Object input){
        return switch (input){
            case Double i -> i;
            case String s -> Double.parseDouble(s);
            default -> throw new IllegalArgumentException("Cannot convert input to double: " + input);
        };
    }

    public static Integer convertInteger(Object input){
        return switch (input){
            case Integer i -> i;
            case String s -> Integer.parseInt(s);
            default -> throw new IllegalArgumentException("Cannot convert input to integer: " + input);
        };
    }

    public static Long convertLong(Object input){
        return switch (input){
            case Long i -> i;
            case String s -> Long.parseLong(s);
            default -> throw new IllegalArgumentException("Cannot convert input to long: " + input);
        };
    }

    public static Float convertFloat(Object input){
        return switch (input){
            case Float i -> i;
            case String s -> Float.parseFloat(s);
            default -> throw new IllegalArgumentException("Cannot convert input to float: " + input);
        };
    }

    public static String getTextValue(Object value) {
        return switch (value) {
            case String str -> str;
            case byte[] b -> util.getUTF(b);
            case Map<?, ?> map -> SimpleMapper.getInstance().getMapper().writeValueAsString(map);
            default -> String.valueOf(value);
        };
    }

    public static byte[] getBinaryValue(Object value) {
        return switch (value) {
            case byte[] b -> b;
            case String str -> util.getUTF(str);
            case Map<?, ?> map -> SimpleMapper.getInstance().getMapper().writeValueAsBytes(map);
            default -> util.getUTF(String.valueOf(value));
        };
    }

    public static int getLength(Object value) {
        return switch (value) {
            case null -> 0;
            case byte[] b -> b.length;
            case String str -> str.length();
            case List<?> item -> item.size();
            default -> String.valueOf(value).length();
        };
    }

    public static Object getB64(Object value) {
        if (value instanceof byte[] b) {
            return util.bytesToBase64(b);
        } else if (value instanceof String str) {
            try {
                return util.base64ToBytes(str);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("invalid base64 text");
            }
        }
        return value;
    }


}
