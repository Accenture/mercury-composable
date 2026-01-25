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

package org.platformlambda.db;

import org.platformlambda.core.util.Utility;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
    protected Map<Integer, Object> positionParams = new HashMap<>();
    protected Map<String, Object> namedParams = new HashMap<>();
    protected Map<String, String> classMapping = new HashMap<>();
    protected Map<Integer, String> nullParams = new HashMap<>();
    protected Map<String, String> namedNulls = new HashMap<>();
    protected String type;
    protected int indexBase = 1;
    // numberedIndex uses the "$n" syntax while regular index uses the "?" syntax.
    // e.g. PostGreSQL R2DBC uses the "$n" syntax and DB2 JDBC uses the "?" syntax.
    // This value is used when doing conversion from named to index parameters.
    protected boolean numberedIndex = false;
    protected boolean supportNamedParameters = false;
    // named parameter conversion is allowed only once
    private boolean converted = false;
    private boolean bindComplete = false;

    public String getType() {
        return type;
    }

    public int getIndexBase() {
        return indexBase;
    }

    public boolean isNumberedIndex() {
        return numberedIndex;
    }

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public Map<Integer, Object> getParameters() {
        return positionParams;
    }

    /**
     * This method is reserved for the serializer. Use bindParameter method instead.
     * @param parameters of indexes and values
     */
    public void setParameters(Map<Integer, Object> parameters) {
        this.positionParams = parameters;
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
     * Bind named parameters
     * (This must be the last bind method if you have bound individual parameters earlier)
     *
     * @param params as name-value pairs
     */
    public void bindNamedParameters(Map<String, Object> params) {
        if (bindComplete) {
            throw new IllegalArgumentException("Parameters have been bound");
        } else {
            bindComplete = true;
            // named parameters
            var provided = new HashMap<>(params);
            // pre-processing of list values
            var listParams = new HashMap<String, String>();
            for (var entry : provided.entrySet()) {
                if (entry.getValue() instanceof List<?> values) {
                    listParams.put(entry.getKey(), list2str(values));
                }
            }
            // update the SQL statement with list values, if any
            listParams.keySet().forEach(name -> {
                provided.remove(name);
                setStatement(getStatement().replace(":" + name, listParams.get(name)));
            });
            for (var entry : provided.entrySet()) {
                bindParameter(entry.getKey(), entry.getValue());
            }
            if (!supportNamedParameters) {
                convertNamedParamsToIndex();
            }
        }
    }

    /**
     * Bind a variable number of parameter values, append if current parameters are not empty
     * (This must be the last bind method if you have bound individual parameters earlier)
     *
     * @param values to bind as parameters
     */
    public void bindParameters(Object... values) {
        if (values.length == 1 && values[0] instanceof Map<?, ?> provided) {
            var params = new HashMap<String, Object>();
            provided.keySet().forEach(name -> params.put(String.valueOf(name), provided.get(name)));
            bindNamedParameters(params);
        } else {
            var listPos = getPositionsForListValues(values);
            // Parameters contain list values?
            if (listPos.contains(true)) {
                // convert position parameters into named parameters
                int n = 1;
                Map<String, Object> params = new HashMap<>();
                for (var v : values) {
                    params.put("p"+n, v);
                    n++;
                }
                // update SQL statement
                if (numberedIndex) {
                    transformNumberedIndex(listPos);
                } else {
                    transformQuestionMarkIndex(listPos);
                }
                bindNamedParameters(params);
            } else {
                bindParametersFrom(positionParams.size() + indexBase, values);
            }
        }
    }

    private void transformNumberedIndex(List<Boolean> listPos) {
        for (int i = listPos.size(); i > 0; i--) {
            var paramIndex = "$" + i;
            if (statement.contains(paramIndex)) {
                statement = statement.replace(paramIndex, ":p" + i);
            } else {
                throw new IllegalArgumentException("Parameter " + paramIndex + " not found");
            }
        }
    }

    private void transformQuestionMarkIndex(List<Boolean> listPos) {
        for (int i = 0; i < listPos.size(); i++) {
            var seq = i + 1;
            var q = statement.indexOf('?');
            if (q == -1) {
                throw new IllegalArgumentException("Parameter " + seq + " not found");
            } else {
                statement = statement.substring(0, q) + (":p" + seq) + statement.substring(q + 1);
            }
        }
    }

    private List<Boolean> getPositionsForListValues(Object... values) {
        int n = 0;
        var result = new ArrayList<Boolean>();
        for (Object value : values) {
            result.add(n++, value instanceof List);
        }
        return result;
    }

    /**
     * Bind a value to a parameter (as "?" in a SQL statement)
     * @param key is the index starting 1
     * @param value must be an object compatible with the corresponding column data type
     *              e.g. Date would map to TIMESTAMP
     */
    public void bindParameter(int key, Object value) {
        if (value == null) {
            // best effort to bind parameter value as null
            bindNullParameter(key, String.class);
        } else {
            var dataType = getDataType(value);
            positionParams.put(key, value instanceof byte[] bytes ? Utility.getInstance().bytesToBase64(bytes) : value);
            classMapping.put(String.valueOf(key), dataType);
        }
    }

    /**
     * Bind a value to a named parameter (as ":name" in a SQL statement)
     * @param name is the parameter name
     * @param value must be an object compatible with the corresponding column data type
     *              e.g. Date would map to TIMESTAMP
     */
    public void bindParameter(String name, Object value) {
        if (value == null) {
            // best effort to bind parameter value as null
            bindNullParameter(name, String.class);
        } else {
            var dataType = getDataType(value);
            var normalized = value instanceof byte[] bytes ? Utility.getInstance().bytesToBase64(bytes) : value;
            namedParams.put(name, normalized);
            classMapping.put(name, dataType);
        }
    }

    /**
     * Bind a variable number of parameter values from a given index
     * 1. DB2 JDBC parameter index starts from 1 and PostGreSQL R2DBC parameter index starts from 0
     * 2. This must be the last bind method if you have bound individual parameters using bindParameter(number, value)
     *    method earlier
     *
     * @param start index
     * @param values to bind as parameters
     */
    public void bindParametersFrom(int start, Object... values) {
        if (bindComplete) {
            throw new IllegalArgumentException("Parameters have been bound");
        } else {
            bindComplete = true;
            if (start < 0) {
                throw new IllegalArgumentException("Start index must not be negative");
            }
            int n = start;
            for (Object value : values) {
                bindParameter(n++, value);
            }
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
        var value = positionParams.get(idx);
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

    /**
     * This method enables use of named parameters even when the underlying DB library does not do so.
     * (It converts named parameters into indexed SQL statement and parameters)
     * <p>
     * If you are using the bindParameters method, it will invoke this automatically.
     * This is required after you bind individual parameters manually.
     */
    public void convertNamedParamsToIndex() {
        if (!converted) {
            converted = true;
            // find all named parameters from SQL statement
            var text = getStatement();
            var labelFound = new AtomicBoolean(false);
            var paramList = new ArrayList<String>();
            var revised = new StringBuilder();
            var nIndex = new AtomicInteger(1);
            var start = new AtomicInteger(0);
            var word = new StringBuilder();
            for (int i=0; i < text.length(); i++) {
                var c = text.charAt(i);
                if (c == ':') {
                    scanForWord(text, i, start, nIndex, revised, labelFound);
                } else {
                    saveWord(start, i, paramList, c, word, labelFound);
                }
            }
            if (!word.isEmpty()) {
                paramList.add(word.toString());
            }
            if (start.get() < text.length()) {
                revised.append(text.substring(start.get()));
            }
            setStatement(String.valueOf(revised));
            refactorParams(paramList);
        }
    }

    private void refactorParams(List<String> paramList) {
        int idx = getIndexBase();
        for (var name: paramList) {
            var o = getOriginalParameter(name);
            if (o != null) {
                bindParameter(idx++, o);
            } else {
                try {
                    var clazz = getNullClass(name);
                    if (clazz != null) {
                        bindNullParameter(idx++, clazz);
                    }
                } catch (IllegalArgumentException e) {
                    // ignore exception
                }
            }
        }
        namedParams.clear();
        namedNulls.clear();
        var toBeRemoved = new ArrayList<String>();
        var util = Utility.getInstance();
        for (var name: classMapping.keySet()) {
            if (!util.isDigits(name)) {
                toBeRemoved.add(name);
            }
        }
        for (var name: toBeRemoved) {
            classMapping.remove(name);
        }
    }

    private void scanForWord(String text, int i, AtomicInteger start, AtomicInteger nIndex,
                             StringBuilder revised, AtomicBoolean labelFound) {
        if (labelFound.get()) {
            throw new IllegalArgumentException("Invalid named parameter syntax");
        } else {
            labelFound.set(true);
            if (i > 0) {
                revised.append(text, start.get(), i);
                if (isNumberedIndex()) {
                    revised.append('$').append(nIndex.getAndAdd(1));
                } else {
                    revised.append('?');
                }
                start.set(i + 1);
            }
        }
    }

    private void saveWord(AtomicInteger start, int i, List<String> paramList,
                          char c, StringBuilder word, AtomicBoolean labelFound) {
        if (labelFound.get()) {
            if (isWord(c)) {
                word.append(c);
                start.set(i + 1);
            } else {
                paramList.add(word.toString());
                word.setLength(0);
                labelFound.set(false);
            }
        }
    }

    private boolean isWord(char c) {
        return ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_');
    }

    private String list2str(List<?> values) {
        var type1 = false;
        var type2 = false;
        var sb = new StringBuilder();
        for (Object v : values) {
            if (v instanceof Number) {
                sb.append(v);
                type1 = true;
            } else {
                var normalized = String.valueOf(v).replace("'", "''");
                sb.append('\'').append(normalized).append('\'');
                type2 = true;
            }
            sb.append(", ");
        }
        if (type1 && type2) {
            throw new IllegalArgumentException("List parameter must be of the same type");
        }
        return sb.isEmpty()? "" : sb.substring(0, sb.length() - 2);
    }
}
