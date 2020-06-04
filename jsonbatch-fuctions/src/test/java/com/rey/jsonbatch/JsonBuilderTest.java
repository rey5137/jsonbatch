package com.rey.jsonbatch;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.rey.jsonbatch.function.BeanShellFunction;
import com.rey.jsonbatch.function.Function;
import com.rey.jsonbatch.function.Functions;
import com.rey.jsonbatch.function.GroovyFunction;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class JsonBuilderTest {

    private JsonBuilder jsonBuilder;

    private DocumentContext documentContext;

    @Before
    public void setUp() throws Exception {
        Logger root = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.TRACE);

        ObjectMapper objectMapper = new ObjectMapper();
        Configuration conf = Configuration.builder()
                .jsonProvider(new JacksonJsonProvider(objectMapper))
                .mappingProvider(new JacksonMappingProvider(objectMapper))
                .build();
        List<Function> functions = new ArrayList<>();
        Collections.addAll(functions, Functions.basic());
        Collections.addAll(functions, BeanShellFunction.instance(), GroovyFunction.instance());
        jsonBuilder = new JsonBuilder(functions.toArray(new Function[0]));
        String data = objectMapper.writeValueAsString(buildData());
        documentContext = JsonPath.using(conf).parse(data);
    }

    @Test
    public void build__beanshellFunc() {
        String schema = "num __beanshell(\"@{$[0].third}@ - @{$[1].second}@\")";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals(-0.5, result);
    }

    @Test
    public void build__groovyFunc() {
        String schema = "bool __groovy(\"\\\"@{$[0].first}@\\\".equals(\\\"@{$[1].first}@\\\")\")";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals(false, result);
    }

    private List<Data> buildData() {
        return Arrays.asList(
                new Data("str1", 1L, 1.5, true, 2),
                new Data("str2", 2L, 2.5, false, "abc2"),
                new Data("str3", 3L, 3.5, null, true),
                new Data("str4", 4L, 4.5, null, "5"),
                new Data("str5", 0L, 5.5, false, "True")
        );
    }

    private static class Data {

        public String first;
        public Long second;
        public Double third;
        public Boolean fourth;
        public Object fifth;

        public Data(String first, Long second, Double third, Boolean fourth, Object fifth) {
            this.first = first;
            this.second = second;
            this.third = third;
            this.fourth = fourth;
            this.fifth = fifth;
        }

    }
}