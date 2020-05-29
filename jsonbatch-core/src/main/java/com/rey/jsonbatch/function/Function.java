package com.rey.jsonbatch.function;

import com.rey.jsonbatch.JsonBuilder.Type;

import java.util.List;

public interface Function {

    String getName();

    Object invoke(Type type, List<Object> arguments);

}
