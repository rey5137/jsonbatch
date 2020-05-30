package com.rey.jsonbatch.function;

import com.rey.jsonbatch.JsonBuilder.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static com.rey.jsonbatch.JsonBuilder.Type.INTEGER;
import static com.rey.jsonbatch.function.MathUtils.toBigDecimal;
import static com.rey.jsonbatch.function.MathUtils.toBigInteger;

@SuppressWarnings("unchecked")
public class SumFunction extends Function {

    private Logger logger = LoggerFactory.getLogger(SumFunction.class);

    @Override
    public String getName() {
        return "sum";
    }

    @Override
    public boolean isReduceFunction() {
        return true;
    }

    @Override
    public Result handle(Type type, Object argument, Result prevResult) {
        if(type == INTEGER) {
            Result<BigInteger> result = prevResult == null ? Result.of(new BigInteger("0"), false) : prevResult;
            if(argument instanceof List)
                result.setValue(sumAll(result.getValue(), (List) argument));
            else {
                BigInteger value = toBigInteger(argument);
                if(value == null) {
                    logger.error("Cannot process [{}] type", argument.getClass());
                    throw new IllegalArgumentException("Cannot process item");
                }
                result.setValue(result.getValue().add(value));
            }
            return result;
        }
        else {
            Result<BigDecimal> result = prevResult == null ? Result.of(new BigDecimal("0"), false) : prevResult;
            if(argument instanceof List)
                result.setValue(sumAll(result.getValue(), (List) argument));
            else {
                BigDecimal value = toBigDecimal(argument);
                if(value == null) {
                    logger.error("Cannot process [{}] type", argument.getClass());
                    throw new IllegalArgumentException("Cannot process item");
                }
                result.setValue(result.getValue().add(value));
            }
            return result;
        }
    }

    private BigInteger sumAll(BigInteger total, List<Object> items) {
        for(Object item : items) {
            if(item instanceof List)
                total = sumAll(total, (List) item);
            else {
                BigInteger value = toBigInteger(item);
                if(value == null) {
                    logger.error("Cannot process [{}] type", item.getClass());
                    throw new IllegalArgumentException("Cannot process item");
                }
                total = total.add(value);
            }
        }
        return total;
    }

    private BigDecimal sumAll(BigDecimal total, List<Object> items) {
        for(Object item : items) {
            if(item instanceof List)
                total = sumAll(total, (List) item);
            else {
                BigDecimal value = toBigDecimal(item);
                if(value == null) {
                    logger.error("Cannot process [{}] type", item.getClass());
                    throw new IllegalArgumentException("Cannot process item");
                }
                total = total.add(value);
            }
        }
        return total;
    }

    public static SumFunction instance() {
        return new SumFunction();
    }

}
