JsonBatch
=====================

**An Engine to run batch request with JSON based REST APIs**

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.rey5137/jsonbatch-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.rey5137/jsonbatch-core)
[![Javadoc](https://www.javadoc.io/badge/com.github.rey5137/jsonbatch-core.svg)](http://www.javadoc.io/doc/com.github.rey5137/jsonbatch-core)

* [Getting Started](#getting-started)
* [How it work](#how-it-work)
* [How it build JSON](#how-it-build-json)
* [Data type](#data-type)
* [Function](#function)
* [Raw data](#raw-data)
* [Where is the data](#where-is-the-data)
* [A real example](#a-real-example)

## Getting Started

JsonBatch is available at the Central Maven Repository.
```xml
<dependency>
  <groupId>com.github.rey5137</groupId>
  <artifactId>jsonbatch-core</artifactId>
  <version>1.1.0</version>
</dependency>
```
We also need to add a sub package that implement RequestDispatcher.
```xml
<dependency>
    <groupId>com.github.rey5137</groupId>
    <artifactId>jsonbatch-apache-httpclient</artifactId>
    <version>1.1.0</version>
</dependency>
```

JsonBatch depends on Jayway JsonPath library to parse json path.

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

## How it work
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
(if a request template has predicate field is NULL, it will always be true).
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

## How it build JSON
To know how to build a json object from template, JsonBatch use a json with each value follow a specific format: 

**\<data type\> <json_path or function(sum, min, max, ...) or raw_data>**

For example:
```json
{
  "field_1": "int $.responses[0].body.field_a" 
}
```
The above template means: build a json object with "field_1" is integer, and extract value from json path "$.responses[0].body.field_a"

You can omit the **\<data type>** part like that:
```json
{
  "field_1": "$.responses[0].body.field_a" 
}
```
JsonBatch will use the type of extracted value instead of casting it.


## Data type
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
 
<table>
<tr> <td> Template </td> <td> Value </td> <td> Result </td> </tr>
<tr>
<td>

```json
{
    "field_1": "int $.responses[0].body.field_a"
}
```

</td>
<td> "10" </td>
<td>

```json
{
    "field_1": 10
}
```

</td>
</tr>

<tr>
<td>

```json
{
    "field_1": "int[] $.responses[0].body.field_a"
}
```

</td>
<td> "10" </td>
<td>

```json
{
    "field_1": [ 10 ]
}
```

</td>
</tr>

<tr>
<td>

```json
{
    "field_1": "int $.responses[*].body.field_a"
}
```

</td>
<td> ["9", "10"] </td>
<td>

```json
{
    "field_1": 9
}
```

</td>
</tr>

</table>

## Function
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
 
 ## Raw data
 You can also pass raw data directly to value (in json format). Some examples:
 <table>
 <tr> <td> Template </td> <td> Result </td> </tr>
 <tr>
 <td>
 
 ```json
 {
     "field_1": "int 1"
 }
 ```
 
 </td>
 <td>
 
 ```json
 {
     "field_1": 1
 }
 ```
 
 </td>
 </tr>
 
 <tr>
 <td>
 
 ```json
 {
     "field_1": "obj {\"key\": 1}"
 }
 ```
 
 </td>
 <td>
 
 ```json
 {
     "field_1": {
        "key": 1
     }
 }
 ```
 
 </td>
 </tr>
 
 <tr>
 <td>
 
 ```json
 {
     "field_1": "str abc @{$.key}@"
 }
 ```
 
 </td>
 <td>
 
 ```json
 {
     "field_1": "abc 1"
 }
 ```
 
 </td>
 </tr>
 
 </table>
 
 Note that, for string raw data, we can pass inline variable with format: **@{\<schema>}@**
 
  
 ## Where is the data
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

## A real example
Below is a real BatchTemplate example that work with **https://jsonplaceholder.typicode.com** REST API.
```json
{
    "requests": [
        {
            "http_method": "GET",
            "url": "https://jsonplaceholder.typicode.com/posts",
            "headers": {
                "Accept": "str application/json, */*"
            },
            "body": null,
            "requests": [
                {
                    "http_method": "GET",
                    "url": "https://jsonplaceholder.typicode.com/posts/@{$.responses[0].body[0].id}@",
                    "headers": {
                        "Accept": "str application/json, */*"
                    },
                    "body": null,
                    "requests": [
                        {
                            "http_method": "POST",
                            "url": "https://jsonplaceholder.typicode.com/posts",
                            "headers": {
                                "Content-type": "str application/json; charset=UTF-8"
                            },
                            "body": {
                                "title": "str A new post",
                                "userId": "int $.responses[1].body.userId",
                                "body": "str $.responses[1].body.body"
                            },
                            "responses": [
                                {
                                    "predicate": "__cmp(\"@{$.responses[2].status}@ != 201\")",
                                    "status": "$.responses[2].status",
                                    "headers": null,
                                    "body": {
                                        "first_post": "obj $.responses[1].body",
                                        "new_post": "Error"
                                    }
                                }
                            ]
                        }
                    ]
                }
            ]
        }
    ],
    "responses": [
        {
            "status": "$.responses[2].status",
            "headers": null,
            "body": {
                "first_post": "obj $.responses[1].body",
                "new_post": "obj $.responses[2].body"
            }
        }
    ],
    "dispatch_options": {
        "fail_back_as_string": true,
        "ignore_parsing_error": true
    }
}
```
Let me explain this template:
- First, it will make a GET request to **https://jsonplaceholder.typicode.com/posts** to get list of a post.
- Then, it extract the id of the first post, then make a second GET request to **https://jsonplaceholder.typicode.com/posts/{id}** to get the post details.
- After that, it will make a POST request to **https://jsonplaceholder.typicode.com/posts** to create a new post with userId & body are same as first post.
- If the POST request succeed, it will return a response with both first post & new post. If not (status != 201), it will return a response with new_post = "Error". 
