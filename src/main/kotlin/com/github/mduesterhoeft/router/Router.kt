package com.github.mduesterhoeft.router

class Router {

    val routes = mutableListOf<RouterFunction<*>>()

    fun <T> GET(pattern: String, handlerFunction: HandlerFunction<T>) =
        RequestPredicate(
            method = "GET",
            pathPattern = pattern,
            consumes = emptySet()
        ).also { routes += RouterFunction(it, handlerFunction) }

    fun <T> POST(pattern: String, handlerFunction: HandlerFunction<T>) =
        RequestPredicate("POST", pattern).also {
            routes += RouterFunction(it, handlerFunction)
    }

    companion object {
        fun router(routes: Router.() -> Unit) = Router().apply(routes)
    }
}

typealias HandlerFunction<T> = (request: ApiRequest) -> ResponseEntity<T>

data class RouterFunction<T>(
    val requestPredicate: RequestPredicate,
    val handler: HandlerFunction<T>
)
