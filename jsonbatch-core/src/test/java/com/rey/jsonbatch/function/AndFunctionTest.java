package com.rey.jsonbatch.function;

import org.junit.Test;

import static org.junit.Assert.*;

public class AndFunctionTest {

    private AndFunction function = AndFunction.instance();

    @Test
    public void handle__oneArgument() {
        Function.Result<Boolean> result = function.handle(null, true, null);
        assertEquals(true, result.value);

        result = function.handle(null, false, null);
        assertEquals(false, result.value);
    }

    @Test
    public void handle__twoArguments() {
        Function.Result<Boolean> result = function.handle(null, true,
                function.handle(null, true, null));
        assertEquals(true, result.value);
        assertEquals(false, result.isDone);

        result = function.handle(null, false,
                function.handle(null, true, null));
        assertEquals(false, result.value);
        assertEquals(true, result.isDone);
    }

}