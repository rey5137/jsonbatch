JsonBatch
=====================

**An Engine to run batch request with JSON based REST APIs**

Getting Started
---------------

JsonBatch depend on Jayway JsonPath to parse json path.

First we have to create a BatchEngine. Below is a simple example:
```java
  Configuration conf = Configuration.builder().build();
  JsonBuilder jsonBuilder = new JsonBuilder(Functions.basic());
  RequestDispatcher requestDispatcher = new RequestDispatcher() { ... };
  BatchEngine batchEngine = new BatchEngine(conf, jsonBuilder, requestDispatcher);
```

BatchEngine has only 1 public method: 
```java
  public Response execute(Request originalRequest, BatchTemplate template);
```

By supplying the original request and a template, BatchEngine will construct & execute each request sequentially, then collect all responses and construct the final response.

How it work
--------------
Here is Batch template full JSON format:
```json
{
  "requests": [
      {
        "predicate": "...",
        "http_method": "...",
        "url": "...",
        "headers": { ... },
        "body": { ... },
        "requests": [  ... <next requests> ... ],
        "responses": [ ... <response templates> ... ]
      },
      ...
  ],
  "responses": [
      {
        "predicate": "...",
        "status": "...",
        "headers": { ... },
        "body": { ... }
      },
      ...
  ],
  "dispatch_options": {
    "fail_back_as_string": ...,
    "ignore_parsing_error": ...
  }
}
```  
By start, the Engine will loop though the **requests** list and choose the first template has predicate expression is true. 
The Engine will build request from template, pass it to **RequestDispatcher** to execute request and collect response. 

After that, it will find the first matching template from the responses list of current request template. 
If it found a response template, it will stop execution chain, build and return the response. 
If no matching response template found, the Engine will continue find next request from the requests list of current request template.

After all requests are executed, the Engine will try to find a matching response template from responses list of BatchTemplate.
If a matching response template found, it will build the final response and return it.
If not, it will return a response contains all requests & responses it has collected so far.

When **RequestDispatcher** execute a request, you can pass options via dispatch_options object to instruct it how to handle response:
- fail_back_as_string: If RequestDispatcher cannot parse response body as JSON, it will return as String.
- ignore_parsing_error: Ignore error when parsing response body, and return null instead.

How it build JSON
---------------
To know how to build a json object from template, JsonBatch use a json with each value follow a specific format: **\<data type\> <json_path or function(sum, min, max, ...) or raw_data>**

For example:
```json
{
  "field_1": "int $.responses[0].body.field_a" 
}
```
The above template mean: build a json object with "field_1" is integer, and extract value from json path "$.responses[0].body.field_a"

You can omit the \<data type> part like that:
```json
{
  "field_1": "$.responses[0].body.field_a" 
}
```
JsonBatch will use the type of extracted value instead of casting it.


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
 
 In case the actual type of value is different with wanted type, the Engine will try to convert if possible. Some examples:
 
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
 
 | Function   | Syntax                         | Example                             | Description                                    |
 | :--------- | :----------------------------- | :-----------------------------------| :--------------------------------------------- |
 | sum        | __sum(\<arguments>)      | __sum("$.responses[*].body.field_a")      | Sum all values from int/decimal array          |
 | min        | __min(\<arguments>)      | __min("$.responses[*].body.field_a")      | Get minimum value from int/decimal array       |
 | max        | __max(\<arguments>)      | __max("$.responses[*].body.field_a")      | Get maximum value from int/decimal array       |
 | average    | __average(\<arguments>)  | __average("$.responses[*].body.field_a")  | Calculate average value from int/decimal array |
 | and        | __and(\<arguments>)      | __and("$.responses[*].body.field_a")      | And logical |
 | or         | __or(\<arguments>)       | __or("$.responses[*].body.field_a")       | Or logical |
 | compare    | __cmp("\<expression>")   | __cmp("@{$.field_a}@ > 10")               | Compare 2 value |
 | regex      | __regex("<json_path>", "\<pattern>", \<index>)  | __regex("$.field_a", "(.*)", 1)  | Extract from string by regex pattern and group index |
 
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
 |{                                | {                    |
 |  "field_1": "str abc @{$.key}@" |   "field_1": "abc 1" |
 |}                                | }                    |
 
 Note that, for string raw data, we can pass inline variable with format: **@{\<schema>}@**
 
  
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
        "status": ...,
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