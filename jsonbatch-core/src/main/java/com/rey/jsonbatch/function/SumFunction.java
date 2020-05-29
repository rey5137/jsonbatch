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
public class SumFunction implements Function {

    private Logger logger = LoggerFactory.getLogger(SumFunction.class);

    @Override
    public String getName() {
        return "sum";
    }

    @Override
    public Object invoke(Type type, List<Object> arguments) {
        if(type == INTEGER)
            return sumAll(new BigInteger("0"), arguments);
        return sumAll(new BigDecimal("0"), arguments);
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
