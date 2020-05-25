package com.rey.jsonbatch.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class BatchTemplate {

    @JsonProperty("requests")
    private List<RequestTemplate> requests;

    @JsonProperty("response")
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
