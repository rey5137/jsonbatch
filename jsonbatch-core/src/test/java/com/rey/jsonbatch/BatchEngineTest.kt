package com.rey.jsonbatch

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import com.jayway.jsonpath.spi.json.JsonProvider
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import com.rey.jsonbatch.TestUtils.assertArray
import com.rey.jsonbatch.function.Functions
import com.rey.jsonbatch.model.BatchTemplate
import com.rey.jsonbatch.model.DispatchOptions
import com.rey.jsonbatch.model.Request
import com.rey.jsonbatch.model.Response
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock

class BatchEngineTest {

    private lateinit var batchEngine: BatchEngine

    private lateinit var requestDispatcherMock: RequestDispatcher

    private lateinit var objectMapper: ObjectMapper

    private lateinit var configuration: Configuration

    @Before
    fun setUp() {
        objectMapper = ObjectMapper()
        objectMapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
        configuration = Configuration.builder()
                .jsonProvider(JacksonJsonProvider(objectMapper))
                .mappingProvider(JacksonMappingProvider(objectMapper))
                .build()
        val jsonBuilder = JsonBuilder(*Functions.basic())
        requestDispatcherMock = mock(RequestDispatcher::class.java)
        batchEngine = BatchEngine(configuration, jsonBuilder, requestDispatcherMock)
    }

    @Test
    fun execute__withLoopRequest() {
        val template = """
            {
                "requests": [
                    {
                        "loop": {
                            "counter_init": 0,
                            "counter_predicate": "__cmp(\"@{$.requests[0].counter}@ < 5\")",
                            "counter_update": "$.requests[0].times.length()",
                            "requests": [
                                {
                                    "http_method": "POST",
                                    "url": "https://localhost.com/@{$.requests[0].counter}@",
                                    "body": {}
                                }
                            ]
                        }
                    }
                ],
                "responses": null
            }
        """.toObj(BatchTemplate::class.java)
        val response = """
            {
                "headers": {},
                "body": {
                    "key": "a"
                }
            }
        """.toObj(Response::class.java)

        doReturn(response).`when`(requestDispatcherMock).dispatch(any(Request::class.java), any(JsonProvider::class.java), any(DispatchOptions::class.java))
        val finalResponse = batchEngine.execute(Request(), template)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(finalResponse))
        val context = JsonPath.using(configuration).parse(finalResponse.body)
        assertEquals(1, context.read("$.requests.length()", Int::class.java))
        assertEquals(5, context.read("$.requests[0].times.length()", Int::class.java))
        assertArray(context.read("$.requests[0].times[*][*].url", List::class.java),
                "https://localhost.com/0", "https://localhost.com/1", "https://localhost.com/2", "https://localhost.com/3", "https://localhost.com/4")
    }

    @Test
    fun execute__withLoopRequest__maxLoopTime() {
        val template = """
            {
                "requests": [
                    {
                        "loop": {
                            "counter_init": 0,
                            "counter_predicate": "__cmp(\"@{$.requests[0].counter}@ < 5\")",
                            "counter_update": "$.requests[0].times.length()",
                            "requests": [
                                {
                                    "http_method": "POST",
                                    "url": "https://localhost.com/@{$.requests[0].counter}@",
                                    "body": {}
                                }
                            ]
                        }
                    }
                ],
                "responses": null,
                "loop_options": {
                    "max_loop_time": 2
                }
            }
        """.toObj(BatchTemplate::class.java)
        val response = """
            {
                "headers": {},
                "body": {
                    "key": "a"
                }
            }
        """.toObj(Response::class.java)

        doReturn(response).`when`(requestDispatcherMock).dispatch(any(Request::class.java), any(JsonProvider::class.java), any(DispatchOptions::class.java))
        val finalResponse = batchEngine.execute(Request(), template)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(finalResponse))
        val context = JsonPath.using(configuration).parse(finalResponse.body)
        assertEquals(1, context.read("$.requests.length()", Int::class.java))
        assertEquals(2, context.read("$.requests[0].times.length()", Int::class.java))
        assertArray(context.read("$.requests[0].times[*][*].url", List::class.java),
                "https://localhost.com/0", "https://localhost.com/1")
    }

    @Test
    fun execute__withLoopRequest__withMultiRequestsEachLoop() {
        val template = """
            {
                "requests": [
                    {
                        "loop": {
                            "counter_init": 0,
                            "counter_predicate": "__cmp(\"@{$.requests[0].counter}@ < 5\")",
                            "counter_update": "$.requests[0].times.length()",
                            "requests": [
                                {
                                    "http_method": "POST",
                                    "url": "https://localhost.com/@{$.requests[0].counter}@",
                                    "body": {},
                                    "requests": [
                                        {
                                            "http_method": "GET",
                                            "url": "https://test.com/@{$.requests[0].counter}@",
                                            "body": {}
                                        }
                                    ]
                                }
                            ]
                        }
                    }
                ],
                "responses": null
            }
        """.toObj(BatchTemplate::class.java)
        val response = """
            {
                "headers": {},
                "body": {
                    "key": "a"
                }
            }
        """.toObj(Response::class.java)

        doReturn(response).`when`(requestDispatcherMock).dispatch(any(Request::class.java), any(JsonProvider::class.java), any(DispatchOptions::class.java))
        val finalResponse = batchEngine.execute(Request(), template)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(finalResponse))
        val context = JsonPath.using(configuration).parse(finalResponse.body)
        assertEquals(1, context.read("$.requests.length()", Int::class.java))
        assertEquals(5, context.read("$.requests[0].times.length()", Int::class.java))
        assertArray(context.read("$.requests[0].times[*][0].url", List::class.java),
                "https://localhost.com/0", "https://localhost.com/1", "https://localhost.com/2", "https://localhost.com/3", "https://localhost.com/4")
        assertArray(context.read("$.requests[0].times[*][1].url", List::class.java),
                "https://test.com/0", "https://test.com/1", "https://test.com/2", "https://test.com/3", "https://test.com/4")
    }

    @Test
    fun execute__withLoopRequest__withNestedLoop() {
        val template = """
            {
                "requests": [
                    {
                        "loop": {
                            "counter_init": 0,
                            "counter_predicate": "__cmp(\"@{$.requests[0].counter}@ < 2\")",
                            "counter_update": "$.requests[0].times.length()",
                            "requests": [
                                {
                                    "loop": {
                                        "counter_init": 0,
                                        "counter_predicate": "__cmp(\"@{$.requests[0].times[-1][0].counter}@ < 2\")",
                                        "counter_update": "$.requests[0].times[-1][0].times.length()",
                                        "requests": [
                                            {
                                                "http_method": "POST",
                                                "url": "https://localhost.com/@{$.requests[0].counter}@/@{$.requests[0].times[-1][0].counter}@",
                                                "body": {}
                                            }
                                        ]
                                    }
                                }
                            ]
                        }
                    }
                ],
                "responses": null
            }
        """.toObj(BatchTemplate::class.java)
        val response = """
            {
                "headers": {},
                "body": {
                    "key": "a"
                }
            }
        """.toObj(Response::class.java)

        doReturn(response).`when`(requestDispatcherMock).dispatch(any(Request::class.java), any(JsonProvider::class.java), any(DispatchOptions::class.java))
        val finalResponse = batchEngine.execute(Request(), template)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(finalResponse))
        val context = JsonPath.using(configuration).parse(finalResponse.body)
        assertEquals(1, context.read("$.requests.length()", Int::class.java))
        assertEquals(2, context.read("$.requests[0].times.length()", Int::class.java))
        assertArray(context.read("$.requests[0].times[0][0].times[*][0].url", List::class.java),
                "https://localhost.com/0/0", "https://localhost.com/0/1")
        assertArray(context.read("$.requests[0].times[1][0].times[*][0].url", List::class.java),
                "https://localhost.com/1/0", "https://localhost.com/1/1")
    }

    @Test
    fun execute__withLoopRequest__andFollowRequest() {
        val template = """
            {
                "requests": [
                    {
                        "loop": {
                            "counter_init": 0,
                            "counter_predicate": "__cmp(\"@{$.requests[0].counter}@ < 3\")",
                            "counter_update": "$.requests[0].times.length()",
                            "requests": [
                                {
                                    "http_method": "POST",
                                    "url": "https://localhost.com/@{$.requests[0].counter}@",
                                    "body": {}
                                }
                            ]
                        },
                        "requests": [
                            {
                                "http_method": "GET",
                                "url": "https://test.com",
                                "body": {}
                            }
                        ]
                    }
                ],
                "responses": null
            }
        """.toObj(BatchTemplate::class.java)
        val response = """
            {
                "headers": {},
                "body": {
                    "key": "a"
                }
            }
        """.toObj(Response::class.java)

        doReturn(response).`when`(requestDispatcherMock).dispatch(any(Request::class.java), any(JsonProvider::class.java), any(DispatchOptions::class.java))
        val finalResponse = batchEngine.execute(Request(), template)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(finalResponse))
        val context = JsonPath.using(configuration).parse(finalResponse.body)
        assertEquals(2, context.read("$.requests.length()", Int::class.java))
        assertEquals(3, context.read("$.requests[0].times.length()", Int::class.java))
    }

    @Test
    fun execute__withTransformer() {
        val template = """
            {
                "requests": [
                    {
                        "http_method": "GET",
                        "url": "https://test.com",
                        "body": {},
                        "transformers": [
                            {
                                "body": {
                                    "key": "int $.body.key_2"
                                }
                            }
                        ]
                    }
                ],
                "responses": null
            }
        """.toObj(BatchTemplate::class.java)
        val response = """
            {
                "status": 201,
                "headers": {},
                "body": {
                    "key_1": "a",
                    "key_2": "2"
                }
            }
        """.toObj(Response::class.java)

        doReturn(response).`when`(requestDispatcherMock).dispatch(any(Request::class.java), any(JsonProvider::class.java), any(DispatchOptions::class.java))
        val finalResponse = batchEngine.execute(Request(), template)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(finalResponse))
        val context = JsonPath.using(configuration).parse(finalResponse.body)
        assertEquals(201, context.read("$.responses[0].status", Int::class.java))
        assertEquals(2, context.read("$.responses[0].body.key", Int::class.java))
    }

    @Test
    fun execute__withVar() {
        val template = """
            {
                "requests": [
                    {
                        "http_method": "GET",
                        "url": "https://test.com",
                        "body": {},
                        "vars": [
                            {
                                "vars": {
                                    "var_1": "int $.responses[0].body.key_2"
                                }
                            },
                            {
                                "vars": {
                                    "var_@{$.vars.var_1}@": "$.responses[0].body.key_1"
                                }
                            }
                        ]
                    }
                ],
                "responses": [
                    {
                        "body": "$.vars"
                    }
                ]
            }
        """.toObj(BatchTemplate::class.java)
        val response = """
            {
                "status": 201,
                "headers": {},
                "body": {
                    "key_1": "a",
                    "key_2": "2"
                }
            }
        """.toObj(Response::class.java)

        doReturn(response).`when`(requestDispatcherMock).dispatch(any(Request::class.java), any(JsonProvider::class.java), any(DispatchOptions::class.java))
        val finalResponse = batchEngine.execute(Request(), template)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(finalResponse))
        val context = JsonPath.using(configuration).parse(finalResponse.body)
        assertEquals(2, context.read("$.var_1", Int::class.java))
        assertEquals("a", context.read("$.var_2", String::class.java))
    }

    @Test
    fun test() {
        val template = """
            {
                "requests": [
                    {
                        "http_method": "POST",
                        "url": "https://localhost.com",
                        "headers": {
                            "header": "str[] $.original.headers.header_2[*]"
                        },
                        "body": {
                            "key_1": "str $.original.body.key_1",
                            "key_2": "int $.original.body.key_2"
                        }
                    }
                ],
                "responses": null
            }
        """.trimIndent()
        val original_request = """
            {
                "headers": {
                    "header_1": ["abc"],
                    "header_2": ["qwe", "zxc"]
                },
                "body": {
                    "key_1": "abc",
                    "key_2": 2
                }
            }
        """.trimIndent()
        val response = """
            {
                "headers": {
                    "header_1": ["1"],
                    "header_2": ["2"]
                },
                "body": [
                    {   
                        "key": "a",
                        "value": 1
                    },
                    {   
                        "key": "b",
                        "value": 2
                    }
                ]
            }
        """.trimIndent()
        val batchTemplate = objectMapper.readValue(template, BatchTemplate::class.java)
        val originalRequest = objectMapper.readValue(original_request, Request::class.java)
        val firstResponse = objectMapper.readValue(response, Response::class.java)

        doReturn(firstResponse).`when`(requestDispatcherMock).dispatch(any(Request::class.java), any(JsonProvider::class.java), any(DispatchOptions::class.java));
        val finalResponse = batchEngine.execute(originalRequest, batchTemplate)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(finalResponse))
    }

    @Test
    fun test2() {
        val template = """
            {
                "requests": [
                    {
                        "http_method": "POST",
                        "url": "https://localhost.com",
                        "headers": {
                            "header": "str[] $.original.headers.header_2[*]"
                        },
                        "body": {
                            "key_1": "str $.original.body.key_1",
                            "key_2": "int $.original.body.key_2"
                        }
                    }
                ],
                "responses": [
                    {
                        "headers": {
                            "header": "str $.responses[0].headers.header_1"
                        },
                        "body": {
                            "keys": "str[] $.responses[0].body[*].key",
                            "sum_value": "int __sum(\"$.responses[0].body[*].value\")"
                        }
                    }
                ]
            }
        """.trimIndent()
        val original_request = """
            {
                "headers": {
                    "header_1": ["abc"],
                    "header_2": ["qwe", "zxc"]
                },
                "body": {
                    "key_1": "abc",
                    "key_2": 2
                }
            }
        """.trimIndent()
        val response = """
            {
                "headers": {
                    "header_1": [ "1" ],
                    "header_2": [ "2" ]
                },
                "body": [
                    {   
                        "key": "a",
                        "value": 1
                    },
                    {   
                        "key": "b",
                        "value": 2
                    }
                ]
            }
        """.trimIndent()
        val batchTemplate = objectMapper.readValue(template, BatchTemplate::class.java)
        val originalRequest = objectMapper.readValue(original_request, Request::class.java)
        val firstResponse = objectMapper.readValue(response, Response::class.java)

        doReturn(firstResponse).`when`(requestDispatcherMock).dispatch(any(Request::class.java), any(JsonProvider::class.java), any(DispatchOptions::class.java));
        val finalResponse = batchEngine.execute(originalRequest, batchTemplate)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(finalResponse))
    }

    fun <T> String.toObj(cl: Class<T>): T = objectMapper.readValue(this, cl)
}