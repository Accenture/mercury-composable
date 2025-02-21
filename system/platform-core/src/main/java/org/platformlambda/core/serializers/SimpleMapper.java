/*

    Copyright 2018-2025 Accenture Technology

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */

package org.platformlambda.core.serializers;

import com.google.gson.*;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class SimpleMapper {
    private static final String SNAKE_CASE_SERIALIZATION = "snake.case.serialization";
    private final SimpleObjectMapper mapper;
    private final SimpleObjectMapper snakeMapper;
    private final SimpleObjectMapper camelMapper;
    private final Gson snakeGson;
    private final Gson camelGson;
    private static final Gson compactGson = new GsonBuilder().disableHtmlEscaping()
                                            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();
    private static final Gson prettyGson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting()
                                            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();
    private static final SimpleMapper instance = new SimpleMapper();

    private SimpleMapper() {
        // Camel or snake case
        AppConfigReader config = AppConfigReader.getInstance();
        boolean snake = "true".equals(config.getProperty(SNAKE_CASE_SERIALIZATION, "true"));
        this.mapper = new SimpleObjectMapper(preconfigureGson(snake));
        this.snakeGson = preconfigureGson(true);
        this.camelGson = preconfigureGson(false);
        this.snakeMapper = new SimpleObjectMapper(this.snakeGson);
        this.camelMapper = new SimpleObjectMapper(this.camelGson);
    }

    /**
     * This Gson instance may be used for writing JSON string in application log.
     *
     * @return gson instance
     */
    public Gson getCompactGson() {
        return compactGson;
    }

    /**
     * This Gson instance may be used for pretty printing.
     *
     * @return gson instance
     */
    public Gson getPrettyGson() {
        return prettyGson;
    }

    /**
     * If you prefer to use the Gson serializer API directly,
     * you may get this shared Gson instance that has been pre-configured.
     * <p>
     * Please do not reconfigure this gson instance because it will break
     * the serialization behavior. If you need to customize your own Gson,
     * create a new builder instance.
     *
     * @param snake case (true or false)
     * @return preconfigured gson
     */
    public Gson getJson(boolean snake) {
        return snake? snakeGson : camelGson;
    }

    private Gson preconfigureGson(boolean snake) {
        // configure Gson engine
        GsonBuilder builder = new GsonBuilder();
        // avoid equal sign to become 003d unicode
        builder.disableHtmlEscaping();
        // UTC date
        builder.registerTypeAdapter(Date.class, new UtcSerializer());
        builder.registerTypeAdapter(Date.class, new UtcDeserializer());
        // local datetime, date and time
        builder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer());
        builder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer());
        builder.registerTypeAdapter(LocalDate.class, new LocalDateSerializer());
        builder.registerTypeAdapter(LocalDate.class, new LocalDateDeserializer());
        builder.registerTypeAdapter(LocalTime.class, new LocalTimeSerializer());
        builder.registerTypeAdapter(LocalTime.class, new LocalTimeDeserializer());
        // SQL timestamp, date and time
        builder.registerTypeAdapter(java.sql.Timestamp.class, new SqlTimestampSerializer());
        builder.registerTypeAdapter(java.sql.Timestamp.class, new SqlTimestampDeserializer());
        builder.registerTypeAdapter(java.sql.Date.class, new SqlDateSerializer());
        builder.registerTypeAdapter(java.sql.Date.class, new SqlDateDeserializer());
        builder.registerTypeAdapter(java.sql.Time.class, new SqlTimeSerializer());
        builder.registerTypeAdapter(java.sql.Time.class, new SqlTimeDeserializer());
        // Big integer and decimal
        builder.registerTypeAdapter(BigInteger.class, new BigIntegerSerializer());
        builder.registerTypeAdapter(BigInteger.class, new BigIntegerDeserializer());
        builder.registerTypeAdapter(BigDecimal.class, new BigDecimalSerializer());
        builder.registerTypeAdapter(BigDecimal.class, new BigDecimalDeserializer());
        // tell GSON not to use double for all numbers and treat them correctly
        builder.setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE);
        // Indent JSON output
        builder.setPrettyPrinting();
        // Camel or snake case
        if (snake) {
            builder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        }
        return builder.create();
    }

    public static SimpleMapper getInstance() {
        return instance;
    }

    /**
     * Get default object mapper
     * <p>
     * @return object mapper
     */
    public SimpleObjectMapper getMapper() {
        return mapper;
    }

    /**
     * Get snake_case object mapper
     * <p>
     * @return object mapper
     */
    public SimpleObjectMapper getSnakeCaseMapper() {
        return snakeMapper;
    }

    /**
     * Get camelCase object mapper
     * <p>
     * @return object mapper
     */
    public SimpleObjectMapper getCamelCaseMapper() {
        return camelMapper;
    }

    /// Custom serializers ///

    private static class UtcSerializer implements JsonSerializer<Date> {

        @Override
        public JsonElement serialize(Date date, Type type, JsonSerializationContext context) {
            return new JsonPrimitive(Utility.getInstance().date2str(date));
        }
    }

    private static class UtcDeserializer implements JsonDeserializer<Date> {

        @Override
        public Date deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            return Utility.getInstance().str2date(json.getAsString());
        }
    }

    private static class LocalTimeSerializer implements JsonSerializer<LocalTime> {

        @Override
        public JsonElement serialize(LocalTime time, Type type, JsonSerializationContext context) {
            return new JsonPrimitive(time.toString());
        }
    }

    private static class LocalTimeDeserializer implements JsonDeserializer<LocalTime> {

        @Override
        public LocalTime deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            return LocalTime.parse(json.getAsString());
        }
    }

    private static class LocalDateSerializer implements JsonSerializer<LocalDate> {

        @Override
        public JsonElement serialize(LocalDate date, Type type, JsonSerializationContext context) {
            return new JsonPrimitive(date.toString());
        }
    }

    private static class LocalDateDeserializer implements JsonDeserializer<LocalDate> {

        @Override
        public LocalDate deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            return Utility.getInstance().str2localDate(json.getAsString());
        }
    }

    private static class LocalDateTimeSerializer implements JsonSerializer<LocalDateTime> {

        @Override
        public JsonElement serialize(LocalDateTime date, Type type, JsonSerializationContext context) {
            return new JsonPrimitive(date.toString().replace('T', ' '));
        }
    }

    private static class LocalDateTimeDeserializer implements JsonDeserializer<LocalDateTime> {

        @Override
        public LocalDateTime deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            return Utility.getInstance().str2LocalDateTime(json.getAsString());
        }
    }

    private static class SqlTimestampSerializer implements JsonSerializer<java.sql.Timestamp> {

        @Override
        public JsonElement serialize(java.sql.Timestamp date, Type type, JsonSerializationContext context) {
            return new JsonPrimitive(date.toString());
        }
    }

    private static class SqlTimestampDeserializer implements JsonDeserializer<java.sql.Timestamp> {

        @Override
        public java.sql.Timestamp deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            try {
                return java.sql.Timestamp.valueOf(json.getAsString());
            } catch (IllegalArgumentException e) {
                // parse input as ISO-8601
                Date date = Utility.getInstance().str2date(json.getAsString());
                return new java.sql.Timestamp(date.getTime());
            }
        }
    }

    private static class SqlDateSerializer implements JsonSerializer<java.sql.Date> {

        @Override
        public JsonElement serialize(java.sql.Date date, Type type, JsonSerializationContext context) {
            return new JsonPrimitive(date.toString());
        }
    }

    private static class SqlDateDeserializer implements JsonDeserializer<java.sql.Date> {

        @Override
        public java.sql.Date deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            try {
                return java.sql.Date.valueOf(json.getAsString());
            } catch (IllegalArgumentException e) {
                // parse input as ISO-8601
                Date date = Utility.getInstance().str2date(json.getAsString());
                return new java.sql.Date(date.getTime());
            }
        }
    }

    private static class SqlTimeSerializer implements JsonSerializer<java.sql.Time> {

        @Override
        public JsonElement serialize(java.sql.Time time, Type type, JsonSerializationContext context) {
            return new JsonPrimitive(time.toString());
        }
    }

    private static class SqlTimeDeserializer implements JsonDeserializer<java.sql.Time> {

        @Override
        public java.sql.Time deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            return java.sql.Time.valueOf(json.getAsString());
        }
    }

    private static class BigIntegerSerializer implements JsonSerializer<BigInteger> {

        @Override
        public JsonElement serialize(BigInteger number, Type type, JsonSerializationContext context) {
            return new JsonPrimitive(number.toString());
        }
    }

    private static class BigIntegerDeserializer implements JsonDeserializer<BigInteger> {

        @Override
        public BigInteger deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            return new BigInteger(json.getAsString());
        }
    }

    private class BigDecimalSerializer implements JsonSerializer<BigDecimal> {

        @Override
        public JsonElement serialize(BigDecimal number, Type type, JsonSerializationContext context) {
            // we want to avoid scientific notation when sending result to the browser
            String result = number.toPlainString();
            return new JsonPrimitive(isZero(result)? "0" : result);
        }
    }

    private static class BigDecimalDeserializer implements JsonDeserializer<BigDecimal> {

        @Override
        public BigDecimal deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            return new BigDecimal(json.getAsString());
        }
    }

    public boolean isZero(BigDecimal number) {
        return isZero(number.toPlainString());
    }

    /**
     * Check if the BigDecimal is zero
     *
     * @param number in BigDecimal
     * @return true if zero
     */
    public boolean isZero(String number) {
        if (number != null && !number.isEmpty()) {
            if (number.startsWith("0E")) {
                /*
                 * scientific notation of zero
                 * just in case if caller uses the BigDecimal's toString method
                 */
                return true;
            } else {
                /*
                 * string representation of zero
                 */
                for (int i = 0; i < number.length(); i++) {
                    if (number.charAt(i) == '0' || number.charAt(i) == '+' ||
                            number.charAt(i) == '-' || number.charAt(i) == '.') continue;
                    return false;
                }
            }
        }
        return true;
    }
}
