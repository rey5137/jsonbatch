package com.rey.jsonbatch.model;

import java.util.Map;

public class VarTemplate {

    private Object predicate;

    private Map<String, Object> vars;

    public Object getPredicate() {
        return predicate;
    }

    public void setPredicate(Object predicate) {
        this.predicate = predicate;
    }

    public Map<String, Object> getVars() {
        return vars;
    }

    public void setVars(Map<String, Object> vars) {
        this.vars = vars;
    }

}
