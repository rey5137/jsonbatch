package com.rey.jsonbatch.function;

import com.rey.jsonbatch.JsonBuilder.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
public class AndFunction extends Function {

    private Logger logger = LoggerFactory.getLogger(AndFunction.class);

    @Override
    public String getName() {
        return "and";
    }

    @Override
    public boolean isReduceFunction() {
        return true;
    }

    @Override
    public Result handle(Type type, Object argument, Result prevResult) {
        Result<Boolean> result = prevResult == null ? Result.of(true, false) : prevResult;
        if(argument instanceof Boolean) {
            result.value = (Boolean)argument;
            result.isDone = !result.value;
            return result;
        }
        logger.error("Cannot process [{}] type", argument.getClass());
        throw new IllegalArgumentException("Cannot process item");
    }

    public static AndFunction instance() {
        return new AndFunction();
    }

}
