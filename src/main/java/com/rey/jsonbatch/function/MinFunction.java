package com.rey.jsonbatch.function;

import com.jayway.jsonpath.DocumentContext;
import com.rey.jsonbatch.JsonBuilder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unchecked")
public class MinFunction implements JsonFunction {

    private static final String PATTERN_ARGUMENT = "^\\s*\"(.*)\"\\s*$";

    @Override
    public String getName() {
        return "min";
    }

    @Override
    public List<JsonBuilder.Type> supportedTypes() {
        return Arrays.asList(
                JsonBuilder.Type.INTEGER,
                JsonBuilder.Type.NUMBER
        );
    }

    @Override
    public Object handle(JsonBuilder jsonBuilder, JsonBuilder.Type type, String arguments, DocumentContext context) {
        Matcher matcher = Pattern.compile(PATTERN_ARGUMENT).matcher(arguments);
        if (!matcher.matches())
            throw new IllegalArgumentException("Invalid argument: " + arguments);
        String path = matcher.group(1);
        switch (type) {
            case INTEGER: {
                List<Long> items = (List<Long>) jsonBuilder.build("int[] " + path, context);
                return items.stream().reduce(Math::min).orElse(null);
            }
            case NUMBER: {
                List<BigDecimal> items = (List<BigDecimal>) jsonBuilder.build("num[] " + path, context);
                return items.stream().reduce((first, second) -> first.compareTo(second) <= 0 ? first : second).orElse(null);
            }
        }
        return null;
    }

    public static MinFunction instance() {
        return new MinFunction();
    }
}
