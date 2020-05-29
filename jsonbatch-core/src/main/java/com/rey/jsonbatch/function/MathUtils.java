package com.rey.jsonbatch.function;

import java.math.BigDecimal;
import java.math.BigInteger;

public class MathUtils {

    public static int toInteger(Object value) {
        if(value instanceof BigInteger)
            return ((BigInteger)value).intValue();
        else if(value instanceof Integer)
            return ((Integer)value);
        else if(value instanceof Long)
            return ((int)value);
        else if(value instanceof String)
            return Integer.parseInt((String)value);
        else if(value instanceof Float)
            return Math.round((float)value);
        else if(value instanceof Double)
            return (int)Math.round((double)value);
        else if(value instanceof BigDecimal)
            return Math.round(((BigDecimal)value).floatValue());
        return 0;
    }

    public static BigInteger toBigInteger(Object value) {
        if(value instanceof BigInteger)
            return (BigInteger)value;
        else if(value instanceof Integer || value instanceof Long || value instanceof String)
            return new BigInteger(value.toString());
        else if(value instanceof Float)
            return new BigInteger(String.valueOf(Math.round((float)value)));
        else if(value instanceof Double)
            return new BigInteger(String.valueOf(Math.round((double)value)));
        else if(value instanceof BigDecimal)
            return ((BigDecimal)value).toBigInteger();
        return null;
    }
    
    public static BigDecimal toBigDecimal(Object value) {
        if(value instanceof BigDecimal)
            return (BigDecimal)value;
        else if(value instanceof Float || value instanceof Double || value instanceof Integer || value instanceof Long || value instanceof String)
            return new BigDecimal(value.toString());
        return null;
    }

    public static BigInteger min(BigInteger a, BigInteger b) {
        if(a == null)
            return b;
        return a.compareTo(b) <= 0 ? a : b;
    }

    public static BigInteger max(BigInteger a, BigInteger b) {
        if(a == null)
            return b;
        return a.compareTo(b) > 0 ? a : b;
    }

    public static BigDecimal min(BigDecimal a, BigDecimal b) {
        if(a == null)
            return b;
        return a.compareTo(b) <= 0 ? a : b;
    }

    public static BigDecimal max(BigDecimal a, BigDecimal b) {
        if(a == null)
            return b;
        return a.compareTo(b) > 0 ? a : b;
    }

}
