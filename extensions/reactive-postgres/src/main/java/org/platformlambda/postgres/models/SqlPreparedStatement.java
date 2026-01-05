/*

    Copyright 2018-2026 Accenture Technology

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

package org.platformlambda.postgres.models;

import org.platformlambda.core.util.Utility;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the base class for db2-pool and pg-pool to extend for their specific use cases
 */
public abstract class SqlPreparedStatement {
    private static final String TYPE_BYTES = "bytes";
    private static final String TYPE_BIG_INTEGER = "big-integer";
    private static final String TYPE_BIG_DECIMAL = "big-decimal";
    private static final String TYPE_TIMESTAMP = "timestamp";
    private static final String TYPE_SQL_DATE = "sql-date";
    private static final String TYPE_SQL_TIME = "sql-time";
    private static final String TYPE_LOCAL_DATE = "local-date";
    private static final String TYPE_LOCAL_TIME = "local-time";
    private static final String TYPE_LOCAL_DATETIME = "local-datetime";
    private static final String TYPE_OFFSET_DATETIME = "offset-datetime";
    private static final String TYPE_UTC_DATE = "date";
    private static final String TYPE_BOOLEAN = "boolean";
    private static final String TYPE_STRING = "string";
    private static final String TYPE_NUMBER = "number";
    private static final String TYPE_BYTE = "byte";
    private static final String TYPE_SHORT = "short";
    private static final String TYPE_FLOAT = "float";
    private static final String TYPE_DOUBLE = "double";
    private static final String TYPE_LONG = "long";
    private static final String TYPE_INTEGER = "integer";
    protected static final String QUERY = "query";
    protected static final String UPDATE = "update";
    protected String statement;
    protected Map<Integer, Object> parameters = new HashMap<>();
    protected Map<String, Object> namedParams = new HashMap<>();
    protected Map<String, String> classMapping = new HashMap<>();
    protected Map<Integer, String> nullParams = new HashMap<>();
    protected Map<String, String> namedNulls = new HashMap<>();
    protected String type;
    protected int indexBase = 1;

    public String getType() {
        return type;
    }

    public int getIndexBase() {
        return indexBase;
    }

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public Map<Integer, Object> getParameters() {
        return parameters;
    }

    /**
     * This method is reserved for the serializer. Use bindParameter method instead.
     * @param parameters of indexes and values
     */
    public void setParameters(Map<Integer, Object> parameters) {
        this.parameters = parameters;
    }

    public Map<Integer, String> getNullParams() {
        return nullParams;
    }

    /**
     * This method is reserved for the serializer. Use bindNullParameter method instead.
     * @param nullParams of indexes and null classes
     */
    public void setNullParams(Map<Integer, String> nullParams) {
        this.nullParams = nullParams;
    }

    public Map<String, Object> getNamedParams() {
        return namedParams;
    }

    /**
     * This method is reserved for the serializer. Use bindParameter method instead.
     * @param namedParams of name and value
     */
    public void setNamedParams(Map<String, Object> namedParams) {
        this.namedParams = namedParams;
    }

    public Map<String, String> getNamedNulls() {
        return namedNulls;
    }

    /**
     * This method is reserved for the serializer. Use bindNullParameter method instead.
     * @param namedNulls of names and null classes
     */
    public void setNamedNulls(Map<String, String> namedNulls) {
        this.namedNulls = namedNulls;
    }

    /**
     * Bind a value to a parameter (as "?" in a SQL statement)
     * @param key is the index starting 1
     * @param value must be an object compatible with the corresponding column data type
     *              e.g. Date would map to TIMESTAMP
     */
    public void bindParameter(int key, Object value) {
        var dataType = getDataType(value);
        parameters.put(key, value instanceof byte[] bytes? Utility.getInstance().bytesToBase64(bytes) : value);
        classMapping.put(String.valueOf(key), dataType);
    }

    /**
     * Bind a value to a named parameter (as ":name" in a SQL statement)
     * @param name is the parameter name
     * @param value must be an object compatible with the corresponding column data type
     *              e.g. Date would map to TIMESTAMP
     */
    public void bindParameter(String name, Object value) {
        var dataType = getDataType(value);
        var normalized = value instanceof byte[] bytes? Utility.getInstance().bytesToBase64(bytes) : value;
        namedParams.put(name, normalized);
        classMapping.put(name, dataType);
    }

    /**
     * Bind a variable number of parameter values, append if current parameters are not empty
     * @param values to bind as parameters
     */
    public void bindParameters(Object... values) {
        bindParametersFrom(parameters.size() + indexBase, values);
    }

    /**
     * Bind a variable number of parameter values from a given index
     * (DB2 JDBC parameter index starts from 1 and PostGreSQL R2DBC parameter index starts from 0)
     *
     * @param start index
     * @param values to bind as parameters
     */
    public void bindParametersFrom(int start, Object... values) {
        if (start < 0) {
            throw new IllegalArgumentException("Start index must not be negative");
        }
        int n = start;
        for  (Object value : values) {
            bindParameter(n++, value);
        }
    }

    /**
     * Bind a null value to a parameter
     * @param key is the index starting 1
     * @param dataType is a class that is compatible with a table column
     */
    public void bindNullParameter(int key, Class<?> dataType) {
        nullParams.put(key, getDataType(dataType));
    }

    /**
     * Bind a null value to a named parameter
     * @param name is the parameter name
     * @param dataType is a class that is compatible with a table column
     */
    public void bindNullParameter(String name, Class<?> dataType) {
        namedNulls.put(name, getDataType(dataType));
    }

    /**
     * Reserved for system use
     *
     * @return mapping of parameter's index/name to simple class name
     */
    public Map<String, String> getClassMapping() {
        return classMapping;
    }

    public Object getOriginalParameter(int idx) {
        var value = parameters.get(idx);
        var dataType = classMapping.get(String.valueOf(idx));
        return restoreOriginalParameter(value, dataType);
    }

    public Object getOriginalParameter(String name) {
        var value = namedParams.get(name);
        var dataType = classMapping.get(name);
        return restoreOriginalParameter(value, dataType);
    }

    private Object restoreOriginalParameter(Object value, String dataType) {
        if (value instanceof String text) {
            if (TYPE_BYTES.equals(dataType)) {
                return Utility.getInstance().base64ToBytes(text);
            }
            if (TYPE_BIG_INTEGER.equals(dataType)) {
                return new BigInteger(text);
            }
            if (TYPE_BIG_DECIMAL.equals(dataType)) {
                return new BigDecimal(text);
            }
            if (TYPE_TIMESTAMP.equals(dataType)) {
                return Timestamp.valueOf(text);
            }
            if (TYPE_SQL_DATE.equals(dataType)) {
                return java.sql.Date.valueOf(text);
            }
            if (TYPE_SQL_TIME.equals(dataType)) {
                return java.sql.Time.valueOf(text);
            }
            return restoreOriginalTimestamp(text, dataType);
        }
        // string and number do not need special handling
        return value;
    }

    private Object restoreOriginalTimestamp(String text, String dataType) {
        if (TYPE_LOCAL_DATE.equals(dataType)) {
            return LocalDate.parse(text);
        }
        if (TYPE_LOCAL_TIME.equals(dataType)) {
            return LocalTime.parse(text);
        }
        if (TYPE_LOCAL_DATETIME.equals(dataType)) {
            return LocalDateTime.parse(text);
        }
        if (TYPE_OFFSET_DATETIME.equals(dataType)) {
            return OffsetDateTime.parse(text);
        }
        if (TYPE_UTC_DATE.equals(dataType)) {
            return Utility.getInstance().str2date(text);
        }
        return text;
    }

    public Class<?> getNullClass(int idx) {
        return str2class(nullParams.get(idx), String.valueOf(idx));
    }

    public Class<?> getNullClass(String name) {
        return str2class(namedNulls.get(name), name);
    }

    private Class<?> str2class(String clazz, String ptr) {
        return switch (clazz) {
            case TYPE_STRING -> String.class;
            case TYPE_BOOLEAN -> Boolean.class;
            case TYPE_BYTE -> Byte.class;
            case TYPE_SHORT -> Short.class;
            case TYPE_BYTES -> byte[].class;
            case TYPE_FLOAT -> Float.class;
            case TYPE_DOUBLE -> Double.class;
            case TYPE_INTEGER -> Integer.class;
            case TYPE_LONG -> Long.class;
            case TYPE_BIG_INTEGER -> BigInteger.class;
            case TYPE_BIG_DECIMAL -> BigDecimal.class;
            case TYPE_TIMESTAMP -> Timestamp.class;
            case TYPE_SQL_DATE -> java.sql.Date.class;
            case TYPE_SQL_TIME -> java.sql.Time.class;
            case TYPE_LOCAL_DATE -> LocalDate.class;
            case TYPE_LOCAL_TIME -> LocalTime.class;
            case TYPE_LOCAL_DATETIME -> LocalDateTime.class;
            case TYPE_OFFSET_DATETIME -> OffsetDateTime.class;
            case TYPE_UTC_DATE -> Date.class;
            case null -> throw new IllegalArgumentException("Null class not encoded in "+ptr);
            default -> throw new IllegalArgumentException("Unsupported data type: " + clazz);
        };
    }

    public int getNullSqlType(int idx) {
        return str2SqlType(nullParams.get(idx), String.valueOf(idx));
    }

    public int getNullSqlType(String name) {
        return str2SqlType(namedNulls.get(name), name);
    }

    private int str2SqlType(String clazz, String ptr) {
        return switch (clazz) {
            case TYPE_STRING -> Types.VARCHAR;
            case TYPE_BOOLEAN -> Types.BOOLEAN;
            case TYPE_BYTE, TYPE_SHORT -> Types.SMALLINT;
            case TYPE_BYTES -> Types.BLOB;
            case TYPE_FLOAT -> Types.FLOAT;
            case TYPE_DOUBLE -> Types.DOUBLE;
            case TYPE_INTEGER -> Types.INTEGER;
            case TYPE_LONG, TYPE_BIG_INTEGER -> Types.BIGINT;
            case TYPE_BIG_DECIMAL -> Types.DECIMAL;
            case TYPE_TIMESTAMP, TYPE_LOCAL_DATETIME -> Types.TIMESTAMP;
            case TYPE_SQL_DATE, TYPE_LOCAL_DATE -> Types.DATE;
            case TYPE_SQL_TIME, TYPE_LOCAL_TIME -> Types.TIME;
            case TYPE_OFFSET_DATETIME, TYPE_UTC_DATE -> Types.TIMESTAMP_WITH_TIMEZONE;
            case null -> throw new IllegalArgumentException("Null class not encoded in "+ptr);
            default -> throw new IllegalArgumentException("Unsupported data type: " + clazz);
        };
    }

    private String getDataType(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Parameter value must not be null");
        }
        if (value instanceof Byte || value instanceof Short ||
                value instanceof Float || value instanceof Double ||
                value instanceof Integer || value instanceof Long) {
            return TYPE_NUMBER;
        }
        return switch (value) {
            case BigInteger ignored -> TYPE_BIG_INTEGER;
            case BigDecimal ignored -> TYPE_BIG_DECIMAL;
            case Timestamp ignored -> TYPE_TIMESTAMP;
            case java.sql.Date ignored -> TYPE_SQL_DATE;
            case java.sql.Time ignored -> TYPE_SQL_TIME;
            case LocalDate ignored -> TYPE_LOCAL_DATE;
            case LocalTime ignored -> TYPE_LOCAL_TIME;
            case LocalDateTime ignored -> TYPE_LOCAL_DATETIME;
            case OffsetDateTime ignored -> TYPE_OFFSET_DATETIME;
            case Date ignored -> TYPE_UTC_DATE;
            case String ignored -> TYPE_STRING;
            case Boolean ignored -> TYPE_BOOLEAN;
            case byte[] ignored -> TYPE_BYTES;
            default -> throw new IllegalArgumentException("Unsupported data type: " + value.getClass().getName());
        };
    }

    private String getDataType(Class<?> clazz) {
        if (clazz == String.class) {
            return TYPE_STRING;
        }
        if (clazz == Boolean.class) {
            return TYPE_BOOLEAN;
        }
        if (clazz == byte[].class) {
            return TYPE_BYTES;
        }
        if (clazz == BigInteger.class) {
            return TYPE_BIG_INTEGER;
        }
        if (clazz == BigDecimal.class) {
            return TYPE_BIG_DECIMAL;
        }
        if (clazz == Timestamp.class) {
            return TYPE_TIMESTAMP;
        }
        if (clazz == java.sql.Date.class) {
            return TYPE_SQL_DATE;
        }
        if (clazz == java.sql.Time.class) {
            return TYPE_SQL_TIME;
        }
        if (clazz == LocalDate.class) {
            return TYPE_LOCAL_DATE;
        }
        if (clazz == LocalTime.class) {
            return TYPE_LOCAL_TIME;
        }
        if (clazz == LocalDateTime.class) {
            return TYPE_LOCAL_DATETIME;
        }
        if (clazz == OffsetDateTime.class) {
            return TYPE_OFFSET_DATETIME;
        }
        if (clazz == Date.class) {
            return TYPE_UTC_DATE;
        }
        if (clazz == Byte.class || clazz == Short.class ||
                clazz == Float.class || clazz == Double.class || clazz == Integer.class || clazz == Long.class) {
            return clazz.getSimpleName().toLowerCase();
        }
        throw new IllegalArgumentException("Unsupported data type: " + clazz);
    }
}
