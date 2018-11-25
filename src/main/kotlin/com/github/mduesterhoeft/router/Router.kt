package com.github.mduesterhoeft.router

class Router {

    val routes = mutableListOf<RouterFunction>()

    fun GET(pattern: String, handlerFunction: HandlerFunction) =
        RequestPredicate(
            method = "GET",
            pathPattern = pattern,
            consumes = emptySet()
        ).also { routes += RouterFunction(it, handlerFunction) }

    fun POST(pattern: String, handlerFunction: HandlerFunction) =
        RequestPredicate("POST", pattern).also {
            routes += RouterFunction(it, handlerFunction)
    }

    companion object {
        fun router(routes: Router.() -> Unit) = Router().apply(routes)
    }
}

typealias HandlerFunction = (request: ApiRequest) -> ApiResponse

data class RouterFunction(
    val requestPredicate: RequestPredicate,
    val handler: HandlerFunction
)
