package com.rey.jsonbatch;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.rey.jsonbatch.function.MathUtils;
import com.rey.jsonbatch.model.BatchTemplate;
import com.rey.jsonbatch.model.DispatchOptions;
import com.rey.jsonbatch.model.LoopTemplate;
import com.rey.jsonbatch.model.Request;
import com.rey.jsonbatch.model.RequestTemplate;
import com.rey.jsonbatch.model.Response;
import com.rey.jsonbatch.model.ResponseTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class BatchEngine {

    private Logger logger = LoggerFactory.getLogger(BatchEngine.class);

    private Configuration configuration;
    private JsonBuilder jsonBuilder;
    private RequestDispatcher requestDispatcher;

    private static final String KEY_ORIGINAL = "original";
    private static final String KEY_REQUESTS = "requests";
    private static final String KEY_RESPONSES = "responses";
    private static final String KEY_COUNTER = "counter";
    private static final String KEY_TIMES = "times";


    public BatchEngine(Configuration configuration,
                       JsonBuilder jsonBuilder,
                       RequestDispatcher requestDispatcher) {
        this.configuration = configuration;
        this.jsonBuilder = jsonBuilder;
        this.requestDispatcher = requestDispatcher;
    }

    public Response execute(Request originalRequest, BatchTemplate template) throws Exception {
        logger.info("Start executing batch with [{}] original request", originalRequest);
        DocumentContext context = JsonPath.using(configuration).parse("{}");
        Map<String, Object> jsonContext = context.json();
        jsonContext.put(KEY_ORIGINAL, originalRequest.toMap());
        jsonContext.put(KEY_REQUESTS, new ArrayList<>());
        jsonContext.put(KEY_RESPONSES, new ArrayList<>());
        if (template.getDispatchOptions() == null)
            template.setDispatchOptions(new DispatchOptions());

        Deque<Step> queue = new ArrayDeque<>();
        Step step = buildStep(template.getRequests(), (List)jsonContext.get(KEY_REQUESTS), (List)jsonContext.get(KEY_RESPONSES), context, 0);
        if (step != null)
            queue.push(step);

        while (!queue.isEmpty()) {
            step = queue.pop();

            if (isLoopStep(step)) {
                LoopTemplate loopTemplate = step.requestTemplate.getLoop();
                if (step.loopTime == 0) {
                    Object counter = jsonBuilder.build(loopTemplate.getCounterInit(), context);

                    step.loopRequest = new HashMap<>();
                    step.loopRequest.put(KEY_COUNTER, counter);
                    step.loopRequest.put(KEY_TIMES, new ArrayList<>());
                    step.requests.add(step.loopRequest);

                    step.loopResponse = new HashMap<>();
                    step.loopResponse.put(KEY_TIMES, new ArrayList<>());
                    step.responses.add(step.loopResponse);
                } else {
                    Object counter = jsonBuilder.build(loopTemplate.getCounterUpdate(), context);
                    step.loopRequest.put(KEY_COUNTER, counter);
                }

                Boolean shouldLoop = MathUtils.toBoolean(jsonBuilder.build(loopTemplate.getCounterPredicate(), context));
                if (shouldLoop) {
                    List<Object> nextRequests = new ArrayList<>();
                    List<Object> nextResponses = new ArrayList<>();
                    Step nextStep = buildStep(loopTemplate.getRequests(), nextRequests, nextResponses, context, 0);
                    if (nextStep != null) {
                        List<Object> requestTimes = (List<Object>)step.loopRequest.get(KEY_TIMES);
                        requestTimes.add(nextRequests);
                        List<Object> responseTimes = (List<Object>)step.loopResponse.get(KEY_TIMES);
                        responseTimes.add(nextResponses);
                        queue.push(step);
                        queue.push(nextStep);
                        step.loopTime++;
                        continue;
                    }
                }

                ResponseTemplate responseTemplate = chooseResponseTemplate(step.requestTemplate.getResponses(), context);
                if (responseTemplate != null) {
                    logger.info("Found break response");
                    Response response = buildResponse(responseTemplate, context);
                    logger.info("Done executing batch with [{}] original request", originalRequest);
                    return response;
                }
                step = buildStep(step.requestTemplate.getRequests(), step.requests, step.responses, context, step.index + 1);
                if (step != null)
                    queue.push(step);
            } else {
                Request request = buildRequest(step.requestTemplate, context);
                Response response = requestDispatcher.dispatch(request, configuration.jsonProvider(), template.getDispatchOptions());
                step.requests.add(request.toMap());
                step.responses.add(response.toMap());
                ResponseTemplate responseTemplate = chooseResponseTemplate(step.requestTemplate.getResponses(), context);
                if (responseTemplate != null) {
                    logger.info("Found break response");
                    response = buildResponse(responseTemplate, context);
                    logger.info("Done executing batch with [{}] original request", originalRequest);
                    return response;
                }
                step = buildStep(step.requestTemplate.getRequests(), step.requests, step.responses, context, step.index + 1);
                if (step != null)
                    queue.push(step);
            }
        }

        Response response;
        ResponseTemplate responseTemplate = chooseResponseTemplate(template.getResponses(), context);
        if (responseTemplate != null) {
            logger.info("Found final response");
            response = buildResponse(responseTemplate, context);
        } else {
            logger.info("Not found final response. Return all batch responses");
            response = new Response();
            response.setStatus(200);
            response.setBody(context.json());
        }

        logger.info("Done executing batch with [{}] original request", originalRequest);
        return response;
    }

    private Step buildStep(List<RequestTemplate> requestTemplates, List<Object> requests, List<Object> responses, DocumentContext context, int index) {
        RequestTemplate requestTemplate = chooseRequestTemplate(requestTemplates, context);
        if (requestTemplate == null)
            return null;
        Step step = Step.of(requestTemplate, requests, responses);
        step.index = index;
        return step;
    }

    private RequestTemplate chooseRequestTemplate(List<RequestTemplate> requestTemplates, DocumentContext context) {
        if (requestTemplates == null)
            return null;
        for (RequestTemplate requestTemplate : requestTemplates) {
            if (requestTemplate.getPredicate() == null || MathUtils.toBoolean(jsonBuilder.build(requestTemplate.getPredicate(), context)))
                return requestTemplate;
        }
        return null;
    }

    private ResponseTemplate chooseResponseTemplate(List<ResponseTemplate> responseTemplates, DocumentContext context) {
        if (responseTemplates == null)
            return null;
        for (ResponseTemplate responseTemplate : responseTemplates) {
            if (responseTemplate.getPredicate() == null || MathUtils.toBoolean(jsonBuilder.build(responseTemplate.getPredicate(), context)))
                return responseTemplate;
        }
        return null;
    }

    private Request buildRequest(RequestTemplate template, DocumentContext context) {
        Request request = new Request();
        request.setHttpMethod(jsonBuilder.build(template.getHttpMethod(), context).toString());
        request.setUrl(jsonBuilder.build(template.getUrl(), context).toString());
        if (template.getBody() != null) {
            request.setBody(jsonBuilder.build(template.getBody(), context));
        }
        if (template.getHeaders() != null) {
            request.setHeaders(buildHeaders((Map<String, Object>) jsonBuilder.build(template.getHeaders(), context)));
        } else {
            request.setHeaders(new HashMap<>());
        }
        return request;
    }

    private Response buildResponse(ResponseTemplate template, DocumentContext context) {
        Response response = new Response();
        if (template.getStatus() != null)
            response.setStatus(MathUtils.toInteger(jsonBuilder.build(template.getStatus(), context)));
        else
            response.setStatus(200);
        if (template.getBody() != null)
            response.setBody(jsonBuilder.build(template.getBody(), context));
        if (template.getHeaders() != null)
            response.setHeaders(buildHeaders((Map<String, Object>) jsonBuilder.build(template.getHeaders(), context)));
        return response;
    }

    private Map<String, List<String>> buildHeaders(Map<String, Object> values) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (value instanceof Collection) {
                headers.put(key, (List<String>) ((Collection) value).stream()
                        .map(String::valueOf)
                        .collect(Collectors.toList()));
            } else
                headers.put(key, Collections.singletonList(String.valueOf(value)));
        });
        return headers;
    }

    private boolean isLoopStep(Step step) {
        return step != null && step.requestTemplate.getLoop() != null;
    }

    private static class Step {
        RequestTemplate requestTemplate;
        List<Object> requests;
        List<Object> responses;
        int index;

        Map<String, Object> loopRequest;
        Map<String, Object> loopResponse;
        int loopTime = 0;

        Step(RequestTemplate requestTemplate, List<Object> requests, List<Object> responses) {
            this.requestTemplate = requestTemplate;
            this.requests = requests;
            this.responses = responses;
        }

        private static Step of(RequestTemplate requestTemplate, List<Object> requests, List<Object> responses) {
            return new Step(requestTemplate, requests, responses);
        }

    }
}
