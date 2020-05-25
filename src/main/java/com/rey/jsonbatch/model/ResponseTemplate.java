package com.rey.jsonbatch.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResponseTemplate {

    @JsonProperty("headers")
    private Object headers;

    @JsonProperty("body")
    private Object body;

    public Object getHeaders() {
        return headers;
    }

    public void setHeaders(Object headers) {
        this.headers = headers;
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }
}
