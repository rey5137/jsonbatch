package com.rey.jsonbatch.model;

import java.util.List;

public class LoopTemplate {

    private String counterInit;

    private String counterPredicate;

    private String counterUpdate;

    private List<RequestTemplate> loopRequests;

    public String getCounterInit() {
        return counterInit;
    }

    public void setCounterInit(String counterInit) {
        this.counterInit = counterInit;
    }

    public String getCounterPredicate() {
        return counterPredicate;
    }

    public void setCounterPredicate(String counterPredicate) {
        this.counterPredicate = counterPredicate;
    }

    public String getCounterUpdate() {
        return counterUpdate;
    }

    public void setCounterUpdate(String counterUpdate) {
        this.counterUpdate = counterUpdate;
    }

    public List<RequestTemplate> getLoopRequests() {
        return loopRequests;
    }

    public void setLoopRequests(List<RequestTemplate> loopRequests) {
        this.loopRequests = loopRequests;
    }
}
