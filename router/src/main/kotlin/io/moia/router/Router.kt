/*
 * Copyright 2019 MOIA GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

package io.moia.router

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent.ProxyRequestContext
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import kotlin.reflect.KType
import kotlin.reflect.typeOf

typealias PredicateFactory = (String, String, Set<String>, Set<String>) -> RequestPredicate

@Suppress("FunctionName")
class Router(private val predicateFactory: PredicateFactory) {
    val routes = mutableListOf<RouterFunction<*, *>>()

    var defaultConsuming = setOf("application/json")
    var defaultProducing = setOf("application/json")

    var defaultContentType = "application/json"

    var filter: Filter = Filter.NoOp

    fun <I, T> GET(
        pattern: String,
        handlerFunction: HandlerFunction<I, T>,
    ) = defaultRequestPredicate(pattern, "GET", handlerFunction, emptySet())

    fun <I, T> POST(
        pattern: String,
        handlerFunction: HandlerFunction<I, T>,
    ) = defaultRequestPredicate(pattern, "POST", handlerFunction)

    fun <I, T> PUT(
        pattern: String,
        handlerFunction: HandlerFunction<I, T>,
    ) = defaultRequestPredicate(pattern, "PUT", handlerFunction)

    fun <I, T> DELETE(
        pattern: String,
        handlerFunction: HandlerFunction<I, T>,
    ) = defaultRequestPredicate(pattern, "DELETE", handlerFunction, emptySet())

    fun <I, T> PATCH(
        pattern: String,
        handlerFunction: HandlerFunction<I, T>,
    ) = defaultRequestPredicate(pattern, "PATCH", handlerFunction)

    private fun <I, T> defaultRequestPredicate(
        pattern: String,
        method: String,
        handlerFunction: HandlerFunction<I, T>,
        consuming: Set<String> = defaultConsuming,
    ) = predicateFactory(method, pattern, consuming, defaultProducing)
        .also { routes += RouterFunction(it, handlerFunction) }

    companion object {
        fun defaultPredicateFactory(
            method: String,
            pattern: String,
            consuming: Set<String>,
            producing: Set<String>,
        ): RequestPredicate =
            RequestPredicateImpl(
                method = method,
                pathPattern = pattern,
                consumes = consuming,
                produces = producing,
            )

        fun router(routes: Router.() -> Unit) = Router(Router::defaultPredicateFactory).apply(routes)

        fun router(
            factory: PredicateFactory,
            routes: Router.() -> Unit,
        ) = Router(factory).apply(routes)
    }
}

interface Filter : (APIGatewayRequestHandlerFunction) -> APIGatewayRequestHandlerFunction {
    companion object {
        operator fun invoke(fn: (APIGatewayRequestHandlerFunction) -> APIGatewayRequestHandlerFunction): Filter =
            object :
                Filter {
                override operator fun invoke(next: APIGatewayRequestHandlerFunction): APIGatewayRequestHandlerFunction = fn(next)
            }
    }
}

val Filter.Companion.NoOp: Filter get() = Filter { next -> { next(it) } }

fun Filter.then(next: Filter): Filter = Filter { this(next(it)) }

fun Filter.then(next: APIGatewayRequestHandlerFunction): APIGatewayRequestHandlerFunction = { this(next)(it) }

typealias APIGatewayRequestHandlerFunction = (APIGatewayProxyRequestEvent) -> APIGatewayProxyResponseEvent
typealias ConcreteHandlerFunction<I, T> = (request: Request<I>) -> ResponseEntity<T>

abstract class HandlerFunction<I, T> {
    abstract val requestType: KType
    abstract val responseType: KType

    abstract val handlerFunction: ConcreteHandlerFunction<I, T>

    companion object {
        inline operator fun <reified I, reified T> invoke(crossinline handler: ConcreteHandlerFunction<I, T>): HandlerFunction<I, T> {
            val requestType = typeOf<I>()
            val responseType = typeOf<T>()
            return object : HandlerFunction<I, T>() {
                override val requestType: KType = requestType
                override val responseType: KType = responseType
                override val handlerFunction: ConcreteHandlerFunction<I, T> = { request -> handler.invoke(request) }
            }
        }
    }
}

class RouterFunction<I, T>(
    val requestPredicate: RequestPredicate,
    val handler: HandlerFunction<I, T>,
) {
    override fun toString(): String {
        return "RouterFunction(requestPredicate=$requestPredicate)"
    }
}

data class Request<I>(val apiRequest: APIGatewayProxyRequestEvent, val body: I, val pathPattern: String = apiRequest.path) {
    val pathParameters by lazy { UriTemplate.from(pathPattern).extract(apiRequest.path) }
    val queryParameters: Map<String, String>? by lazy { apiRequest.queryStringParameters }
    val multiValueQueryStringParameters: Map<String, List<String>>? by lazy { apiRequest.multiValueQueryStringParameters }
    val requestContext: ProxyRequestContext by lazy { apiRequest.requestContext }

    fun getPathParameter(name: String): String = pathParameters[name] ?: error("Could not find path parameter '$name")

    fun getQueryParameter(name: String): String? = queryParameters?.get(name)

    fun getMultiValueQueryStringParameter(name: String): List<String>? = multiValueQueryStringParameters?.get(name)

    fun getJwtCognitoUsername(): String? = (JwtAccessor(this.apiRequest).extractJwtClaims()?.get("cognito:username") as? String)
}
