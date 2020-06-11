package com.rey.jsonbatch.model;

import java.util.List;

public class LoopTemplate {

    private Object counterInit;

    private Object counterPredicate;

    private Object counterUpdate;

    private List<RequestTemplate> requests;

    public Object getCounterInit() {
        return counterInit;
    }

    public void setCounterInit(Object counterInit) {
        this.counterInit = counterInit;
    }

    public Object getCounterPredicate() {
        return counterPredicate;
    }

    public void setCounterPredicate(Object counterPredicate) {
        this.counterPredicate = counterPredicate;
    }

    public Object getCounterUpdate() {
        return counterUpdate;
    }

    public void setCounterUpdate(Object counterUpdate) {
        this.counterUpdate = counterUpdate;
    }

    public List<RequestTemplate> getRequests() {
        return requests;
    }

    public void setRequests(List<RequestTemplate> requests) {
        this.requests = requests;
    }
}
