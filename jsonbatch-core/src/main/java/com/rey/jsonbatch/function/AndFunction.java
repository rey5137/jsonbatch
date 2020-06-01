package com.rey.jsonbatch.function;

import com.rey.jsonbatch.JsonBuilder.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

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
        else if(argument instanceof Collection) {
            result.value = isTrue((Collection) argument);
            result.isDone = !result.value;
            return result;
        }
        logger.error("Cannot process [{}] type", argument.getClass());
        throw new IllegalArgumentException("Cannot process item");
    }

    private Boolean isTrue(Collection<Object> items) {
        for(Object item : items) {
            if(item instanceof Boolean) {
                if(!(Boolean)item)
                    return false;
            }
            else if(item instanceof Collection) {
                if(!isTrue((Collection)item))
                    return false;
            }
            else
                logger.error("Cannot process [{}] type", item.getClass());
        }
        return true;
    }

    public static AndFunction instance() {
        return new AndFunction();
    }

}
