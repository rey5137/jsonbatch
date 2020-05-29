package com.rey.jsonbatch;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestUtils {

    public static <T> void assertArray(List<T> actuals, T... expected) {
        assertEquals("Expected array length is " + expected.length + " but actual is " + actuals.size(), expected.length, actuals.size());
        for(int i = 0; i < expected.length; i++) {
            assertEquals("Expected element " + i + " is " + expected[i] + " but actual is " + actuals.get(i), expected[i], actuals.get(i));
        }
    }

}
