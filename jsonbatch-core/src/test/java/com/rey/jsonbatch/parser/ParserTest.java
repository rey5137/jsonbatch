package com.rey.jsonbatch.parser;

import org.junit.Test;

import java.util.List;

import static com.rey.jsonbatch.TestUtils.assertArray;
import static com.rey.jsonbatch.parser.TokenValue.of;

public class ParserTest {

    private Parser parser = new Parser();

    @Test
    public void parse__jsonPath() {
        List<TokenValue> values = parser.parse("$.body.request   ");
        assertArray(values,
                of(Token.JSON_PATH, "$.body.request"));
    }

    @Test
    public void parse__rawData() {
        List<TokenValue> values = parser.parse( "  123   ");
        assertArray(values,
                of(Token.RAW, "123"));
    }

    @Test
    public void parse__function() {
        List<TokenValue> values = parser.parse("__sum(\"$.body.key\", 123 , \"abc\")");
        assertArray(values,
                of(Token.FUNC, "sum"),
                of(Token.JSON_PATH, "$.body.key"),
                of(Token.RAW, "123"),
                of(Token.RAW, "abc"),
                of(Token.END_FUNC));
    }

    @Test
    public void parse__function__nestedFunc() {
        List<TokenValue> values = parser.parse("__sum(\"qwe\\\"abc\", __avg(\"$.body  \"))");
        assertArray(values,
                of(Token.FUNC, "sum"),
                of(Token.RAW, "qwe\"abc"),
                of(Token.FUNC, "avg"),
                of(Token.JSON_PATH, "$.body"),
                of(Token.END_FUNC),
                of(Token.END_FUNC));
    }

    @Test
    public void parse__function__escapedJsonPath() {
        List<TokenValue> values = parser.parse("__sum(\"\\$.body.key\")");
        assertArray(values,
                of(Token.FUNC, "sum"),
                of(Token.RAW, "$.body.key"),
                of(Token.END_FUNC));
    }

}