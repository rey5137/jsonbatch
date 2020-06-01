package com.rey.jsonbatch.apachehttpclient;

import com.jayway.jsonpath.spi.json.JsonProvider;
import com.rey.jsonbatch.RequestDispatcher;
import com.rey.jsonbatch.model.DispatchOptions;
import com.rey.jsonbatch.model.Request;
import com.rey.jsonbatch.model.Response;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApacheHttpClientRequestDispatcher implements RequestDispatcher {

    private Logger logger = LoggerFactory.getLogger(ApacheHttpClientRequestDispatcher.class);

    private HttpClient httpClient;

    public ApacheHttpClientRequestDispatcher(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public Response dispatch(Request request, JsonProvider jsonProvider, DispatchOptions options) throws Exception {
        RequestBuilder requestBuilder = RequestBuilder.create(request.getHttpMethod().toUpperCase());
        requestBuilder.setUri(request.getUrl());
        logger.debug("Request {}: {}", request.getHttpMethod(), request.getUrl());
        request.getHeaders().forEach((key, values) -> values.forEach(value -> requestBuilder.addHeader(key, value)));
        if(request.getBody() != null) {
            String json = jsonProvider.toJson(request.getBody());
            logger.debug("Request body: {}", json);
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
        String charsetName = contentEncodingHeader == null ? "UTF-8" : contentEncodingHeader.getValue();

        if(options.getFailBackAsString())
            try {
                String bodyAsString = readString(httpResponse.getEntity().getContent(), charsetName);
                response.setBody(bodyAsString);
                try {
                    response.setBody(jsonProvider.parse(bodyAsString));
                }
                catch (Exception ex) {
                    logger.warn("Cannot parse response body as JSON", ex);
                }
            }
            catch (Exception e) {
                logger.warn("Cannot parse response body as String", e);
                if(!options.getIgnoreParsingError())
                    throw e;
            }
        else
            try {
                response.setBody(jsonProvider.parse(httpResponse.getEntity().getContent(), charsetName));
            }
            catch (Exception ex) {
                logger.warn("Cannot parse response body as JSON", ex);
                if(!options.getIgnoreParsingError())
                    throw ex;
            }
        return response;
    }

    private String readString(InputStream inputStream, String charset) throws IOException {
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[1024];
        try (Reader reader = new BufferedReader(new InputStreamReader(inputStream, charset))) {
            int c;
            while((c = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, c);
            }
        }
        return builder.toString();
    }

}
