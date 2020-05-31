package com.rey.jsonbatch.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Response {

    private Integer status;

    private Map<String, List<String>> headers;

    private Object body;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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
        map.put("status", status);
        map.put("headers", headers);
        map.put("body", body);
        return map;
    }

}
