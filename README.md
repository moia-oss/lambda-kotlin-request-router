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

## Getting Started

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



