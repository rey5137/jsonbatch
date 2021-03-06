package com.rey.jsonbatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.rey.jsonbatch.function.AndFunction;
import com.rey.jsonbatch.function.AverageFunction;
import com.rey.jsonbatch.function.CompareFunction;
import com.rey.jsonbatch.function.MaxFunction;
import com.rey.jsonbatch.function.MinFunction;
import com.rey.jsonbatch.function.OrFunction;
import com.rey.jsonbatch.function.RegexFunction;
import com.rey.jsonbatch.function.SumFunction;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.rey.jsonbatch.TestUtils.assertArray;
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
        jsonBuilder = new JsonBuilder(SumFunction.instance(),
                AverageFunction.instance(),
                MinFunction.instance(),
                MaxFunction.instance(),
                RegexFunction.instance(),
                AndFunction.instance(),
                OrFunction.instance(),
                CompareFunction.instance());
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
        assertEquals(1, result);
    }

    @Test
    public void buildNode__integerType__fromStringValue() {
        String schema = "int $[3].fifth";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals(new BigInteger("5"), result);
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
        assertEquals(1.5, result);
    }

    @Test
    public void buildNode__numberType__fromIntegerValue() {
        String schema = "num $[1].second";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals(2.0F, result);
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
        assertEquals(new BigInteger("10"), result);
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
        assertEquals(new BigInteger("2"), result);
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
        assertEquals(new BigInteger("0"), result);
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
        assertEquals(new BigInteger("4"), result);
    }

    @Test
    public void buildNode__maxFunction__decimalValue() {
        String schema = "num __max(\"$[*].third\")";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals(new BigDecimal(5.5), result);
    }

    @Test
    public void buildNode__regexFunction() {
        String schema = "str __regex(\"$[0].first\", \"^str(\\\\d)$\", 1)";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals("1", result);
    }

    @Test
    public void buildNode__compareFunction__decimalValue() {
        String schema = "__cmp(\"@{$[0].third}@ > @{$[1].third}@\")";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals(false, result);
    }

    @Test
    public void buildNode__andFunction__decimalValue() {
        String schema = "__and(__cmp(\"@{$[0].third}@ <= @{$[1].third}@\"), __cmp(\"@{$[0].fourth}@ == true\"))";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals(true, result);
    }

    @Test
    public void buildNode__rawString__inlineVariable() {
        String schema = "str asd @{$[0].first}@ qwe @{int __sum(\"$[*].second\")}@ zxc";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals("asd str1 qwe 10 zxc", result);
    }

    @Test
    public void buildNode__rawString__nestedVariable() {
        String schema = "str asd @{qwe @{$[0].first}@}@";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals("asd qwe str1", result);
    }

    @Test
    public void buildNode__rawString__nestedFunc() {
        String schema = "str asd @{__regex(\"@{$[0].first}@\", \".*\", 0)}@";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals("asd str1", result);
    }

    @Test
    public void buildNode__rawString__escapedVar() {
        String schema = "str asd @{qwe \\\\@{\\\\}@}@";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals("asd qwe @{}@", result);
    }

    @Test
    public void buildNode__function__escapeQuote() {
        String schema = "str __regex(\"qwe \\\" abc\", \".*\", 0)";
        Object result = jsonBuilder.build(schema, documentContext);
        assertEquals("qwe \" abc", result);
    }

    @Test
    public void buildNode__rawObject() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("a", 1);
        schema.put("b", false);
        schema.put("c", Collections.singletonList(2D));
        Map<String, Object> result = (Map<String, Object>)jsonBuilder.build(schema, documentContext);
        assertEquals(1, result.get("a"));
        assertEquals(false, result.get("b"));
        assertArray((List<Object>)result.get("c"), 2D);
    }

    @Test
    public void buildNode__rawArray() {
        List<Object> schema = Arrays.asList(1, true, "abc");
        List<Object> result = (List<Object>)jsonBuilder.build(schema, documentContext);
        assertArray(result, 1, true, "abc");
    }

    @Test
    public void buildNode__missingType() {
        assertEquals("str1", jsonBuilder.build("$[0].first", documentContext));
        assertEquals(2, jsonBuilder.build("$[1].second", documentContext));
        assertEquals(true, jsonBuilder.build("$[0].fourth", documentContext));
    }

    @Test
    public void buildNode__jsonPathWithInlineVariable() {
        assertEquals("str3", jsonBuilder.build("$[@{$[1].second}@].first", documentContext));
    }

    @Test
    public void buildObject() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("first", "int $[?(@.first == 'str2')].second");
        schema.put("second", "str[] $[?(@.fourth == false)].first");

        Map<String, Object> result = (Map<String, Object>)jsonBuilder.build(schema, documentContext);

        assertEquals(2, result.get("first"));
        assertArray((List)result.get("second"), "str2", "str5");
    }

    @Test
    public void buildObject__withKeyHasInlineVariable() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("key_@{$[0].first}@", 1);
        schema.put("key_@{$[0].second}@", "$[0].third");
        schema.put("key_@{abc", 2);

        Map<String, Object> result = (Map<String, Object>)jsonBuilder.build(schema, documentContext);

        assertEquals(1, result.get("key_str1"));
        assertEquals(1.5, result.get("key_1"));
        assertEquals(2, result.get("key_@{abc"));
    }

    @Test
    public void buildObject__withObjectSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("first", "$.second");
        schema.put("second", "$$[@{$.second}@].first");
        schema.put("__object_schema", "$[?(@.fourth == false)]");

        Map<String, Object> result = (Map<String, Object>)jsonBuilder.build(schema, documentContext);

        assertEquals(2, result.get("first"));
        assertEquals("str3", result.get("second"));
    }

    @Test
    public void buildArray__withStringSchema__withSingleItem() {
        List result = (List)jsonBuilder.build(Collections.singletonList("$[0].second"), documentContext);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0));
    }

    @Test
    public void buildArray__withStringSchema__withMultiItem() {
        List result = (List)jsonBuilder.build(Collections.singletonList("str[] $[*].second"), documentContext);

        assertEquals(5, result.size());
        assertEquals("1", result.get(0));
        assertEquals("2", result.get(1));
        assertEquals("3", result.get(2));
        assertEquals("4", result.get(3));
        assertEquals("0", result.get(4));
    }

    @Test
    public void buildArray__withObjectSchema() {
        Map<String, Object> childSchema = new HashMap<>();
        childSchema.put("first", "int $.second");
        childSchema.put("second", "str $.fourth");
        childSchema.put("__array_schema", "$");

        List<Map<String, Object>> result = (List<Map<String, Object>>)jsonBuilder.build(Collections.singletonList(childSchema), documentContext);

        assertEquals(5, result.size());
        assertEquals(1, result.get(0).get("first"));
        assertEquals("true", result.get(0).get("second"));
        assertEquals(2, result.get(1).get("first"));
        assertEquals("false", result.get(1).get("second"));
        assertEquals(3, result.get(2).get("first"));
        assertEquals(null, result.get(2).get("second"));
        assertEquals(4, result.get(3).get("first"));
        assertEquals(null, result.get(3).get("second"));
        assertEquals(0, result.get(4).get("first"));
        assertEquals("false", result.get(4).get("second"));
    }

    @Test
    public void buildArray__withObjectSchema__andArrayPathIsNotString() {
        Map<String, Object> childSchema = new HashMap<>();
        childSchema.put("first", "int $.second");
        childSchema.put("second", "str $.third");
        childSchema.put("__array_schema", Arrays.asList("$[?(@.fourth == true)]", "$[?(@.fifth == true)]"));

        List<Map<String, Object>> result = (List<Map<String, Object>>)jsonBuilder.build(Collections.singletonList(childSchema), documentContext);

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).get("first"));
        assertEquals("1.5", result.get(0).get("second"));
        assertEquals(3, result.get(1).get("first"));
        assertEquals("3.5", result.get(1).get("second"));
    }

    @Test
    public void buildArray__withListSchema() {

        List result = (List)jsonBuilder.build(Collections.singletonList(Collections.singletonList("$[0].second")), documentContext);

        assertEquals(1, result.size());
        List items = (List)result.get(0);
        assertEquals(1, items.size());
        assertEquals(1, items.get(0));
    }

    @Test
    public void buildArray__multiItems() {
        Map<String, Object> childSchema = new HashMap<>();
        childSchema.put("first", "int $.second");
        childSchema.put("__array_schema", "$[?(@.fourth == true)]");

        Map<String, Object> secondChildSchema = new HashMap<>();
        secondChildSchema.put("first", "str $.first");
        secondChildSchema.put("__array_schema", "$[?(@.fourth == false)]");

        List<Map<String, Object>> result = (List<Map<String, Object>>)jsonBuilder.build(Arrays.asList(childSchema, secondChildSchema), documentContext);

        assertEquals(3, result.size());
        assertEquals(1, result.get(0).get("first"));
        assertEquals("str2", result.get(1).get("first"));
        assertEquals("str5", result.get(2).get("first"));
    }

    @Test
    public void buildArray__withObjectAndRootContextJsonPath() {
        Map<String, Object> childSchema = new HashMap<>();
        childSchema.put("first", "$.second");
        childSchema.put("second", "$$[@{$.second}@].first");
        childSchema.put("__array_schema", "$[?(@.fourth == false)]");

        List<Map<String, Object>> result = (List<Map<String, Object>>)jsonBuilder.build(Collections.singletonList(childSchema), documentContext);

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).get("first"));
        assertEquals("str3", result.get(0).get("second"));
        assertEquals(0, result.get(1).get("first"));
        assertEquals("str1", result.get(1).get("second"));
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