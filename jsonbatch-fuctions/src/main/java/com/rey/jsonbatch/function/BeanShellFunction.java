package com.rey.jsonbatch.function;

import bsh.EvalError;
import bsh.Interpreter;
import com.rey.jsonbatch.JsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class BeanShellFunction extends Function {

    private Logger logger = LoggerFactory.getLogger(BeanShellFunction.class);

    private Interpreter interpreter = new Interpreter();

    @Override
    public String getName() {
        return "beanshell";
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
        try {
            return interpreter.eval(expression);
        } catch (EvalError error) {
            throw new RuntimeException(error);
        }
    }

    public static BeanShellFunction instance() {
        return new BeanShellFunction();
    }

}
