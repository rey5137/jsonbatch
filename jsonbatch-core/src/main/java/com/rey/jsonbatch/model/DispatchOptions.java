package com.rey.jsonbatch.model;

public class DispatchOptions {

    private Boolean failBackAsString = false;

    private Boolean ignoreParsingError = false;

    public Boolean getFailBackAsString() {
        return failBackAsString;
    }

    public void setFailBackAsString(Boolean failBackAsString) {
        this.failBackAsString = failBackAsString;
    }

    public Boolean getIgnoreParsingError() {
        return ignoreParsingError;
    }

    public void setIgnoreParsingError(Boolean ignoreParsingError) {
        this.ignoreParsingError = ignoreParsingError;
    }

}
