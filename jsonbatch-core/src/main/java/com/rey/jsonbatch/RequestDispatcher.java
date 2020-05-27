package com.rey.jsonbatch;

import com.jayway.jsonpath.spi.json.JsonProvider;
import com.rey.jsonbatch.model.Request;
import com.rey.jsonbatch.model.Response;

import java.io.IOException;

public interface RequestDispatcher {

    Response dispatch(Request request, JsonProvider jsonProvider) throws IOException;

}
