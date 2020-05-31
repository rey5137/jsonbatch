package com.rey.jsonbatch.apachehttpclient;

import com.jayway.jsonpath.spi.json.JsonProvider;
import com.rey.jsonbatch.RequestDispatcher;
import com.rey.jsonbatch.model.Request;
import com.rey.jsonbatch.model.Response;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApacheHttpClientRequestDispatcher implements RequestDispatcher {

    private HttpClient httpClient;

    public ApacheHttpClientRequestDispatcher(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public Response dispatch(Request request, JsonProvider jsonProvider) throws IOException {
        RequestBuilder requestBuilder = RequestBuilder.create(request.getHttpMethod().toUpperCase());
        requestBuilder.setUri(request.getUrl());
        request.getHeaders().forEach((key, values) -> values.forEach(value -> requestBuilder.addHeader(key, value)));
        if(request.getBody() != null) {
            String json = jsonProvider.toJson(request.getBody());
            requestBuilder.setEntity(new StringEntity(json));
        }
        HttpResponse httpResponse = httpClient.execute(requestBuilder.build());
        Response response = new Response();
        Map<String, List<String>> headerMap = new HashMap<>();
        for(Header header : httpResponse.getAllHeaders()) {
            headerMap.computeIfAbsent(header.getName(), key -> new ArrayList<>()).add(header.getValue());
        }
        response.setStatus(httpResponse.getStatusLine().getStatusCode());
        response.setHeaders(headerMap);
        Header contentEncodingHeader = httpResponse.getEntity().getContentEncoding();
        response.setBody(jsonProvider.parse(httpResponse.getEntity().getContent(), contentEncodingHeader == null ? "UTF-8" : contentEncodingHeader.getValue()));
        return response;
    }

}
