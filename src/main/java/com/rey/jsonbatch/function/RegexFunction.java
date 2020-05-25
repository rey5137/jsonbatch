package com.rey.jsonbatch.function;

import com.jayway.jsonpath.DocumentContext;
import com.rey.jsonbatch.JsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unchecked")
public class RegexFunction implements JsonFunction {

    private Logger logger = LoggerFactory.getLogger(RegexFunction.class);

    private static final String PATTERN_ARGUMENT = "^\\s*\"(.*)\"\\s*,\\s*\"(.*)\"\\s*,\\s*(\\d*)\\s*$";

    @Override
    public String getName() {
        return "regex";
    }

    @Override
    public List<JsonBuilder.Type> supportedTypes() {
        return Collections.singletonList(
                JsonBuilder.Type.STRING
        );
    }

    @Override
    public Object handle(JsonBuilder jsonBuilder, JsonBuilder.Type type, String arguments, DocumentContext context) {
        Matcher matcher = Pattern.compile(PATTERN_ARGUMENT).matcher(arguments);
        if (!matcher.matches())
            throw new IllegalArgumentException("Invalid argument: " + arguments);
        String path = matcher.group(1);
        String pattern = matcher.group(2);
        int groupIndex = Integer.parseInt(matcher.group(3));
        String value = (String) jsonBuilder.build("str " + path, context);
        matcher = Pattern.compile(pattern).matcher(value);
        if (matcher.matches()) {
            if (groupIndex <= matcher.groupCount())
                return matcher.group(groupIndex);
            else
                logger.warn("Group index {} large than total {}", groupIndex, matcher.groupCount());
            return null;
        }
        logger.warn("Pattern [{}] not match with value [{}]", pattern, value);
        return null;
    }

    public static RegexFunction instance() {
        return new RegexFunction();
    }
}
