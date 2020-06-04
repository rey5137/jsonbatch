package com.rey.jsonbatch.function;

import org.junit.Test;

import java.util.Collections;

import static com.rey.jsonbatch.function.BeanShellFunction.*;
import static org.junit.Assert.*;

public class BeanShellFunctionTest {

    private BeanShellFunction function = instance();

    @Test
    public void invoke__math() {
        check(2, "1 * 2");
        check(1.5, "(1 + 2.0) / 2");
        check(true, " 2 > 1");
    }

    @Test
    public void invoke__callFunc() {
        check(2, "java.lang.Math.round(1.6F)");
    }

    private void check(Object expectedResult, String expression) {
        Object result = function.invoke(null, Collections.singletonList(expression));
        assertEquals(expectedResult, result);
    }
}