package com.rey.jsonbatch.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RequestTemplate {

    @JsonProperty("http_method")
    private String httpMethod;

    @JsonProperty("url")
    private String url;

    @JsonProperty("headers")
    private Object headers;

    @JsonProperty("body")
    private Object body;

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

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
