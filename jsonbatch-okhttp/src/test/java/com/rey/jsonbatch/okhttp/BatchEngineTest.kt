package com.rey.jsonbatch.okhttp

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import com.rey.jsonbatch.BatchEngine
import com.rey.jsonbatch.JsonBuilder
import com.rey.jsonbatch.function.*
import com.rey.jsonbatch.model.BatchTemplate
import com.rey.jsonbatch.model.Request
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit



class BatchEngineTest {

    private lateinit var batchEngine: BatchEngine

    private lateinit var objectMapper: ObjectMapper

    @Before
    fun setUp() {
        val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        root.level = Level.DEBUG

        objectMapper = ObjectMapper()
        objectMapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
        val conf = Configuration.builder()
                .jsonProvider(JacksonJsonProvider(objectMapper))
                .mappingProvider(JacksonMappingProvider(objectMapper))
                .build()
        val jsonBuilder = JsonBuilder(*Functions.basic())
        val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()
        batchEngine = BatchEngine(conf, jsonBuilder, OkHttpRequestDispatcher(client))
    }
    
    @Test
    fun test() {
        val template = """
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
        """.trimIndent()
        val original_request = """
            {
                "headers": null,
                "body": null
            }
        """.trimIndent()
        val batchTemplate = objectMapper.readValue(template, BatchTemplate::class.java)
        val originalRequest = objectMapper.readValue(original_request, Request::class.java)

        val finalResponse = batchEngine.execute(originalRequest, batchTemplate)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(finalResponse))
    }

    @Test
    fun test2() {
        val template = """
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
                                        "http_method": "GET",
                                        "url": "https://google.com",
                                        "headers": null,
                                        "body": null,
                                        "responses": [
                                            {
                                                "predicate": "__cmp(\"@{$.responses[2].status}@ != 201\")",
                                                "status": "$.responses[2].status",
                                                "headers": null,
                                                "body": {
                                                    "first_post": "obj $.responses[1].body",
                                                    "new_post": "Error",
                                                    "response": "$.responses[2].body"
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
        """.trimIndent()
        val original_request = """
            {
                "headers": null,
                "body": null
            }
        """.trimIndent()
        val batchTemplate = objectMapper.readValue(template, BatchTemplate::class.java)
        val originalRequest = objectMapper.readValue(original_request, Request::class.java)

        val finalResponse = batchEngine.execute(originalRequest, batchTemplate)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(finalResponse))
    }
    
}