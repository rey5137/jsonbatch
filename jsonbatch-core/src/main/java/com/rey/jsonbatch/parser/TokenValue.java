package com.rey.jsonbatch.parser;

import java.util.Objects;

public class TokenValue {

    private Token token;
    private String value;

    private TokenValue(Token token, String value) {
        this.token = token;
        this.value = value;
    }

    public Token getToken() {
        return token;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenValue that = (TokenValue) o;
        return token == that.token &&
                Objects.equals(value, that.value);
    }

    public static TokenValue of(Token token, String value) {
        return new TokenValue(token, value);
    }

    public static TokenValue of(Token token) {
        return new TokenValue(token, null);
    }

}
