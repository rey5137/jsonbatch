package com.rey.jsonbatch.function;

import com.rey.jsonbatch.JsonBuilder;
import groovy.util.Eval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GroovyFunction extends Function {

    private Logger logger = LoggerFactory.getLogger(GroovyFunction.class);

    @Override
    public String getName() {
        return "groovy";
    }

    @Override
    public boolean isReduceFunction() {
        return false;
    }

    @Override
    public Object invoke(JsonBuilder.Type type, List<Object> arguments) {
        if (arguments.size() != 1) {
            logger.error("Expect only one argument");
            throw new IllegalArgumentException("Invalid arguments size");
        }
        if (!(arguments.get(0) instanceof String)) {
            logger.error("Expect string argument");
            throw new IllegalArgumentException("Invalid argument type: " + arguments.get(0).getClass());
        }
        String expression = (String) arguments.get(0);
        return Eval.me(expression);
    }

    public static GroovyFunction instance() {
        return new GroovyFunction();
    }

}
