package com.rey.jsonbatch.function;

import com.jayway.jsonpath.DocumentContext;
import com.rey.jsonbatch.JsonBuilder;
import com.rey.jsonbatch.Logger;

import java.util.List;

public interface JsonFunction {

    String getName();

    List<JsonBuilder.Type> supportedTypes();

    Object handle(JsonBuilder jsonBuilder, JsonBuilder.Type type, String arguments, DocumentContext context, Logger logger);

}
