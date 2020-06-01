package com.rey.jsonbatch;

import com.jayway.jsonpath.spi.json.JsonProvider;
import com.rey.jsonbatch.model.DispatchOptions;
import com.rey.jsonbatch.model.Request;
import com.rey.jsonbatch.model.Response;

public interface RequestDispatcher {

    Response dispatch(Request request, JsonProvider jsonProvider, DispatchOptions options) throws Exception;

}
