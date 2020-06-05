package com.rey.jsonbatch;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.rey.jsonbatch.function.Function;
import com.rey.jsonbatch.function.MathUtils;
import com.rey.jsonbatch.parser.Parser;
import com.rey.jsonbatch.parser.Token;
import com.rey.jsonbatch.parser.TokenValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class JsonBuilder {

    private Logger logger = LoggerFactory.getLogger(JsonBuilder.class);

    private static final Pattern PATTERN_NUMERIC = Pattern.compile("^[0123456789.]*$");

    private static final String KEY_ARRAY_PATH = "__array_path";

    private Map<String, Function> functionMap = new HashMap<>();

    private Parser parser = new Parser();

    public JsonBuilder(Function... functions) {
        for (Function f : functions)
            functionMap.put(f.getName(), f);
    }

    public Object build(Object schema, DocumentContext context) {
        logger.info("Build schema: {}", schema);
        if (schema instanceof String)
            return buildNode((String) schema, context);
        if (schema instanceof Map)
            return buildObject((Map) schema, context);
        if (schema instanceof Collection)
            return buildList((Collection) schema, context);
        logger.error("Unsupported class: {}", schema.getClass());
        throw new IllegalArgumentException("Unsupported class: " + schema.getClass());
    }

    private Object buildNode(String schema, DocumentContext context) {
        Type type = null;
        List<TokenValue> tokenValues = null;
        for (Type t : Type.values()) {
            for (String value : t.values) {
                if (schema.startsWith(value)) {
                    type = t;
                    tokenValues = parser.parse(schema.substring(value.length()).trim());
                    break;
                }
            }
        }
        if (type == null)
            tokenValues = parser.parse(schema.trim());

        TokenValue firstToken = tokenValues.get(0);
        if (firstToken.getToken() == Token.JSON_PATH)
            return buildNodeFromJsonPath(type, context, firstToken.getValue());
        else if (firstToken.getToken() == Token.FUNC) {
            return buildNodeFromFunction(type, tokenValues, context);
        }

        return buildNodeFromRawData(type, firstToken.getValue(), context);
    }

    private Map buildObject(Map<String, Object> schema, DocumentContext context) {
        Map<String, Object> result = new LinkedHashMap<>();
        schema.forEach((key, childSchema) -> {
            if (isValidKey(key)) {
                logger.info("Build for [{}] key with schema: {}", key, childSchema);
                if (childSchema instanceof String)
                    result.put(key, buildNode((String) childSchema, context));
                if (childSchema instanceof Map)
                    result.put(key, buildObject((Map) childSchema, context));
                if (childSchema instanceof Collection)
                    result.put(key, buildList((Collection) childSchema, context));
            }
        });
        return result;
    }

    private List buildList(Collection schema, DocumentContext context) {
        List<Object> result = new ArrayList<>();
        for (Object childSchema : (Iterable<Object>) schema) {
            logger.info("Build items with schema: {}", childSchema);
            if (childSchema instanceof String) {
                Object item = build(childSchema, context);
                if (item instanceof Collection)
                    result.addAll((Collection) item);
                else
                    result.add(item);
            } else if (childSchema instanceof Map) {
                String arrayPath = (String) ((Map)childSchema).get(KEY_ARRAY_PATH);
                if (arrayPath == null) {
                    logger.error("Missing array path in child schema");
                    throw new IllegalArgumentException("Missing array path in child schema");
                }
                List<Object> items = context.read(arrayPath);
                result.addAll(items.stream()
                        .map(object -> build(childSchema, JsonPath.using(context.configuration()).parse(object)))
                        .collect(Collectors.toList()));
            } else if(childSchema instanceof Collection) {
                result.add(buildList((Collection)childSchema, context));
            }
        }
        return result;
    }

    private Object buildNodeFromJsonPath(Type type, DocumentContext context, String jsonPath) {
        logger.trace("build Node with [{}] jsonPath to [{}] type", jsonPath, type);
        Object object = context.read(jsonPath);
        if (object == null)
            return null;
        if (type == null)
            return object;
        if (!type.isArray) {
            if (object instanceof List) {
                List list = (List) object;
                object = list.isEmpty() ? null : list.get(0);
            }
            return castToType(object, type);
        } else {
            if (!(object instanceof List)) {
                object = Collections.singleton(object);
            }
            return ((List) object).stream()
                    .map(obj -> castToType(obj, type.elementType))
                    .collect(Collectors.toList());
        }
    }

    private Object buildNodeFromFunction(Type type, List<TokenValue> tokenValues, DocumentContext context) {
        TokenValue tokenValue = tokenValues.remove(0);
        final String funcName = tokenValue.getValue();
        logger.trace("build Node with [{}] function to [{}] type", funcName, type);
        Function function = functionMap.get(funcName);
        if (function == null) {
            logger.error("Unsupported function: {}", funcName);
            throw new IllegalArgumentException("Not support function: " + funcName);
        }
        if (function.isReduceFunction()) {
            Function.Result result = null;
            while (!tokenValues.isEmpty()) {
                tokenValue = tokenValues.get(0);
                Object argument = null;
                if (tokenValue.getToken() == Token.JSON_PATH)
                    argument = context.read(tokenValue.getValue());
                else if (tokenValue.getToken() == Token.FUNC)
                    argument = buildNodeFromFunction(null, tokenValues, context);
                else if (tokenValue.getToken() == Token.RAW)
                    argument = parseRawData(tokenValue.getValue(), context);
                else if (tokenValue.getToken() == Token.END_FUNC)
                    break;
                tokenValues.remove(0);
                result = function.handle(type, argument, result);
                if (result != null && result.isDone())
                    return result.getValue();
            }
            return result == null ? null : result.getValue();
        } else {
            List<Object> arguments = new ArrayList<>();
            while (!tokenValues.isEmpty()) {
                tokenValue = tokenValues.get(0);
                if (tokenValue.getToken() == Token.JSON_PATH)
                    arguments.add(context.read(tokenValue.getValue()));
                else if (tokenValue.getToken() == Token.FUNC)
                    arguments.add(buildNodeFromFunction(null, tokenValues, context));
                else if (tokenValue.getToken() == Token.RAW)
                    arguments.add(parseRawData(tokenValue.getValue(), context));
                else if (tokenValue.getToken() == Token.END_FUNC)
                    break;
                tokenValues.remove(0);
            }
            return function.invoke(type, arguments);
        }
    }

    private Object parseRawData(String rawData, DocumentContext context) {
        if(PATTERN_NUMERIC.matcher(rawData).matches()) {
            if (rawData.contains(".")) {
                try {
                    return new BigDecimal(rawData);
                } catch (NumberFormatException ex) {
                    logger.trace("Cannot parse [{}] as decimal", rawData);
                }
            } else {
                try {
                    return new BigInteger(rawData);
                } catch (NumberFormatException ex) {
                    logger.trace("Cannot parse [{}] as integer", rawData);
                }
            }
        }
        if (rawData.equalsIgnoreCase("true") || rawData.equalsIgnoreCase("false")) {
            return rawData.equalsIgnoreCase("true");
        }
        return buildStringFromRawData(rawData, context);
    }

    private Object buildNodeFromRawData(Type type, String rawData, DocumentContext context) {
        logger.trace("build Node with [{}] rawData to [{}] type", rawData, type);
        if (type == null)
            return buildStringFromRawData(rawData, context);
        switch (type) {
            case STRING:
                return buildStringFromRawData(rawData, context);
            case INTEGER:
            case NUMBER:
            case BOOLEAN:
                return castToType(rawData, type);
            default:
                return context.configuration().jsonProvider().parse(rawData);
        }
    }

    private Object castToType(Object object, Type type) {
        switch (type) {
            case STRING:
                return object.toString();
            case INTEGER:
                if(object instanceof Integer || object instanceof Long || object instanceof BigInteger)
                    return object;
                if(object instanceof Float)
                    return Math.round((Float)object);
                if(object instanceof Double)
                    return Math.round((Double)object);
                if(object instanceof BigDecimal)
                    return ((BigDecimal)object).toBigInteger();
                if(object instanceof String)
                    return new BigInteger(object.toString());
                throw new IllegalArgumentException("Cannot cast " + object.getClass() + " to integer");
            case NUMBER:
                if(object instanceof Float || object instanceof Double || object instanceof BigDecimal)
                    return object;
                if(object instanceof Integer)
                    return ((Integer) object).floatValue();
                if(object instanceof Long)
                    return ((Long) object).doubleValue();
                if(object instanceof BigInteger)
                    return new BigDecimal((BigInteger)object);
                if (object instanceof String)
                    return new BigDecimal(object.toString());
                throw new IllegalArgumentException("Cannot cast " + object.getClass() + " to number");
            case BOOLEAN:
                Boolean result = MathUtils.toBoolean(object);
                if (result == null)
                    throw new IllegalArgumentException("Cannot cast " + object.getClass() + " to boolean");
                return result;
        }
        return object;
    }

    private String buildStringFromRawData(String str, DocumentContext context) {
        boolean isEscaped = false;
        StringBuilder builder = new StringBuilder();
        StringBuilder varBuilder = new StringBuilder();
        int varCount = 0;
        int i = 0;
        while(i < str.length()) {
            char curChar = str.charAt(i);
            if(curChar == '@' && checkChar(str, i + 1, '{') && !isEscaped) {
                varBuilder.append(str, i, i + 2);
                i++;
                varCount++;
            }
            else if(curChar == '}' && checkChar(str, i + 1, '@') && !isEscaped && varCount > 0) {
                varBuilder.append(str, i, i + 2);
                i++;
                varCount--;
                if(varCount == 0) {
                    builder.append(build(varBuilder.substring(2, varBuilder.length() - 2), context));
                    varBuilder.delete(0, varBuilder.length());
                }
            }
            else if (varCount != 0) {
                if(curChar == '\\') {
                    if(isEscaped)
                        varBuilder.append(curChar);
                    isEscaped = !isEscaped;
                }
                else {
                    varBuilder.append(curChar);
                    isEscaped = false;
                }
            }
            else {
                if(curChar == '\\') {
                    if(isEscaped)
                        builder.append(curChar);
                    isEscaped = !isEscaped;
                }
                else {
                    builder.append(curChar);
                    isEscaped = false;
                }
            }
            i++;
        }
        return builder.toString();
    }

    private boolean checkChar(String str, int index, char c) {
        return index < str.length() && str.charAt(index) == c;
    }

    private boolean isValidKey(String key) {
        return !KEY_ARRAY_PATH.equals(key);
    }

    public enum Type {
        STRING(null, "str ", "string "),
        INTEGER(null, "int ", "integer "),
        NUMBER(null, "num ", "number "),
        BOOLEAN(null, "bool ", "boolean "),
        OBJECT(null, "obj ", "object "),
        STRING_ARRAY(Type.STRING, "str[] ", "string[] "),
        INTEGER_ARRAY(Type.INTEGER, "int[] ", "integer[] "),
        NUMBER_ARRAY(Type.NUMBER, "num[] ", "number[] "),
        BOOLEAN_ARRAY(Type.BOOLEAN, "bool[] ", "boolean[] "),
        OBJECT_ARRAY(Type.OBJECT, "obj[] ", "object[] ");

        public final boolean isArray;
        public final Type elementType;
        private final String[] values;

        Type(Type elementType, String... values) {
            this.isArray = elementType != null;
            this.elementType = elementType;
            this.values = values;
        }

    }

}
