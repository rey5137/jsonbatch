package com.rey.jsonbatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.rey.jsonbatch.function.AverageFunction;
import com.rey.jsonbatch.function.MaxFunction;
import com.rey.jsonbatch.function.MinFunction;
import com.rey.jsonbatch.function.RegexFunction;
import com.rey.jsonbatch.function.SumFunction;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class JsonBuilderTest {

    private JsonBuilder jsonBuilder;

    private DocumentContext documentContext;

    @Before
    public void setUp() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Configuration conf = Configuration.builder()
                .jsonProvider(new JacksonJsonProvider(objectMapper))
                .mappingProvider(new JacksonMappingProvider(objectMapper))
                .build();
        jsonBuilder = new JsonBuilder(SumFunction.instance(),
                AverageFunction.instance(),
                MinFunction.instance(),
                MaxFunction.instance(),
                RegexFunction.instance());
        String data = objectMapper.writeValueAsString(buildData());
        documentContext = JsonPath.using(conf).parse(data);
    }

    @Test
    public void buildNode__stringType() {
        String schema = "str $[0].first";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals("str1", result);
    }

    @Test
    public void buildNode__stringType__diffValueType() {
        String schema = "str $[1].third";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals("2.5", result);
    }

    @Test
    public void buildNode__stringType__fromList() {
        String schema = "str $[*].fourth";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals("true", result);
    }

    @Test
    public void buildNode__integerType() {
        String schema = "int $[0].second";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals(1L, result);
    }

    @Test
    public void buildNode__integerType__fromStringValue() {
        String schema = "int $[3].fifth";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals(5L, result);
    }

    @Test
    public void buildNode__integerType__fromFloatValue() {
        String schema = "int $[0].third";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals(2L, result);
    }

    @Test
    public void buildNode__numberType() {
        String schema = "num $[0].third";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals(new BigDecimal(1.5F), result);
    }

    @Test
    public void buildNode__numberType__fromIntegerValue() {
        String schema = "num $[1].second";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals(new BigDecimal(2), result);
    }

    @Test
    public void buildNode__numberType__fromStringValue() {
        String schema = "num $[3].fifth";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals(new BigDecimal(5), result);
    }

    @Test
    public void buildNode__booleanType() {
        String schema = "bool $[0].fourth";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals(true, result);
    }

    @Test
    public void buildNode__booleanType__fromIntegerValue() {
        String schema = "bool $[4].second";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals(false, result);
    }

    @Test
    public void buildNode__booleanType__fromFloatValue() {
        String schema = "bool $[0].third";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals(true, result);
    }

    @Test
    public void buildNode__booleanType__fromStringValue() {
        String schema = "bool $[4].fifth";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals(true, result);
    }

    @Test
    public void buildNode__objectType() {
        String schema = "obj $[1]";
        Map<String, Object> result = (Map<String, Object>)jsonBuilder.build(schema, documentContext);
        Data data = buildData().get(1);
        assertEquals(data.first, result.get("first"));
        assertEquals(data.second.intValue(), (int)result.get("second"));
        assertEquals(data.third, result.get("third"));
        assertEquals(data.fourth, result.get("fourth"));
        assertEquals(data.fifth, result.get("fifth"));
    }

    @Test
    public void buildNode__sumFunction__intValue() {
        String schema = "int __sum(\"$[*].second\")";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals(10L, result);
    }

    @Test
    public void buildNode__sumFunction__decimalValue() {
        String schema = "num __sum(\"$[*].third\")";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals(new BigDecimal(17.5), result);
    }

    @Test
    public void buildNode__averageFunction__intValue() {
        String schema = "int __average(\"$[*].second\")";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals(2L, result);
    }

    @Test
    public void buildNode__averageFunction__decimalValue() {
        String schema = "num __average(\"$[*].third\")";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals(new BigDecimal(3.5), result);
    }

    @Test
    public void buildNode__minFunction__intValue() {
        String schema = "int __min(\"$[*].second\")";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals(0L, result);
    }

    @Test
    public void buildNode__minFunction__decimalValue() {
        String schema = "num __min(\"$[*].third\")";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals(new BigDecimal(1.5), result);
    }

    @Test
    public void buildNode__maxFunction__intValue() {
        String schema = "int __max(\"$[*].second\")";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals(4L, result);
    }

    @Test
    public void buildNode__maxFunction__decimalValue() {
        String schema = "num __max(\"$[*].third\")";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals(new BigDecimal(5.5), result);
    }

    @Test
    public void buildNode__regexFunction() {
        String schema = "str __regex(\"$[0].first\", \"^str(\\d)$\", 1)";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals("1", result);
    }

    @Test
    public void buildNode__rawString__inlineVariable() {
        String schema = "str asd @{str $[0].first}@ qwe @{int __sum(\"$[*].second\")}@ zxc";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals("asd str1 qwe 10 zxc", result);
    }

    @Test
    public void buildObject() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("first", "int $[?(@.first == 'str2')].second");
        schema.put("second", "str[] $[?(@.fourth == false)].first");

        Map<String, Object> result = (Map<String, Object>)jsonBuilder.build(schema, documentContext);

        assertEquals(2L, result.get("first"));
        assertArray((List)result.get("second"), "str2", "str5");
    }

    @Test
    public void buildArray() {
        Map<String, Object> childSchema = new HashMap<>();
        childSchema.put("first", "int $.second");
        childSchema.put("second", "str $.fourth");
        childSchema.put("__array_path", "$");

        List<Map<String, Object>> result = (List<Map<String, Object>>)jsonBuilder.build(Collections.singletonList(childSchema), documentContext);

        assertEquals(5, result.size());
        assertEquals(1L, result.get(0).get("first"));
        assertEquals("true", result.get(0).get("second"));
        assertEquals(2L, result.get(1).get("first"));
        assertEquals("false", result.get(1).get("second"));
        assertEquals(3L, result.get(2).get("first"));
        assertEquals(null, result.get(2).get("second"));
        assertEquals(4L, result.get(3).get("first"));
        assertEquals(null, result.get(3).get("second"));
        assertEquals(0L, result.get(4).get("first"));
        assertEquals("false", result.get(4).get("second"));
    }

    @Test
    public void buildArray__multiItems() {
        Map<String, Object> childSchema = new HashMap<>();
        childSchema.put("first", "int $.second");
        childSchema.put("__array_path", "$[?(@.fourth == true)]");

        Map<String, Object> secondChildSchema = new HashMap<>();
        secondChildSchema.put("first", "str $.first");
        secondChildSchema.put("__array_path", "$[?(@.fourth == false)]");

        List<Map<String, Object>> result = (List<Map<String, Object>>)jsonBuilder.build(Arrays.asList(childSchema, secondChildSchema), documentContext);

        assertEquals(3, result.size());
        assertEquals(1L, result.get(0).get("first"));
        assertEquals("str2", result.get(1).get("first"));
        assertEquals("str5", result.get(2).get("first"));
    }

    private void assertArray(List<Object> actuals, Object... expected) {
        assertEquals("Expected array length is " + expected.length + " but actual is " + actuals.size(), expected.length, actuals.size());
        for(int i = 0; i < expected.length; i++) {
            assertEquals("Expected element " + i + " is " + expected[i] + " but actual is " + actuals.get(i), expected[i], actuals.get(i));
        }
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