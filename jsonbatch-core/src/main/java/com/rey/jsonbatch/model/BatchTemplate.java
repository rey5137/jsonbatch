package com.rey.jsonbatch.model;

import java.util.List;

public class BatchTemplate {

    private List<RequestTemplate> requests;

    private List<ResponseTemplate> responses;

    private DispatchOptions dispatchOptions;

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

    public DispatchOptions getDispatchOptions() {
        return dispatchOptions;
    }

    public void setDispatchOptions(DispatchOptions dispatchOptions) {
        this.dispatchOptions = dispatchOptions;
    }
}
