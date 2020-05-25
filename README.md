Rey JsonBatch
=====================

**An Engine to run batch request with JSON based REST APIs**

Getting Started
---------------

JsonBatch depend on Jayway JsonPath to parse json path.

First we have to create a BatchEngine. Below is a simple example:
```java
  Configuration conf = Configuration.builder().build();
  JsonBuilder jsonBuilder = new JsonBuilder(SumFunction.instance(),
          AverageFunction.instance(),
          MinFunction.instance(),
          MaxFunction.instance());
  RequestDispatcher requestDispatcher = new RequestDispatcher() { ... };
  BatchEngine batchEngine = new BatchEngine(conf, jsonBuilder, requestDispatcher);
```

BatchEngine has only 1 public method: 
```java
  public Response execute(Request originalRequest, BatchTemplate template);
```

By supplying the original request and a template, BatchEngine will construct & execute each request sequentially, then collect all responses and construct the final response.

Batch Template
--------------
A Batch template contains 2 fields:
- requests: A list of request templates, each template has instruction how to build a request.
- response: Final response temmplate, that has instruction how to build final response from all collected responses.

Template schema
---------------



