package com.rey.jsonbatch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.rey.jsonbatch.model.BatchResponse;
import com.rey.jsonbatch.model.BatchTemplate;
import com.rey.jsonbatch.model.Request;
import com.rey.jsonbatch.model.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class BatchEngine {

    private ObjectMapper objectMapper;
    private Configuration configuration;
    private JsonBuilder jsonBuilder;
    private RequestDispatcher requestDispatcher;

    public BatchEngine(ObjectMapper objectMapper,
                       Configuration configuration,
                       JsonBuilder jsonBuilder,
                       RequestDispatcher requestDispatcher) {
        this.objectMapper = objectMapper;
        this.configuration = configuration;
        this.jsonBuilder = jsonBuilder;
        this.requestDispatcher = requestDispatcher;
    }

    public Response execute(Request originalRequest, BatchTemplate template) throws Exception {
        BatchResponse batchResponse = new BatchResponse();
        batchResponse.setOriginal(originalRequest);
        batchResponse.setRequests(new ArrayList<>());
        batchResponse.setResponses(new ArrayList<>());
        DocumentContext context = JsonPath.using(configuration).parse(objectMapper.writeValueAsString(batchResponse));
        for(int i = 0; i < template.getRequests().size(); i++ ) {
            Request request = buildRequest(template.getRequests().get(i), context);
            Response response = requestDispatcher.dispatch(request);
            batchResponse.getRequests().add(request);
            batchResponse.getResponses().add(response);
            context = JsonPath.using(configuration).parse(objectMapper.writeValueAsString(batchResponse));
        }

        if(template.getResponse() != null)
            return buildResponse(template.getResponse(), context);

        return buildResponse(batchResponse);
    }

    private Request buildRequest(Request template, DocumentContext context) {
        Request request = new Request();
        request.setHttpMethod(template.getHttpMethod());
        request.setUrl(buildURL(template.getUrl(), context));
        if(template.getBody() != null) {
            request.setBody(jsonBuilder.build(template.getBody(), context));
        }
        if(template.getHeaders() != null) {
            request.setHeaders((Map<String, Object>)jsonBuilder.build(template.getHeaders(), context));
        }
        else {
            request.setHeaders(new HashMap<>());
        }
        return request;
    }

    private Response buildResponse(Response template, DocumentContext context) {
        Response response = new Response();
        if(template.getBody() != null) {
            response.setBody(jsonBuilder.build(template.getBody(), context));
        }
        if(template.getHeaders() != null) {
            response.setHeaders((Map<String, Object>)jsonBuilder.build(template.getHeaders(), context));
        }
        return response;
    }

    private Response buildResponse(BatchResponse batchResponse) {
        Response response = new Response();
        response.setBody(batchResponse);
        return response;
    }

    private String buildURL(String template, DocumentContext context) {
       return (String)jsonBuilder.build("str " + template, context);
    }

}
