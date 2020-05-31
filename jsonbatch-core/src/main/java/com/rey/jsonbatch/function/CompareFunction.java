package com.rey.jsonbatch.function;

import com.rey.jsonbatch.JsonBuilder.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unchecked")
public class CompareFunction extends Function {

    private Logger logger = LoggerFactory.getLogger(CompareFunction.class);

    private static final String PATTERN_COMPARISON = "(<=|>=|==|!=|>|<)";
    @Override
    public String getName() {
        return "compare";
    }

    @Override
    public boolean isReduceFunction() {
        return false;
    }

    @Override
    public Object invoke(Type type, List<Object> arguments) {
        if(arguments.size() != 1){
            logger.error("Expect only one argument");
            throw new IllegalArgumentException("Invalid arguments size");
        }
        if(!(arguments.get(0) instanceof String)){
            logger.error("Expect string argument");
            throw new IllegalArgumentException("Invalid argument type: " + arguments.get(0).getClass());
        }
        String expression = (String)arguments.get(0);
        Matcher matcher = Pattern.compile(PATTERN_COMPARISON).matcher(expression);
        if(!matcher.find()) {
            logger.error("Not found comparison operator");
            throw new IllegalArgumentException("Invalid format: " + expression);
        }

        String comparison = matcher.group(0);
        Object leftValue = parse(expression.substring(0, matcher.start()).trim(), null);
        Object rightValue = parse(expression.substring(matcher.end()).trim(), leftValue.getClass());

        logger.info("Compare: {} {} {}", leftValue, comparison, rightValue);

        if(leftValue instanceof BigDecimal)
            return compare((BigDecimal)leftValue, (BigDecimal)rightValue, comparison);
        if(leftValue instanceof Boolean)
            return compare((Boolean)leftValue, (Boolean)rightValue, comparison);
        if(leftValue instanceof String)
            return compare((String)leftValue, (String)rightValue, comparison);
        return Boolean.FALSE;
    }

    private <T> T parse(String rawData, Class<T> clazz) {
        if(clazz == null) {
            try {
                return (T)new BigDecimal(rawData);
            }
            catch (NumberFormatException ex) {
                logger.trace("Cannot parse [{}] as decimal", rawData);
            }
            if(rawData.equalsIgnoreCase("true") || rawData.equalsIgnoreCase("false")) {
                return (T) Boolean.valueOf(rawData.equalsIgnoreCase("true"));
            }
            return (T) rawData;
        }
        else if (clazz == BigDecimal.class)
            return (T)new BigDecimal(rawData);
        else if (clazz == Boolean.class)
            return (T) Boolean.valueOf(rawData.equalsIgnoreCase("true"));
        else if (clazz == String.class)
            return (T) rawData;
        return null;
    }

    private Boolean compare(BigDecimal left, BigDecimal right, String comparison) {
        switch (comparison) {
            case "<=":
                return left.compareTo(right) <= 0;
            case ">=":
                return left.compareTo(right) >= 0;
            case "==":
                return left.compareTo(right) == 0;
            case "!=":
                return left.compareTo(right) != 0;
            case "<":
                return left.compareTo(right) < 0;
            case ">":
                return left.compareTo(right) > 0;
            default:
                return false;
        }
    }

    private Boolean compare(Boolean left, Boolean right, String comparison) {
        switch (comparison) {
            case "==":
                return left.equals(right);
            case "!=":
                return !left.equals(right);
            default:
                throw new IllegalArgumentException("Not support operation: " + comparison + " for boolean type");
        }
    }

    private Boolean compare(String left, String right, String comparison) {
        switch (comparison) {
            case "==":
                return left.equals(right);
            case "!=":
                return !left.equals(right);
            default:
                throw new IllegalArgumentException("Not support operation: " + comparison + " for string type");
        }
    }

    public static CompareFunction instance() {
        return new CompareFunction();
    }

}
