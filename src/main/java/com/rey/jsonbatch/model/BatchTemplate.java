package com.rey.jsonbatch.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class BatchTemplate {

    @JsonProperty("requests")
    private List<Request> requests;

    @JsonProperty("response")
    private Response response;

    public List<Request> getRequests() {
        return requests;
    }

    public void setRequests(List<Request> requests) {
        this.requests = requests;
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }
}
