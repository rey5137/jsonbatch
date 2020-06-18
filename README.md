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
* [Raw String](#raw-string)
* [Array](#array)
* [Where is the data](#where-is-the-data)
* [A real example](#a-real-example)
* [Custom function](#custom-function)
* [Loop requests](#loop-requests)
* [Response transform](#response-transform)

## Getting Started

JsonBatch is available at the Central Maven Repository.
```xml
<dependency>
  <groupId>com.github.rey5137</groupId>
  <artifactId>jsonbatch-core</artifactId>
  <version>1.2.1</version>
</dependency> 

// need to include jsonpath dependency

<dependency>
    <groupId>com.jayway.jsonpath</groupId>
    <artifactId>json-path</artifactId>
    <version>2.4.0</version>
</dependency>
```
We also need to add a sub package that implement RequestDispatcher. You can use this package that use Apache HttpClient:
```xml
<dependencies>
    <dependency>
        <groupId>com.github.rey5137</groupId>
        <artifactId>jsonbatch-apache-httpclient</artifactId>
        <version>1.1.2</version>
    </dependency>

    // need to include httpclient dependency

    <dependency>
        <groupId>org.apache.httpcomponents</groupId>
        <artifactId>httpclient</artifactId>
        <version>4.5.2</version>
    </dependency>
</dependencies>
```
Or this one use OkHttp:
```xml
<dependencies>
    <dependency>
        <groupId>com.github.rey5137</groupId>
        <artifactId>jsonbatch-okhttp</artifactId>
        <version>1.1.2</version>
    </dependency>

    // need to include okhttp dependency

    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>okhttp</artifactId>
        <version>4.7.2</version>
    </dependency>
</dependencies>

```

JsonBatch depends on Jayway JsonPath library to parse json path.

First we have to create a BatchEngine. Below is a simple example:
```java
  Configuration conf = Configuration.builder().build();
  JsonBuilder jsonBuilder = new JsonBuilder(Functions.basic());
  RequestDispatcher requestDispatcher = new ApacheHttpClientRequestDispatcher(HttpClients.createDefault());
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
  },
  "loop_options": {
    "max_loop_time": ...
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
To know how to build a JSON object from template, JsonBatch use a JSON with special format. 
All fields that aren't string will be same when build actual JSON but string field have to follow a specific format: 

**\<data type\> <json_path or function(sum, min, max, ...) or raw_string>**

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
 
 ## Raw String
 For string field, instead of using JsonPath or Function, we can use raw string directly. Note that JsonBatch support inline variable with format: **@{\<schema>}@**
 Some examples:
 <table>
 <tr> <td> Template </td> <td> Result </td> </tr>
 <tr>
 <td>
 
 ```json
 {
     "field_1": "This is a raw string"
 }
 ```
 
 </td>
 <td>
 
 ```json
 {
     "field_1": "This is a raw string"
 }
 ```
 
 </td>
 </tr>
 
 <tr>
 <td>
 
 ```json
 {
     "field_1": "This is a raw string with @{$.key}@ var"
 }
 ```
 
 </td>
 <td>
 
 ```json
 {
     "field_1": "This is a raw string with 1 var"
 }
 ```
 
 </td>
 </tr>
 
 <tr>
 <td>
 
 ```json
 {
     "field_1": "This is a raw string with @{nested @{$.key}@}@ var"
 }
 ```
 
 </td>
 <td>
 
 ```json
 {
     "field_1": "This is a raw string with nested 1 var"
 }
 ```
 
 </td>
 </tr>
 
 </table>
 
 
 
 ## Array
 There is several way you can use to build Json Array:
 
- String field with array data type. The engine will try to cast all array items to expected type.
 
 <table>
 <tr> <td> Template </td> <td> Data </td> <td> Result </td> </tr>
 
 <tr>
 <td>
 
 ```json
 {
     "field_1": "int[] $[*].key_1"
 }
 ```
 
 </td>
 
 <td>
  
 ```json
[
  {
    "key_1": "1"
  },
  {
    "key_1": "2"
  }
]
 ```
 
 </td>
 
 <td>
 
 ```json
 {
     "field_1": [1, 2]
 }
 ```
 
 </td>
 </tr>
 
 </table>
 
- Use array field with string item. The engine will extract values from each child schema and merge all into 1 array. 
   
 <table>
 <tr> <td> Template </td> <td> Data </td> <td> Result </td> </tr>
 
 <tr>
 <td>
 
 ```json
 {
    "field_1": [
      "int[] $[*].key_1",
      "int $[0].key_2"
    ]
 }
 ```
 
 </td>
 
 <td>
  
 ```json
[
  {
    "key_1": "1",
    "key_2": 10
  },
  {
    "key_1": "2"
  }
]
 ```
 
 </td>
 
 <td>
 
 ```json
 {
     "field_1": [1, 2, 10]
 }
 ```
 
 </td>
 </tr>
 
 </table>
 
 - Use array field with object item. Same as string item, but inside child item schema, 
 you need to add **__array_path** key to define the root JsonPath of all child item's field.
    
  <table>
  <tr> <td> Template </td> <td> Data </td> <td> Result </td> </tr>
  
  <tr>
  <td>
  
  ```json
  {
     "field_1": [
        {
          "a": "int $.key_1",
          "__array_path": "$.array[*]"
        }   
     ]
  }
  ```
  
  </td>
  
  <td>
   
  ```json
{
  "array": [
    {
      "key_1": "1",
      "key_2": 10
    },
    {
      "key_1": "2",
      "key_2": 9
    }
  ],
  "other": "..."
}
  ```
  
  </td>
  
  <td>
  
  ```json
  {
    "field_1": [ 
      {
        "a": 1
      },
      {
        "a": 2
      }    
    ]
  }
  ```
  
  </td>
  </tr>
  
  </table>
 
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

You can try out this example via [this web app](https://jsonbatch-demo.herokuapp.com/)

## Custom function
You can add your own custom function to JsonBuilder object. All the functions have to extend from a base class **Function**:
```java
public abstract class Function {

    public abstract String getName();

    public abstract boolean isReduceFunction();

    public Object invoke(Type type, List<Object> arguments) {
        return null;
    }

    public Result handle(Type type, Object argument, Result prevResult) {
        return null;
    }
}
```
There is 2 abstract method you will have to override:
- **getName()**: return the unique name of your function.
- **isReduceFunction()**: define that your function is reduce function or not. 

If your function is a reduce function, then you have to override the **handle()** method also. For reduce function, JsonBuilder will pass argument one by one, along with previous Result object (note that for first time it call **handle()** method, prevResult will be null)

If your function isn't a reduce function, then have to override the **invoke()** method, and JsonBuilder will call this method only one time with all the argument list.

Note that the **type** argument is the expected type of function result, and it can be null. 

You can also use this package that provide extra functions to JsonBatch:
```xml
<dependencies>
    <dependency>
        <groupId>com.github.rey5137</groupId>
        <artifactId>jsonbatch-functions</artifactId>
        <version>1.0.0</version>
    </dependency>

    // need this dependency for BeanShellFunction

    <dependency>
        <groupId>org.apache-extras.beanshell</groupId>
        <artifactId>bsh</artifactId>
        <version>2.0b6</version>
    </dependency>

    // need this dependency for GroovyFunction

    <dependency>
        <groupId>org.codehaus.groovy</groupId>
        <artifactId>groovy</artifactId>
        <version>2.5.12</version>
    </dependency>
</dependencies>
```

## Loop requests
There is a case you want to loop some request until you found an expected response. JsonBatch also support this.
Below is an example template:
```json
{
    "requests": [
        {
            "loop": {
                "counter_init": 0,
                "counter_predicate": "__cmp(\"@{$.requests[0].counter}@ < 3\")",
                "counter_update": "$.requests[0].times.length()",
                "requests": [
                    {
                        "predicate": "...",
                        "http_method": "GET",
                        "url": "https://test.com/@{$.requests[0].counter}@",
                        "body": {}
                    },
                    ...
                ]
            }
        }
    ],
    "loop_options": {
        "max_loop_time": 10
    }
}
```
As you can see, inside the first request template, we have a new object **loop** to define the loop request.
- **counter_init**: A schema to initiate a counter object for your loop request. The counter object will be stored and can be accessed later via JsonPath.
- **counter_predicate**: A predicate schema that should return Boolean object. The loop will run as long as this predicate return true.
- **counter_update**: A schema to update the counter object each time the loop run.
- **requests**: A list of request template will be executed each time.

Next is the sample Batch response JSON for above template:
```json
{
  "original": {},
  "requests": [
    {
      "times": [
        [ {
            "http_method": "GET",
            "url": "https://test.com/0",
            "headers": {},
            "body": {}
        } ],
        [ {
            "http_method": "GET",
            "url": "https://test.com/1",
            "headers": {},
            "body": {}
        } ],
        [ {
            "http_method": "GET",
            "url": "https://test.com/2",
            "headers": {},
            "body": {}
        } ]
      ],
      "counter": 3
    }
  ],
  "responses": [
      {
        "times": [
          [ {
              "status": 200,
              "headers": {},
              "body": {}
          } ],
          [ {
              "status": 200,
              "headers": {},
              "body": {}
          } ],
          [ {
              "status": 200,
              "headers": {},
              "body": {}
          } ]
        ]
      }
  ]
}
```

The structure of the loop request is different with single request:
- **times**: An array contains requests of each loop time. 
- **counter**: the counter object of loop request.

The same structure also applied to loop response.

Note that loop request is powerful feature, but also can be misconfigured easily, that lead to an endless loop. 
To avoid this issue, JsonBatch use a config **max_loop_time**  (default is 10). 
If a loop ran too many times and surpassed this config, the Engine will forcefully break the loop.

## Response transform
By default, the Engine will put all the response data into the grand JSON. 
But if you want to only keep some interested data and discard the rest of the response (to make it more memory-friendly),
then you can supply a list of transformers inside the request template. 
```json
{
  "requests": [
      {
        "predicate": "...",
        "http_method": "...",
        "url": "...",
        "headers": { ... },
        "body": { ... },
        "transformers": [
          {
            "predicate": "...",
            "status": "...",
            "headers": { ... },
            "body": { ... }
          },
          ...
        ],
        "requests": [  ... <next requests> ... ],
        "responses": [ ... <response templates> ... ]
      },
      ...
  ]
  ...
}
```  
The transformer template is same as response template, the only different is the JSON object it'll work on (the root level of JsonPath will be different). 
Transformer template works on each corresponding JSON response, but response template works on the grand JSON that constains all data. 
