[![](https://jitpack.io/v/moia-dev/lambda-kotlin-request-router.svg)](https://jitpack.io/#moia-dev/lambda-kotlin-request-router)
[![CircleCI](https://circleci.com/gh/moia-dev/lambda-kotlin-request-router.svg?style=svg&circle-token=c7f438800605a6528b630d44dfa8a1da3ec3c06a)](https://circleci.com/gh/moia-dev/lambda-kotlin-request-router)
[![codecov](https://codecov.io/gh/moia-dev/lambda-kotlin-request-router/branch/master/graph/badge.svg)](https://codecov.io/gh/moia-dev/lambda-kotlin-request-router)

# lambda-kotlin-request-router

A REST request routing layer for AWS lambda handlers written in Kotlin.

## Goal

We came up `lambda-kotlin-request-router` to reduce boilerplate code when implementing a REST API handlers on AWS Lambda.

The library addresses the following aspects:

- serialization and deserialization
- provide useful extensions and abstractions for API Gateway request and response types
- writing REST handlers as functions
- ease implementation of cross cutting concerns for handlers
- ease (local) testing of REST handlers

## Reference

### Getting Started

To use the core module we need the following:

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.moia-dev.lambda-kotlin-request-router:router:0.3.1' 
}

```

Having this we can now go ahead and implement our first handler.
We can implement a request handler as a simple function.
Request and response body are deserialized and serialized for you.

```kotlin
import io.moia.router.Request
import io.moia.router.RequestHandler
import io.moia.router.ResponseEntity
import io.moia.router.Router.Companion.router

class MyRequestHandler : RequestHandler() {

    override val router = router {
        GET("/some") { r: Request<String> -> ResponseEntity.ok(MyResponse(r.body)) }
    }
}
```

### Content Negotiation

The router DSL allows for configuration of the content types a handler
- produces (according to the request's `Accept` header)
- consumes (according to the request's `Content-Type` header)

The router itself carries a default for both values.

```kotlin
var defaultConsuming = setOf("application/json")
var defaultProducing = setOf("application/json")
```

These defaults can be overridden on the router level or on the handler level to specify the content types most of your handlers consume and produce.

```kotlin
router {
    defaultConsuming = setOf("application/json")
    defaultProducing = setOf("application/json")
}
```

Exceptions from this default can be configured on a handler level.

```kotlin
router {
    POST("/some") { r: Request<String> -> ResponseEntity.ok(MyResponse(r.body)) }
        .producing("application/json")
        .consuming("application/json")
}
```

### Filters

Filters are a means to add cross-cutting concerns to your request handling logic outside a handler function.
Multiple filters can be used by composing them.

```kotlin
override val router = router {
        filter = loggingFilter().then(mdcFilter())

        GET("/some", controller::get)
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
}
```




