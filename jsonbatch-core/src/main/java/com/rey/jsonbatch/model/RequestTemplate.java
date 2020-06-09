package com.rey.jsonbatch.model;

import java.util.List;

public class RequestTemplate {

    private String predicate;

    private String httpMethod;

    private String url;

    private Object headers;

    private Object body;

    private List<RequestTemplate> requests;

    private List<ResponseTemplate> responses;

    private LoopTemplate loop;

    public String getPredicate() {
        return predicate;
    }

    public void setPredicate(String predicate) {
        this.predicate = predicate;
    }

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

    public List<RequestTemplate> getRequests() {
        return requests;
    }

    public void setRequests(List<RequestTemplate> requests) {
        this.requests = requests;
    }

    public List<ResponseTemplate> getResponses() {
        return responses;
    }

    public void setResponses(List<ResponseTemplate> responses) {
        this.responses = responses;
    }

    public LoopTemplate getLoop() {
        return loop;
    }

    public void setLoop(LoopTemplate loop) {
        this.loop = loop;
    }
}
