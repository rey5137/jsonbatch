package com.rey.jsonbatch

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import com.jayway.jsonpath.spi.json.JsonProvider
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import com.rey.jsonbatch.function.AverageFunction
import com.rey.jsonbatch.function.MaxFunction
import com.rey.jsonbatch.function.MinFunction
import com.rey.jsonbatch.function.SumFunction
import com.rey.jsonbatch.model.BatchTemplate
import com.rey.jsonbatch.model.DispatchOptions
import com.rey.jsonbatch.model.Request
import com.rey.jsonbatch.model.Response
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock

class BatchEngineTest {

    private lateinit var batchEngine: BatchEngine

    private lateinit var requestDispatcherMock: RequestDispatcher

    private lateinit var objectMapper: ObjectMapper;

    @Before
    fun setUp() {
        objectMapper = ObjectMapper()
        objectMapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
        val conf = Configuration.builder()
                .jsonProvider(JacksonJsonProvider(objectMapper))
                .mappingProvider(JacksonMappingProvider(objectMapper))
                .build()
        val jsonBuilder = JsonBuilder(SumFunction.instance(),
                AverageFunction.instance(),
                MinFunction.instance(),
                MaxFunction.instance())
        requestDispatcherMock = mock(RequestDispatcher::class.java)
        batchEngine = BatchEngine(conf, jsonBuilder, requestDispatcherMock)
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
    
}