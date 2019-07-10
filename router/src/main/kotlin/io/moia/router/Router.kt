package io.moia.router

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent.ProxyRequestContext
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent

class Router {

    val routes = mutableListOf<RouterFunction<*, *>>()

    var defaultConsuming = setOf("application/json")
    var defaultProducing = setOf("application/json")

    var defaultContentType = "application/json"

    var filter: Filter = Filter.NoOp

    fun <I, T> GET(pattern: String, handlerFunction: HandlerFunction<I, T>) =
        defaultRequestPredicate(pattern, "GET", handlerFunction, emptySet())

    fun <I, T> POST(pattern: String, handlerFunction: HandlerFunction<I, T>) =
        defaultRequestPredicate(pattern, "POST", handlerFunction)

    fun <I, T> PUT(pattern: String, handlerFunction: HandlerFunction<I, T>) =
        defaultRequestPredicate(pattern, "PUT", handlerFunction)

    fun <I, T> DELETE(pattern: String, handlerFunction: HandlerFunction<I, T>) =
        defaultRequestPredicate(pattern, "DELETE", handlerFunction, emptySet())

    fun <I, T> PATCH(pattern: String, handlerFunction: HandlerFunction<I, T>) =
        defaultRequestPredicate(pattern, "PATCH", handlerFunction)

    private fun <I, T> defaultRequestPredicate(
        pattern: String,
        method: String,
        handlerFunction: HandlerFunction<I, T>,
        consuming: Set<String> = defaultConsuming
    ) =
        RequestPredicate(
            method = method,
            pathPattern = pattern,
            consumes = consuming,
            produces = defaultProducing
        ).also { routes += RouterFunction(it, handlerFunction) }

    companion object {
        fun router(routes: Router.() -> Unit) = Router().apply(routes)
    }
}

interface Filter : (APIGatewayRequestHandlerFunction) -> APIGatewayRequestHandlerFunction {
    companion object {
        operator fun invoke(fn: (APIGatewayRequestHandlerFunction) -> APIGatewayRequestHandlerFunction): Filter = object :
            Filter {
            override operator fun invoke(next: APIGatewayRequestHandlerFunction): APIGatewayRequestHandlerFunction = fn(next)
        }
    }
}

val Filter.Companion.NoOp: Filter get() = Filter { next -> { next(it) } }

fun Filter.then(next: Filter): Filter = Filter { this(next(it)) }
fun Filter.then(next: APIGatewayRequestHandlerFunction): APIGatewayRequestHandlerFunction = { this(next)(it) }

typealias APIGatewayRequestHandlerFunction = (APIGatewayProxyRequestEvent) -> APIGatewayProxyResponseEvent
typealias HandlerFunction<I, T> = (request: Request<I>) -> ResponseEntity<T>

class RouterFunction<I, T>(
    val requestPredicate: RequestPredicate,
    val handler: HandlerFunction<I, T>
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
}
