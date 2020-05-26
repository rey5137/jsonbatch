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
Here is Batch template full JSON format:
```json
{
  "requests": [
      {
        "http_method": "...",
        "url": "...",
        "headers": {
          "header_1": "...",
          "header_2": "...",
          ...
        },
        "body": { ... }
      },
      ...
  ],
  "response": {
    "headers": {
      "header_1": "...",
      "header_2": "...",
      ...
    },
    "body": { ... }
  }
}
```  
A Batch template contains 2 fields:
- requests: A list of request templates, each template has instruction how to build a request.
- response: Final response template, that has instruction how to build final response from all collected responses.

Template schema
---------------
To know how to build a json object from template, JsonBatch use a json with each value follow a specific format: **<data type> <json_path / function(sum, min, max, ...) / raw data>**

For example:
```json
{
  "field_1": "int $.responses[0].body.field_a" 
}
```
The above template mean: build a json object with "field_1" is integer, and extract value from json path "$.responses[0].body.field_a"

Data type
---------
| Type                     | Description      |
| :----------------------- | :----------------|
| str, string              | String           |
| int, integer             | Integer          |
| num, number              | Decimal          |
| bool, boolean            | Boolean          |
| obj, object              | Any object       |
| str[], string[]          | String array     |
| int[], integer[]         | Integer array    |
| num[], number[]          | Decimal array    |
| bool[], boolean[]        | Boolean array    |
| obj[], object[]          | Any array        |
 
 In case the actual type of value is different with wanted type, the engine will try to convert if possible. Some examples:
 
 | Template                                         | Value            | Result             |
 | :----------------------------------------------- | :----------------|:-------------------|
 |{                                                 |                  |{                   |
 |  "field_1": "int $.responses[0].body.field_a"    | "10"             |  "field_1": 10     |
 |}                                                 |                  |}                   |
 |--------------------------------------------------|------------------|--------------------|
 |{                                                 |                  |{                   |
 |  "field_1": "int[] $.responses[0].body.field_a"  | "10"             |  "field_1": [10]   |   
 |}                                                 |                  |}                   |
 |--------------------------------------------------|------------------|--------------------|
 |{                                                 |                  |{                   |
 |  "field_1": "int $.responses[*].body.field_a"    | ["9", "10]       |  "field_1": 9      |
 |}                                                 |                  |}                   |


Function
---------
 Instead of extracting value from json path, we can use some function to aggregate value. 
 The syntax is: **\<data type> \_\_\<function name>(\<function arguments>)** (Note that there is prefix "\_\_" before function name).
 
 Below is list of supported function:
 
 | Function   | Supported type      | Syntax                    | Example                                   | Description                                    |
 | :--------- | :------------------ | :------------------------ | :---------------------------------------- | :--------------------------------------------- |
 | sum        | integer, number     | __sum("<json_path>")      | __sum("$.responses[*].body.field_a")      | Sum all values from int/decimal array          |
 | min        | integer, number     | __min("<json_path>")      | __min("$.responses[*].body.field_a")      | Get minimum value from int/decimal array       |
 | max        | integer, number     | __max("<json_path>")      | __max("$.responses[*].body.field_a")      | Get maximum value from int/decimal array       |
 | average    | integer, number     | __average("<json_path>")  | __average("$.responses[*].body.field_a")  | Calculate average value from int/decimal array |
 | regex      | string              | __regex("<json_path>", "\<pattern>", \<index>)  | __regex("$.responses[0].body.field_a", "(.*)", 1)  | Extract from string by regex pattern and group index |
 
 Raw data
 ---------
 You can also pass raw data directly to value (in json format). Some examples:
  
 | Template                        |  Result              |
 | :------------------------------ | :------------------- |
 |{                                | {                    |
 |  "field_1": "int 1"             |   "field_1": 1       |
 |}                                | }                    |
 |-------------------------------- | -------------------- |
 |{                                | {                    |
 |  "field_1": "obj {\"key\": 1}"  |   "field_1": {       |   
 |}                                |        "key": 1      |
 |                                 |    }                 |
 |                                 | }                    |
 |---------------------------------| -------------------- |
 
  
 Where is the data
 -----------------
 So far we know how to build the template, next is to understand where the data that engine extract from. 
 So when BatchEngine execute a request, it will build a grand JSON that contains all original request, all the executed requests and responses.
 Below is format of this JSON:
 ```json
{
  "original": {
    "http_method": "...",
    "url": "...",
    "headers": {
      "header_1": [ "..." ],
      "header_2": [ "..." ],
      ...
    },
    "body": { ... }
  },
  "requests": [
      {
        "http_method": "...",
        "url": "...",
        "headers": {
          "header_1": [ "..." ],
          "header_2": [ "..." ],
          ...
        },
        "body": { ... }
      },
      ...
  ],
  "responses": [
      {
        "headers": {
          "header_1": [ "..." ],
          "header_2": [ "..." ],
          ...
        },
        "body": { ... }
      },
      ...
  ]
}
```  