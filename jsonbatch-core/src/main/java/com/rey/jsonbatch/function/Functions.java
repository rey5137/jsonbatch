package com.rey.jsonbatch.function;

public class Functions {

    public static Function[] basic() {
        return new Function[]{
                SumFunction.instance(),
                MinFunction.instance(),
                MaxFunction.instance(),
                AverageFunction.instance(),
                CompareFunction.instance(),
                AndFunction.instance(),
                OrFunction.instance(),
                RegexFunction.instance()
        };
    }
}
