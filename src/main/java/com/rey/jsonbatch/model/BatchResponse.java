package com.rey.jsonbatch.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class BatchResponse {

    @JsonProperty("original")
    private Request original;

    @JsonProperty("requests")
    private List<Request> requests;

    @JsonProperty("responses")
    private List<Response> responses;

    public Request getOriginal() {
        return original;
    }

    public void setOriginal(Request original) {
        this.original = original;
    }

    public List<Request> getRequests() {
        return requests;
    }

    public void setRequests(List<Request> requests) {
        this.requests = requests;
    }

    public List<Response> getResponses() {
        return responses;
    }

    public void setResponses(List<Response> responses) {
        this.responses = responses;
    }
}
