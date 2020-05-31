package com.rey.jsonbatch.function;

import com.rey.jsonbatch.JsonBuilder.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
public class OrFunction extends Function {

    private Logger logger = LoggerFactory.getLogger(OrFunction.class);

    @Override
    public String getName() {
        return "or";
    }

    @Override
    public boolean isReduceFunction() {
        return true;
    }

    @Override
    public Result handle(Type type, Object argument, Result prevResult) {
        Result<Boolean> result = prevResult == null ? Result.of(false, false) : prevResult;
        if(argument instanceof Boolean) {
            result.value = (Boolean)argument;
            result.isDone = result.value;
            return result;
        }
        logger.error("Cannot process [{}] type", argument.getClass());
        throw new IllegalArgumentException("Cannot process item");
    }

    public static OrFunction instance() {
        return new OrFunction();
    }

}
