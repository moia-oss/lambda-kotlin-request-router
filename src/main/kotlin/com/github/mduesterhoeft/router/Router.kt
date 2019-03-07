package com.github.mduesterhoeft.router

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent

class Router {

    val routes = mutableListOf<RouterFunction<*, *>>()

    var defaultConsuming = setOf("application/json", "application/x-protobuf")
    var defaultProducing = setOf("application/json", "application/x-protobuf")

    fun <I, T> GET(pattern: String, handlerFunction: HandlerFunction<I, T>) =
        RequestPredicate(
            method = "GET",
            pathPattern = pattern,
            consumes = emptySet(),
            produces = defaultProducing
        ).also { routes += RouterFunction(it, handlerFunction) }

    fun <I, T> POST(pattern: String, handlerFunction: HandlerFunction<I, T>) =
        RequestPredicate(
            method = "POST",
            pathPattern = pattern,
            consumes = defaultConsuming,
            produces = defaultProducing
        ).also {
            routes += RouterFunction(it, handlerFunction)
        }

    fun <I, T> PUT(pattern: String, handlerFunction: HandlerFunction<I, T>) =
        RequestPredicate(
            method = "PUT",
            pathPattern = pattern,
            consumes = defaultConsuming,
            produces = defaultProducing
        ).also {
            routes += RouterFunction(it, handlerFunction)
        }

    fun <I, T> DELETE(pattern: String, handlerFunction: HandlerFunction<I, T>) =
        RequestPredicate(
            method = "DELETE",
            pathPattern = pattern,
            consumes = defaultConsuming,
            produces = defaultProducing
        ).also {
            routes += RouterFunction(it, handlerFunction)
        }

    fun <I, T> PATCH(pattern: String, handlerFunction: HandlerFunction<I, T>) =
        RequestPredicate(
            method = "PATCH",
            pathPattern = pattern,
            consumes = defaultConsuming,
            produces = defaultProducing
        ).also {
            routes += RouterFunction(it, handlerFunction)
        }

    // the default content types the HandlerFunctions of this router can produce
    fun defaultProducing(contentTypes: Set<String>): Router = this.also { defaultProducing = contentTypes }

    // the default content types the HandlerFunctions of this router can handle
    fun defaultConsuming(contentTypes: Set<String>): Router = this.also { defaultConsuming = contentTypes }

    companion object {
        fun router(routes: Router.() -> Unit) = Router().apply(routes)
    }
}

typealias HandlerFunction<I, T> = (request: Request<I>) -> ResponseEntity<T>

data class RouterFunction<I, T>(
    val requestPredicate: RequestPredicate,
    val handler: HandlerFunction<I, T>
)

data class Request<I>(val apiRequest: APIGatewayProxyRequestEvent, val body: I)
