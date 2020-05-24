package com.rey.jsonbatch;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;
import com.rey.jsonbatch.function.JsonFunction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
public class JsonBuilder {

    private Logger logger;

    private static final String PATTERN_PARAM_DELIMITER = "\\s{1,}";
    private static final String PATTERN_JSON_PATH = "(^\\$.*$)";
    private static final String PATTERN_FUNCTION = "^__(\\w*)\\((.*)\\)$";
    private static final String PATTERN_INLINE_VARIABLE = "@\\{(((?!@\\{).)*)}@";

    private static final String KEY_ARRAY_PATH = "__array_path";

    private List<JsonFunction> functions = new ArrayList<>();

    public JsonBuilder(Logger logger, JsonFunction... functions) {
        this.logger = logger;
        Collections.addAll(this.functions, functions);
    }

    public Object build(Object schema, DocumentContext context) {
        if(schema instanceof String)
            return buildNode((String)schema, context);
        if(schema instanceof Map)
            return buildObject((Map)schema, context);
        if(schema instanceof Collection)
            return buildList((Collection)schema, context);
        throw new IllegalArgumentException("Unsupported class: " + schema.getClass());
    }

    private Object buildNode(String schema, DocumentContext context) {
        String[] parts = schema.split(PATTERN_PARAM_DELIMITER, 2);
        if(parts.length < 2)
            throw new IllegalArgumentException("Invalid schema: " + schema);

        Type type = Type.from(parts[0]);

        Matcher matcher = Pattern.compile(PATTERN_JSON_PATH).matcher(parts[1]);
        if(matcher.matches()) {
            String jsonPath = matcher.group(1);
            return buildNodeFromJsonPath(type, context, jsonPath);
        }

        matcher = Pattern.compile(PATTERN_FUNCTION).matcher(parts[1]);
        if(matcher.matches()) {
            String function = matcher.group(1);
            String arguments = matcher.group(2);
            return buildNodeFromFunction(type, function, arguments, context);
        }

        return buildNodeFromRawData(type, parts[1], context);
    }

    private Map buildObject(Map<String, Object> schema, DocumentContext context) {
        Map<String, Object> result = new LinkedHashMap<>();
        schema.forEach((key, childSchema) -> {
            if(isValidKey(key)) {
                if (childSchema instanceof String)
                    result.put(key, buildNode((String) childSchema, context));
                if (childSchema instanceof Map)
                    result.put(key, buildObject((Map) childSchema, context));
                if (childSchema instanceof List)
                    result.put(key, buildList((List) childSchema, context));
            }
        });
        return result;
    }

    private List buildList(Collection schema, DocumentContext context) {
        Map<String, Object> childSchema = (Map<String, Object>)schema.iterator().next();
        String arrayPath = (String)childSchema.get(KEY_ARRAY_PATH);
        List<Object> list = context.read(arrayPath);
        return list.stream()
                .map(object -> build(childSchema, JsonPath.using(context.configuration()).parse(object)))
                .collect(Collectors.toList());
    }

    private Object buildNodeFromJsonPath(Type type, DocumentContext context, String jsonPath) {
        logger.debug("build Node with [%s] jsonPath to [%s] type", jsonPath, type);
        Object object = context.read(jsonPath);
        if(object == null)
            return null;

        if(!type.isArray) {
            if(object instanceof List) {
                List list = (List)object;
                object = list.isEmpty() ? null : list.get(0);
            }
            return castToType(object, type);
        }
        else {
            if(!(object instanceof List)) {
                object = Collections.singleton(object);
            }
            return ((List)object).stream()
                    .map(obj -> castToType(obj, type.elementType))
                    .collect(Collectors.toList());
        }
    }

    private Object buildNodeFromFunction(Type type, String funcName, String arguments, DocumentContext context) {
        Optional<JsonFunction> funcOptional = functions.stream()
                .filter(func -> func.getName().equals(funcName))
                .findFirst();
        if(!funcOptional.isPresent())
            throw new IllegalArgumentException("Not support function: " + funcName);
        JsonFunction function = funcOptional.get();
        if(!function.supportedTypes().contains(type))
            throw new IllegalArgumentException(String.format("Function [%s] not support type [%s]", funcName, type));
        return function.handle(this, type, arguments, context, logger);
    }

    private Object buildNodeFromRawData(Type type, String rawData, DocumentContext context) {
        switch (type) {
            case STRING:
                return buildStringFromRawData(rawData, context);
            case INTEGER:
            case NUMBER:
            case BOOLEAN:
                return castToType(rawData, type);
            default:
                return JsonPath.using(context.configuration()).parse(rawData);
        }
    }

    private Object castToType(Object object, Type type) {
        switch (type) {
            case STRING:
                return object.toString();
            case INTEGER:
                if(object instanceof String || object instanceof Integer || object instanceof Long)
                    return Long.parseLong(object.toString());
                if(object instanceof Float)
                    return Math.round((float)object);
                if(object instanceof Double)
                    return Math.round((double)object);
                throw new IllegalArgumentException("Cannot cast " + object.getClass() + " to integer");
            case NUMBER:
                if(object instanceof String || object instanceof Integer || object instanceof Long || object instanceof Float || object instanceof Double)
                    return new BigDecimal(object.toString());
                throw new IllegalArgumentException("Cannot cast " + object.getClass() + " to number");
            case BOOLEAN:
                if(object instanceof Boolean)
                    return object;
                if(object instanceof Integer)
                    return !object.equals(0);
                if(object instanceof Long)
                    return !object.equals(0L);
                if(object instanceof Float)
                    return !object.equals(0F);
                if(object instanceof Double)
                    return !object.equals(0D);
                if(object instanceof String)
                    return ((String) object).equalsIgnoreCase("true");
                throw new IllegalArgumentException("Cannot cast " + object.getClass() + " to boolean");
        }
        return object;
    }

    private String buildStringFromRawData(String rawData, DocumentContext context) {
        Matcher matcher = Pattern.compile(PATTERN_INLINE_VARIABLE).matcher(rawData);
        int startIndex = 0;
        StringBuilder builder = new StringBuilder();
        while(matcher.find()) {
            int groupStart = matcher.start();
            int groupEnd = matcher.end();
            if(startIndex < groupStart) {
                builder.append(rawData, startIndex, groupStart);
            }
            builder.append(build(matcher.group(1), context));
            startIndex = groupEnd;
        }

        if(startIndex < rawData.length())
            builder.append(rawData, startIndex, rawData.length());

        return builder.toString();
    }

    private boolean isValidKey(String key) {
        return !KEY_ARRAY_PATH.equals(key);
    }

    public enum Type {
        STRING(null, "str", "string"),
        INTEGER(null, "int", "integer"),
        NUMBER(null, "num", "number"),
        BOOLEAN(null, "bool", "boolean"),
        OBJECT(null, "obj", "object"),
        STRING_ARRAY(Type.STRING, "str[]", "string[]"),
        INTEGER_ARRAY(Type.INTEGER, "int[]", "integer[]"),
        NUMBER_ARRAY(Type.NUMBER, "num[]", "number[]"),
        BOOLEAN_ARRAY(Type.BOOLEAN, "bool[]", "boolean[]"),
        OBJECT_ARRAY(Type.OBJECT, "obj[]", "object[]");

        public final boolean isArray;
        public final Type elementType;
        private final String[] values;

        Type(Type elementType, String... values) {
            this.isArray = elementType != null;
            this.elementType = elementType;
            this.values= values;
        }

        static Type from(String value) {
            return Stream.of(Type.values())
                    .filter(type -> Stream.of(type.values).anyMatch(v -> v.equalsIgnoreCase(value)))
                    .findFirst()
                    .orElse(null);
        }

    }

}
