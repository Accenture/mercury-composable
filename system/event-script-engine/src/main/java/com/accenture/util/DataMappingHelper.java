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

package com.accenture.util;

import com.accenture.automation.SimplePluginLoader;
import com.accenture.models.PluginFunction;
import com.accenture.models.SimpleFileDescriptor;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

public class DataMappingHelper {
    private static final Logger log = LoggerFactory.getLogger(DataMappingHelper.class);
    private static final Utility util = Utility.getInstance();
    private static final String SIMPLE_PLUGIN_PREFIX = "f:";
    private static final String MODEL = "model";
    private static final String MODEL_NAMESPACE = "model.";
    private static final String PARENT = "parent";
    private static final String ROOT = "root";
    private static final String MAP_TO = "->";
    private static final String INPUT = "input";
    private static final String HEADER = "header";
    private static final String INPUT_NAMESPACE = "input.";
    private static final String ERROR_NAMESPACE = "error.";
    private static final String OUTPUT_NAMESPACE = "output.";
    private static final String EXT_NAMESPACE = "ext:";
    private static final String RESULT_NAMESPACE = "result.";
    private static final String HEADER_NAMESPACE = "header.";
    private static final String JSON_PATH_TYPE = "$";
    private static final String TEXT_TYPE = "text(";
    private static final String INTEGER_TYPE = "int(";
    private static final String LONG_TYPE = "long(";
    private static final String FLOAT_TYPE = "float(";
    private static final String DOUBLE_TYPE = "double(";
    private static final String BOOLEAN_TYPE = "boolean(";
    private static final String CLASSPATH_TYPE = "classpath(";
    private static final String FILE_TYPE = "file(";
    private static final String MAP_TYPE = "map(";
    private static final String DATA_TYPE = "datatype";
    private static final String CLOSE_BRACKET = ")";
    private static final String RESULT = "result";
    private static final String STATUS = "status";
    private static final String DECISION = "decision";
    private static final String SUBSTRING_TYPE = "substring(";
    private static final String CONCAT_TYPE = "concat(";
    private static final String AND_TYPE = "and(";
    private static final String OR_TYPE = "or(";
    private static final String TEXT_SUFFIX = "text";
    private static final String BINARY_SUFFIX = "binary";
    private static final String B64_SUFFIX = "b64";
    private static final String INTEGER_SUFFIX = "int";
    private static final String LONG_SUFFIX = "long";
    private static final String FLOAT_SUFFIX = "float";
    private static final String DOUBLE_SUFFIX = "double";
    private static final String BOOLEAN_SUFFIX = "boolean";
    private static final String UUID_SUFFIX = "uuid";
    private static final String LENGTH_SUFFIX = "length";
    private static final String NEGATE_SUFFIX = "!";
    private static final String TRUE = "true";
    private static final String PLUGGABLE_FUNCTION_REGEX = "f:(?<pluginName>.+)\\(.*\\)";
    private static final Pattern PLUGGABLE_FUNCTION_PATTERN = Pattern.compile(PLUGGABLE_FUNCTION_REGEX);
    private static final DataMappingHelper INSTANCE = new DataMappingHelper();

    private enum OPERATION {
        SIMPLE_COMMAND,
        SUBSTRING_COMMAND,
        CONCAT_COMMAND,
        AND_COMMAND,
        OR_COMMAND,
        BOOLEAN_COMMAND
    }

    private DataMappingHelper() {
        // singleton
    }

    public static DataMappingHelper getInstance() {
        return INSTANCE;
    }

    public boolean validInput(String input) {
        int sep = input.lastIndexOf(MAP_TO);
        if (sep > 0) {
            String lhs = input.substring(0, sep).trim();
            String rhs = input.substring(sep+2).trim();
            if (isPluggableFunction(rhs)) {
                return false;
            } else if (isPluggableFunction(lhs)) {
                return isValidPluggableFunction(lhs);
            } else if (validModel(lhs) && validModel(rhs) && !lhs.equals(rhs)) {
                return validInputLhs(lhs);
            }
        }
        return false;
    }

    private boolean isPluggableFunction(String lhs){
        return lhs.matches(PLUGGABLE_FUNCTION_REGEX); // Should match f:func(...args), where args is optional
    }

    private boolean isValidPluggableFunction(String lhs){
        var matcher = PLUGGABLE_FUNCTION_PATTERN.matcher(lhs);
        if (!matcher.find()) {
            return false;
        }
        String pluginName = matcher.group("pluginName");
        return SimplePluginLoader.containsSimplePlugin(pluginName);
    }

    private boolean validModel(String key) {
        List<String> parts = util.split(key, "!: ()");
        if (parts.isEmpty()) {
            return false;
        } else {
            // "model" alone to access the whole model dataset is not allowed
            if (MODEL.equals(parts.getFirst())) {
                return false;
            }
            // Both model.parent and model.root point to the same root state machine.
            // Accessing the whole parent namespace is not allowed.
            if (parts.getFirst().startsWith(MODEL_NAMESPACE)) {
                List<String> segments = util.split(parts.getFirst(), ".");
                return segments.size() != 1 && (segments.size() != 2 ||
                        (!PARENT.equals(segments.get(1)) && !ROOT.equals(segments.get(1))));
            }
            return true;
        }
    }

    private boolean validInputLhs(String lhs) {
        if (lhs.equals(INPUT) || lhs.startsWith(INPUT_NAMESPACE) || lhs.startsWith(JSON_PATH_TYPE) ||
                lhs.startsWith(MODEL_NAMESPACE) || lhs.startsWith(ERROR_NAMESPACE)) {
            return true;
        } else if (lhs.startsWith(MAP_TYPE) && lhs.endsWith(CLOSE_BRACKET)) {
            return validKeyValues(lhs);
        } else {
            return (lhs.startsWith(TEXT_TYPE) ||
                    lhs.startsWith(FILE_TYPE) || lhs.startsWith(CLASSPATH_TYPE) ||
                    lhs.startsWith(INTEGER_TYPE) || lhs.startsWith(LONG_TYPE) ||
                    lhs.startsWith(FLOAT_TYPE) || lhs.startsWith(DOUBLE_TYPE) ||
                    lhs.startsWith(BOOLEAN_TYPE)) && lhs.endsWith(CLOSE_BRACKET);
        }
    }

    private boolean validKeyValues(String text) {
        int last = text.lastIndexOf(CLOSE_BRACKET);
        String ref = text.substring(MAP_TYPE.length(), last).trim();
        if (ref.contains("=") || ref.contains(",")) {
            List<String> keyValues = Utility.getInstance().split(ref, ",");
            Set<String> keys = new HashSet<>();
            for (String kv: keyValues) {
                int eq = kv.indexOf('=');
                String k = eq == -1? kv.trim() : kv.substring(0, eq).trim();
                if (k.isEmpty()) {
                    return false;
                } else {
                    keys.add(k);
                }
            }
            return keys.size() == keyValues.size();
        } else {
            return !ref.isEmpty();
        }
    }

    public boolean validOutput(String output) {
        return validOutput(output, false);
    }

    public boolean validOutput(String output, boolean isDecision) {
        int sep = output.lastIndexOf(MAP_TO);
        if (sep > 0) {
            String lhs = output.substring(0, sep).trim();
            String rhs = output.substring(sep+2).trim();
            if (validModel(lhs) && validModel(rhs) && !lhs.equals(rhs)) {
                return validOutputLhs(lhs) && validOutputRhs(rhs, isDecision);
            }
        }
        return false;
    }

    private boolean validOutputLhs(String lhs) {
        if (lhs.equals(INPUT) || lhs.startsWith(INPUT_NAMESPACE) ||
                isPluggableFunction(lhs) ||
                lhs.startsWith(JSON_PATH_TYPE) ||
                lhs.startsWith(MODEL_NAMESPACE) || lhs.equals(DATA_TYPE) ||
                lhs.equals(RESULT) || lhs.startsWith(RESULT_NAMESPACE) ||
                lhs.equals(STATUS) ||
                lhs.equals(HEADER) || lhs.startsWith(HEADER_NAMESPACE)) {
            return true;
        } else if (lhs.startsWith(MAP_TYPE) && lhs.endsWith(CLOSE_BRACKET)) {
            return validKeyValues(lhs);
        } else {
            return (lhs.startsWith(TEXT_TYPE) ||
                    lhs.startsWith(FILE_TYPE) || lhs.startsWith(CLASSPATH_TYPE) ||
                    lhs.startsWith(INTEGER_TYPE) || lhs.startsWith(LONG_TYPE) ||
                    lhs.startsWith(FLOAT_TYPE) || lhs.startsWith(DOUBLE_TYPE) ||
                    lhs.startsWith(BOOLEAN_TYPE)) && lhs.endsWith(CLOSE_BRACKET);
        }
    }

    private boolean validOutputRhs(String rhs, boolean isDecision) {
        return (rhs.equals(DECISION) && isDecision) || rhs.startsWith(FILE_TYPE) ||
                rhs.startsWith(OUTPUT_NAMESPACE) || rhs.startsWith(MODEL_NAMESPACE) ||
                rhs.startsWith(EXT_NAMESPACE);
    }

    public Object getLhsOrConstant(String lhs, MultiLevelMap source) {
        Object constant = getConstantValue(lhs);
        if (constant != null) {
            return constant;
        }
        return getLhsElement(lhs, source);
    }

    public Object getLhsElement(String lhs, MultiLevelMap source) {
        if (lhs.startsWith(JSON_PATH_TYPE)) {
            return source.getElement(lhs);
        }
        int colon = getModelTypeIndex(lhs);
        String selector = colon == -1? lhs : lhs.substring(0, colon).trim();
        if (isPluggableFunction(selector)) {
            return getValueFromSimplePlugin(selector, source);
        }
        Object value = source.getElement(selector);
        if (colon != -1) {
            String type = lhs.substring(colon+1).trim();
            return getValueByType(type, value, "LHS '"+lhs+"'", source);
        }
        return value;
    }

    public Object getConstantValue(String lhs) {
        int last = lhs.lastIndexOf(CLOSE_BRACKET);
        if (last > 0) {
            if (lhs.startsWith(TEXT_TYPE)) {
                return lhs.substring(TEXT_TYPE.length(), last);
            } else if (lhs.startsWith(INTEGER_TYPE)) {
                return util.str2int(lhs.substring(INTEGER_TYPE.length(), last).trim());
            } else if (lhs.startsWith(LONG_TYPE)) {
                return util.str2long(lhs.substring(LONG_TYPE.length(), last).trim());
            } else if (lhs.startsWith(FLOAT_TYPE)) {
                return util.str2float(lhs.substring(FLOAT_TYPE.length(), last).trim());
            } else if (lhs.startsWith(DOUBLE_TYPE)) {
                return util.str2double(lhs.substring(DOUBLE_TYPE.length(), last).trim());
            } else if (lhs.startsWith(BOOLEAN_TYPE)) {
                return TRUE.equalsIgnoreCase(lhs.substring(BOOLEAN_TYPE.length(), last).trim());
            } else if (lhs.startsWith(MAP_TYPE)) {
                return getConstantMapValue(lhs, last);
            } else if (lhs.startsWith(FILE_TYPE)) {
                return getConstantFileValue(lhs);
            } else if (lhs.startsWith(CLASSPATH_TYPE)) {
                return getConstantClassPathValue(lhs);
            }
        }
        return null;
    }

    private Object getConstantMapValue(String lhs, int last) {
        String ref = lhs.substring(MAP_TYPE.length(), last).trim();
        if (ref.contains("=") || ref.contains(",")) {
            List<String> keyValues = util.split(ref, ",");
            Map<String, Object> map = new HashMap<>();
            for (String kv: keyValues) {
                int eq = kv.indexOf('=');
                String k = eq == -1? kv.trim() : kv.substring(0, eq).trim();
                String v = eq == -1? "" : kv.substring(eq+1).trim();
                if (!k.isEmpty()) {
                    map.put(k, v);
                }
            }
            return map;
        } else {
            return AppConfigReader.getInstance().get(ref);
        }
    }

    private Object getConstantFileValue(String lhs) {
        SimpleFileDescriptor fd = new SimpleFileDescriptor(lhs);
        File f = new File(fd.fileName);
        if (f.exists() && !f.isDirectory() && f.canRead()) {
            if (fd.mode == SimpleFileDescriptor.FILE_MODE.TEXT) {
                return util.file2str(f);
            } else if (fd.mode == SimpleFileDescriptor.FILE_MODE.JSON) {
                return getJsonFileContent(lhs, util.file2str(f));
            } else {
                return util.file2bytes(f);
            }
        } else {
            return null;
        }
    }

    private Object getJsonFileContent(String lhs, String content) {
        var mapper = SimpleMapper.getInstance().getMapper();
        try {
            String json = content.trim();
            if (json.startsWith("[") && json.endsWith("]")) {
                return mapper.readValue(json, List.class);
            }
            if (json.startsWith("{") && json.endsWith("}")) {
                return mapper.readValue(json, Map.class);
            }
        } catch (Exception e) {
            log.warn("Unable to decode JSON file {} - {}", lhs, e.getMessage());
        }
        return content;
    }

    private Object getConstantClassPathValue(String lhs) {
        SimpleFileDescriptor fd = new SimpleFileDescriptor(lhs);
        InputStream in = this.getClass().getResourceAsStream(fd.fileName);
        if (in != null) {
            if (fd.mode == SimpleFileDescriptor.FILE_MODE.TEXT) {
                return util.stream2str(in);
            } else if (fd.mode == SimpleFileDescriptor.FILE_MODE.JSON) {
                return getJsonFileContent(lhs, util.stream2str(in));
            } else {
                return util.stream2bytes(in);
            }
        } else {
            return null;
        }
    }

    private int getModelTypeIndex(String text) {
        if (text.startsWith(MODEL_NAMESPACE)) {
            return text.indexOf(':');
        } else {
            return -1;
        }
    }

    private Object getValueFromSimplePlugin(String selector, MultiLevelMap source){
        int prefix = selector.indexOf(SIMPLE_PLUGIN_PREFIX);
        int startParen = selector.indexOf("(");
        int endParen = selector.lastIndexOf(")");
        if (prefix >= 0 && startParen > 0 && endParen > 0) {
            String pluginName = selector.substring(prefix+2, startParen);
            String pluginParams = selector.substring(startParen+1, endParen);
            List<String> params = Utility.getInstance().split(pluginParams, ",");
            Object[] input = params.stream()
                    .map(String::trim)
                    .map(source::getElement)
                    .toArray();
            PluginFunction plugin = SimplePluginLoader.getSimplePluginByName(pluginName);
            if (plugin == null) {
                log.error("SimplePlugin '{}' not found", pluginName);
                throw new IllegalArgumentException("Unable to process SimplePlugin: " + selector);
            }
            return plugin.calculate(input);
        }
        return null;
    }

    public Object getValueByType(String type, Object value, String path, MultiLevelMap data) {
        try {
            var selection = getMappingType(type);
            if (selection == OPERATION.SIMPLE_COMMAND) {
                return handleSimpleOperation(type, value);
            } else {
                if (type.endsWith(CLOSE_BRACKET)) {
                    String command = type.substring(type.indexOf('(') + 1, type.length() - 1).trim();
                    /*
                     * substring(start, end)]
                     * substring(start)
                     * concat(parameter...) where parameters are model variable or text constant
                     * boolean(value=true)
                     * boolean(value) is same as boolean(value=true)
                     * and(model.anotherKey)
                     * or(model.anotherKey)
                     */
                    if (selection == OPERATION.SUBSTRING_COMMAND) {
                        return getSubstring(value, command);
                    } else if (selection == OPERATION.CONCAT_COMMAND) {
                        return getConcatString(value, command, data);
                    } else if (selection == OPERATION.AND_COMMAND || selection == OPERATION.OR_COMMAND) {
                        return getLogicalOperation(value, command, data, selection);
                    } else if (selection == OPERATION.BOOLEAN_COMMAND) {
                        return TypeConversionUtils.getBooleanValue(value, command);
                    }
                } else {
                    throw new IllegalArgumentException("missing close bracket");
                }
            }
        } catch (IllegalArgumentException e) {
            log.error("Unable to do {} of {} - {}", type, path, e.getMessage());
        }
        return value;
    }

    private OPERATION getMappingType(String type) {
        if (type.startsWith(SUBSTRING_TYPE)) {
            return OPERATION.SUBSTRING_COMMAND;
        } else if (type.startsWith(CONCAT_TYPE)) {
            return OPERATION.CONCAT_COMMAND;
        } else if (type.startsWith(AND_TYPE)) {
            return OPERATION.AND_COMMAND;
        } else if (type.startsWith(OR_TYPE)) {
            return OPERATION.OR_COMMAND;
        } else if (type.startsWith(BOOLEAN_TYPE)) {
            return OPERATION.BOOLEAN_COMMAND;
        } else {
            return OPERATION.SIMPLE_COMMAND;
        }
    }

    private Object handleSimpleOperation(String type, Object value) {
        switch (type) {
            case TEXT_SUFFIX -> {
                return TypeConversionUtils.getTextValue(value);
            }
            case BINARY_SUFFIX -> {
                return TypeConversionUtils.getBinaryValue(value);
            }
            case BOOLEAN_SUFFIX -> {
                return TRUE.equalsIgnoreCase(String.valueOf(value));
            }
            case NEGATE_SUFFIX -> {
                return !(TRUE.equalsIgnoreCase(String.valueOf(value)));
            }
            case INTEGER_SUFFIX -> {
                return util.str2int(String.valueOf(value));
            }
            case LONG_SUFFIX -> {
                return util.str2long(String.valueOf(value));
            }
            case FLOAT_SUFFIX -> {
                return util.str2float(String.valueOf(value));
            }
            case DOUBLE_SUFFIX -> {
                return util.str2double(String.valueOf(value));
            }
            case UUID_SUFFIX -> {
                return util.getUuid4();
            }
            case LENGTH_SUFFIX -> {
                return TypeConversionUtils.getLength(value);
            }
            case B64_SUFFIX -> {
                return TypeConversionUtils.getB64(value);
            }
            default -> throw new IllegalArgumentException("matching type must be " +
                    "substring(start, end), concat, boolean, !, and, or, text, binary, uuid or b64");
        }
    }

    private String getSubstring(Object value, String command) {
        List<String> parts = util.split(command, ", ");
        if (!parts.isEmpty() && parts.size() < 3) {
            if (value instanceof String str) {
                int start = util.str2int(parts.getFirst());
                int end = parts.size() == 1 ? str.length() : util.str2int(parts.get(1));
                if (end > start && start >= 0 && end <= str.length()) {
                    return str.substring(start, end);
                } else {
                    throw new IllegalArgumentException("index out of bound");
                }
            } else {
                throw new IllegalArgumentException("value is not a string");
            }
        } else {
            throw new IllegalArgumentException("invalid syntax");
        }
    }

    private String getConcatString(Object value, String command, MultiLevelMap data) {
        List<String> parts = tokenizeConcatParameters(command);
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("parameters must be model variables and/or text constants");
        } else {
            StringBuilder sb = new StringBuilder();
            var str = String.valueOf(value);
            sb.append(str);
            for (String p: parts) {
                if (p.startsWith(TEXT_TYPE)) {
                    sb.append(p, TEXT_TYPE.length(), p.length()-1);
                }
                if (p.startsWith(MODEL_NAMESPACE)) {
                    var v = String.valueOf(data.getElement(p));
                    sb.append(v);
                }
            }
            return sb.toString();
        }
    }

    private boolean getLogicalOperation(Object value, String command, MultiLevelMap data, OPERATION selection) {
        if (command.startsWith(MODEL_NAMESPACE) && command.length() > MODEL_NAMESPACE.length()) {
            boolean v1 = TRUE.equals(String.valueOf(value));
            boolean v2 = TRUE.equals(String.valueOf(data.getElement(command)));
            return selection == OPERATION.AND_COMMAND ? v1 && v2 : v1 || v2;
        } else {
            throw new IllegalArgumentException("'" + command + "' is not a model variable");
        }
    }

    private List<String> tokenizeConcatParameters(String text) {
        List<String> result = new ArrayList<>();
        var md = new MutableCommand(text.trim());
        while (!md.command.isEmpty()) {
            if (md.command.startsWith(MODEL_NAMESPACE)) {
                var o = getConcatParamModel(result, md);
                if (o.isPresent()) {
                    return o.get();
                }
            } else if (md.command.startsWith(TEXT_TYPE)) {
                var o = getConcatParamText(result, md);
                if (o.isPresent()) {
                    return o.get();
                }
            } else {
                return Collections.emptyList();
            }
        }
        return result;
    }

    private Optional<List<String>> getConcatParamText(List<String> result, MutableCommand md) {
        int close = md.command.indexOf(CLOSE_BRACKET);
        if (close == 1) {
            return Optional.of(Collections.emptyList());
        } else {
            result.add(md.command.substring(0, close+1));
            int sep = md.command.indexOf(',', close);
            if (sep == -1) {
                return Optional.of(result);
            } else {
                md.command = md.command.substring(sep+1).trim();
                return Optional.empty();
            }
        }
    }

    private Optional<List<String>> getConcatParamModel(List<String> result, MutableCommand md) {
        int sep = md.command.indexOf(',');
        if (sep == -1) {
            result.add(md.command);
            return Optional.of(result);
        } else {
            var token = md.command.substring(0, sep).trim();
            if (token.equals(MODEL_NAMESPACE)) {
                return Optional.of(Collections.emptyList());
            } else {
                result.add(token);
                md.command = md.command.substring(sep + 1).trim();
                return Optional.empty();
            }
        }
    }

    private static class MutableCommand {
        String command;

        MutableCommand(String command) {
            this.command = command;
        }
    }
}
