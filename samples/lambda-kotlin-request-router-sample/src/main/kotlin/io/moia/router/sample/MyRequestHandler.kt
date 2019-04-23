package io.moia.router.sample

import io.moia.router.Filter
import io.moia.router.Request
import io.moia.router.RequestHandler
import io.moia.router.ResponseEntity
import io.moia.router.Router.Companion.router
import io.moia.router.getHeaderCaseInsensitive
import io.moia.router.then
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import kotlin.concurrent.timer
import kotlin.system.measureTimeMillis

class MyRequestHandler : RequestHandler() {
    private val controller = SomeController()

    override val router = router {
        //use filters to add cross-cutting concerns to each request
        filter = loggingFilter().then(mdcFilter())

        // functions can be externalized...
        GET("/some", controller::get)

        // simple handlers can also be declared inline
        POST("/some") { r: Request<Sample> -> ResponseEntity.ok(r.body) }
    }

    private fun loggingFilter() = Filter { next -> {
        request ->
            log.info("Handling request ${request.apiRequest.httpMethod} ${request.apiRequest.path}")
            next(request) }
    }

    private fun mdcFilter() = Filter { next -> {
        request ->
            MDC.put("requestId", request.apiRequest.requestContext?.requestId)
            next(request) }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(MyRequestHandler::class.java)
    }
}