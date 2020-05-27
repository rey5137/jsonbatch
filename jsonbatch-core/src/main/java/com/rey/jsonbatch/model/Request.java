package com.rey.jsonbatch.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Request {

    private String httpMethod;

    private String url;

    private Map<String, List<String>> headers;

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

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("http_method", httpMethod);
        map.put("url", url);
        map.put("headers", headers);
        map.put("body", body);
        return map;
    }

}
