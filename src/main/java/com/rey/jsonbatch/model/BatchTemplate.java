package com.rey.jsonbatch.model;

import java.util.List;

public class BatchTemplate {

    private List<RequestTemplate> requests;

    private ResponseTemplate response;

    public List<RequestTemplate> getRequests() {
        return requests;
    }

    public void setRequests(List<RequestTemplate> requests) {
        this.requests = requests;
    }

    public ResponseTemplate getResponse() {
        return response;
    }

    public void setResponse(ResponseTemplate response) {
        this.response = response;
    }
}
