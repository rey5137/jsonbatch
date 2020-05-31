package com.rey.jsonbatch.function;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class CompareFunctionTest {

    private CompareFunction function = CompareFunction.instance();

    @Test
    public void invoke__number() {
        assertTrue((Boolean)function.invoke(null, Collections.singletonList("1 >= 1")));
        assertTrue((Boolean)function.invoke(null, Collections.singletonList("1 < 2")));
        assertTrue((Boolean)function.invoke(null, Collections.singletonList("1 != 2")));
        assertTrue((Boolean)function.invoke(null, Collections.singletonList("1.20 == 1.2")));
        assertFalse((Boolean)function.invoke(null, Collections.singletonList("1 > 2")));
        assertFalse((Boolean)function.invoke(null, Collections.singletonList("1 == 2")));
    }

    @Test
    public void invoke__boolean() {
        assertTrue((Boolean)function.invoke(null, Collections.singletonList("true == true")));
        assertTrue((Boolean)function.invoke(null, Collections.singletonList("false == false")));
        assertTrue((Boolean)function.invoke(null, Collections.singletonList("false != true")));
        assertTrue((Boolean)function.invoke(null, Collections.singletonList("true != false")));
        assertFalse((Boolean)function.invoke(null, Collections.singletonList("true == false")));
    }

    @Test
    public void invoke__string() {
        assertTrue((Boolean)function.invoke(null, Collections.singletonList("abc == abc")));
        assertTrue((Boolean)function.invoke(null, Collections.singletonList("abc != qwe")));
        assertTrue((Boolean)function.invoke(null, Collections.singletonList("abc != ab")));
        assertFalse((Boolean)function.invoke(null, Collections.singletonList("abc == qwe")));
    }

}