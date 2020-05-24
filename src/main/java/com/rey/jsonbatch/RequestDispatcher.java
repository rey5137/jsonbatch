package com.rey.jsonbatch;

import com.rey.jsonbatch.model.Request;
import com.rey.jsonbatch.model.Response;

public interface RequestDispatcher {

    Response dispatch(Request request);

}
