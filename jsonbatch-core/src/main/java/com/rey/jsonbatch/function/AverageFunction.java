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
public class AverageFunction implements Function {

    private Logger logger = LoggerFactory.getLogger(AverageFunction.class);

    @Override
    public String getName() {
        return "average";
    }

    @Override
    public Object invoke(Type type, List<Object> arguments) {
        if(type == INTEGER) {
            Result<BigInteger> result = sumAll(new BigInteger("0"), arguments);
            return result.value.divide(new BigInteger(String.valueOf(result.count)));
        }

        Result<BigDecimal> result = sumAll(new BigDecimal("0"), arguments);
        return result.value.divide(new BigDecimal(String.valueOf(result.count)));
    }

    private Result<BigInteger> sumAll(BigInteger total, List<Object> items) {
        Result<BigInteger> finalResult = new Result<>(0, total);
        for(Object item : items) {
            if(item instanceof List) {
                Result<BigInteger> result = sumAll(total, (List) item);
                finalResult.count += result.count;
                finalResult.value = finalResult.value.add(result.value);

            }
            else {
                BigInteger value = toBigInteger(item);
                if(value == null) {
                    logger.error("Cannot process [{}] type", item.getClass());
                    throw new IllegalArgumentException("Cannot process item");
                }
                finalResult.count ++;
                finalResult.value = finalResult.value.add(value);
            }
        }
        return finalResult;
    }

    private Result<BigDecimal> sumAll(BigDecimal total, List<Object> items) {
        Result<BigDecimal> finalResult = new Result<>(0, total);
        for(Object item : items) {
            if(item instanceof List) {
                Result<BigDecimal> result = sumAll(total, (List) item);
                finalResult.count += result.count;
                finalResult.value = finalResult.value.add(result.value);

            }
            else {
                BigDecimal value = toBigDecimal(item);
                if(value == null) {
                    logger.error("Cannot process [{}] type", item.getClass());
                    throw new IllegalArgumentException("Cannot process item");
                }
                finalResult.count ++;
                finalResult.value = finalResult.value.add(value);
            }
        }
        return finalResult;
    }

    public static AverageFunction instance() {
        return new AverageFunction();
    }

    private class Result<T> {
        int count;
        T value;

        Result(int count, T value) {
            this.count = count;
            this.value = value;
        }
    }

}
