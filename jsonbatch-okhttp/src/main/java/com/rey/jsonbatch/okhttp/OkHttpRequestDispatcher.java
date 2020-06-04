package com.rey.jsonbatch.okhttp;

import com.jayway.jsonpath.spi.json.JsonProvider;
import com.rey.jsonbatch.RequestDispatcher;
import com.rey.jsonbatch.model.DispatchOptions;
import com.rey.jsonbatch.model.Request;
import com.rey.jsonbatch.model.Response;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OkHttpRequestDispatcher implements RequestDispatcher {

    private Logger logger = LoggerFactory.getLogger(OkHttpRequestDispatcher.class);

    private OkHttpClient okHttpClient;

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public OkHttpRequestDispatcher(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
    }

    @Override
    public Response dispatch(Request request, JsonProvider jsonProvider, DispatchOptions options) throws Exception {
        okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder();
        logger.debug("Request {}: {}", request.getHttpMethod(), request.getUrl());
        requestBuilder.url(request.getUrl());
        request.getHeaders().forEach((key, values) -> values.forEach(value -> requestBuilder.addHeader(key, value)));
        if(request.getBody() != null) {
            String json = jsonProvider.toJson(request.getBody());
            logger.debug("Request body: {}", json);
            requestBuilder.method(request.getHttpMethod(), RequestBody.create(json, JSON));
        }
        else
            requestBuilder.method(request.getHttpMethod(), null);

        try (okhttp3.Response httpResponse = okHttpClient.newCall(requestBuilder.build()).execute()) {
            Response response = new Response();
            response.setStatus(httpResponse.code());
            if(options.getFailBackAsString())
                try {
                    String bodyAsString = httpResponse.body().string();
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
                    response.setBody(jsonProvider.parse(httpResponse.body().byteStream(), "UTF-8"));
                }
                catch (Exception ex) {
                    logger.warn("Cannot parse response body as JSON", ex);
                    if(!options.getIgnoreParsingError())
                        throw ex;
                }

            return response;
        }
    }
}
