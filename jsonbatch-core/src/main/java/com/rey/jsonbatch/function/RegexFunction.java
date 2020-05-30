package com.rey.jsonbatch.function;

import com.rey.jsonbatch.JsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unchecked")
public class RegexFunction extends Function {

    private Logger logger = LoggerFactory.getLogger(RegexFunction.class);

    @Override
    public String getName() {
        return "regex";
    }

    @Override
    public boolean isReduceFunction() {
        return false;
    }

    @Override
    public Object invoke(JsonBuilder.Type type, List<Object> arguments) {
        String value = (String) arguments.get(0);
        String pattern = (String) arguments.get(1);
        int groupIndex = MathUtils.toInteger(arguments.get(2));
        Matcher matcher = Pattern.compile(pattern).matcher(value);
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
