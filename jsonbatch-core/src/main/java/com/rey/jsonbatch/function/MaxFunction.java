package com.rey.jsonbatch.function;

import com.rey.jsonbatch.JsonBuilder.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static com.rey.jsonbatch.JsonBuilder.Type.INTEGER;
import static com.rey.jsonbatch.function.MathUtils.max;
import static com.rey.jsonbatch.function.MathUtils.toBigDecimal;
import static com.rey.jsonbatch.function.MathUtils.toBigInteger;

@SuppressWarnings("unchecked")
public class MaxFunction extends Function {

    private Logger logger = LoggerFactory.getLogger(MaxFunction.class);

    @Override
    public String getName() {
        return "max";
    }

    @Override
    public boolean isReduceFunction() {
        return true;
    }

    @Override
    public Result handle(Type type, Object argument, Result prevResult) {
        if(type == INTEGER) {
            Result<BigInteger> result = prevResult == null ? Result.of(null, false) : prevResult;
            if(argument instanceof List)
                result.setValue(max(result.getValue(), maxInteger((List) argument)));
            else {
                BigInteger value = toBigInteger(argument);
                if(value == null) {
                    logger.error("Cannot process [{}] type", argument.getClass());
                    throw new IllegalArgumentException("Cannot process item");
                }
                result.setValue(max(result.getValue(), value));
            }
            return result;
        }
        else {
            Result<BigDecimal> result = prevResult == null ? Result.of(null, false) : prevResult;
            if(argument instanceof List)
                result.setValue(max(result.getValue(), maxDecimal((List) argument)));
            else {
                BigDecimal value = toBigDecimal(argument);
                if(value == null) {
                    logger.error("Cannot process [{}] type", argument.getClass());
                    throw new IllegalArgumentException("Cannot process item");
                }
                result.setValue(max(result.getValue(), value));
            }
            return result;
        }
    }

    private BigInteger maxInteger(List<Object> items) {
        BigInteger result = null;
        for(Object item : items) {
            if(item instanceof List) {
                result = max(result, maxInteger((List)item));
            }
            else {
                BigInteger value = toBigInteger(item);
                if(value == null) {
                    logger.error("Cannot process [{}] type", item.getClass());
                    throw new IllegalArgumentException("Cannot process item");
                }
                result = max(result, value);
            }
        }
        return result;
    }

    private BigDecimal maxDecimal(List<Object> items) {
        BigDecimal result = null;
        for(Object item : items) {
            if(item instanceof List) {
                result = max(result, maxDecimal((List)item));
            }
            else {
                BigDecimal value = toBigDecimal(item);
                if(value == null) {
                    logger.error("Cannot process [{}] type", item.getClass());
                    throw new IllegalArgumentException("Cannot process item");
                }
                result = max(result, value);
            }
        }
        return result;
    }

    public static MaxFunction instance() {
        return new MaxFunction();
    }

}
