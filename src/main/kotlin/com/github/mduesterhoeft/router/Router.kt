package com.github.mduesterhoeft.router

class Router {

    val routes = mutableListOf<RouterFunction<*, *>>()

    fun <I, T> GET(pattern: String, handlerFunction: HandlerFunction<I, T>) =
        RequestPredicate(
            method = "GET",
            pathPattern = pattern,
            consumes = emptySet()
        ).also { routes += RouterFunction(it, handlerFunction) }

    fun <I, T> POST(pattern: String, handlerFunction: HandlerFunction<I, T>) =
        RequestPredicate("POST", pattern).also {
            routes += RouterFunction(it, handlerFunction)
        }

    fun <I, T> PUT(pattern: String, handlerFunction: HandlerFunction<I, T>) =
        RequestPredicate("PUT", pattern).also {
            routes += RouterFunction(it, handlerFunction)
        }

    fun <I, T> DELETE(pattern: String, handlerFunction: HandlerFunction<I, T>) =
        RequestPredicate("DELETE", pattern).also {
            routes += RouterFunction(it, handlerFunction)
        }

    companion object {
        fun router(routes: Router.() -> Unit) = Router().apply(routes)
    }
}

typealias HandlerFunction<I, T> = (request: Request<I>) -> ResponseEntity<T>

data class RouterFunction<I, T>(
    val requestPredicate: RequestPredicate,
    val handler: HandlerFunction<I, T>
)

data class Request<I>(val apiRequest: ApiRequest, val body: I)
