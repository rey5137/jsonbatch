package com.rey.jsonbatch.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class Response {

    @JsonProperty("headers")
    private Map<String, Object> headers;

    @JsonProperty("body")
    private Object body;

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers;
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }
}
