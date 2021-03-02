[![](https://jitpack.io/v/moia-oss/lambda-kotlin-request-router.svg)](https://jitpack.io/#moia-oss/lambda-kotlin-request-router)
[![Build Status](https://travis-ci.org/moia-oss/lambda-kotlin-request-router.svg?branch=master)](https://travis-ci.org/moia-oss/lambda-kotlin-request-router)
[![Coverage Status](https://coveralls.io/repos/github/moia-dev/lambda-kotlin-request-router/badge.svg?branch=master)](https://coveralls.io/github/moia-dev/lambda-kotlin-request-router?branch=master)
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
    implementation 'com.github.moia-oss.lambda-kotlin-request-router:router:0.9.7' 
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
            log.info("Handling request ${request.httpMethod} ${request.path}")
            next(request) }
    }

    private fun mdcFilter() = Filter { next -> {
        request ->
            MDC.put("requestId", request.requestContext?.requestId)
            next(request) }
    }
}
```

### Permissions

Permission handling is a cross-cutting concern that can be handled outside the regular handler function.
The routing DSL also supports expressing required permissions:

```kotlin
override val router = router {
    GET("/some", controller::get).requiringPermissions("A_PERMISSION", "A_SECOND_PERMISSION")
}
```

For the route above the `RequestHandler` checks if *any* of the listed permissions are found on a request.

Additionally we need to configure a strategy to extract permissions from a request on the `RequestHandler`.
By default a `RequestHandler` is using the `NoOpPermissionHandler` which always decides that any required permissions are found.
The `JwtPermissionHandler` can be used to extract permissions from a JWT token found in a header.

```kotlin
class TestRequestHandlerAuthorization : RequestHandler() {
    override val router = router {
       GET("/some", controller::get).requiringPermissions("A_PERMISSION")
    }

    override fun permissionHandlerSupplier(): (r: APIGatewayProxyRequestEvent) -> PermissionHandler =
        { JwtPermissionHandler(
            request = it,
            //the claim to use to extract the permissions - defaults to `scope`
            permissionsClaim = "permissions",
            //separator used to separate permissions in the claim - defaults to ` `
            permissionSeparator = ","
        ) }
}
```

Given the code above the token is extracted from the `Authorization` header.
We can also choose to extract the token from a different header:

```kotlin
JwtPermissionHandler(
    accessor = JwtAccessor(
        request = it,
        authorizationHeaderName = "custom-auth")
)
```

:warning: The implementation here assumes that JWT tokens are validated on the API Gateway. 
So we do no validation of the JWT token.

### Protobuf support

The module `router-protobuf` helps to ease implementation of handlers that receive and return protobuf messages.

```
implementation 'com.github.moia-dev.lambda-kotlin-request-router:router-protobuf:0.8.8'
```

A handler implementation that wants to take advantage of the protobuf support should inherit from `ProtoEnabledRequestHandler`.

```kotlin
class TestRequestHandler : ProtoEnabledRequestHandler() {

        override val router = router {
            defaultProducing = setOf("application/x-protobuf")
            defaultConsuming = setOf("application/x-protobuf")

            defaultContentType = "application/x-protobuf"

            GET("/some-proto") { _: Request<Unit> -> ResponseEntity.ok(Sample.newBuilder().setHello("Hello").build()) }
                .producing("application/x-protobuf", "application/json")
            POST("/some-proto") { r: Request<Sample> -> ResponseEntity.ok(r.body) }
            GET<Unit, Unit>("/some-error") { _: Request<Unit> -> throw ApiException("boom", "BOOM", 400) }
        }

        override fun createErrorBody(error: ApiError): Any =
            io.moia.router.proto.sample.SampleOuterClass.ApiError.newBuilder()
                .setMessage(error.message)
                .setCode(error.code)
                .build()

        override fun createUnprocessableEntityErrorBody(errors: List<UnprocessableEntityError>): Any =
            errors.map { error ->
                io.moia.router.proto.sample.SampleOuterClass.UnprocessableEntityError.newBuilder()
                    .setMessage(error.message)
                    .setCode(error.code)
                    .setPath(error.path)
                    .build()
            }
    }
```

Make sure you override `createErrorBody` and `createUnprocessableEntityErrorBody` to map error type to your proto error messages.


### Open API validation support

The module `router-openapi-request-validator` can be used to validate an interaction against an [OpenAPI](https://www.openapis.org/) specification.
Internally we use the [swagger-request-validator](https://bitbucket.org/atlassian/swagger-request-validator) to achieve this.

This library validates:
- if the resource used is documented in the OpenApi specification
- if request and response can be successfully validated against the request and response schema
- ...

```
testImplementation 'com.github.moia-oss.lambda-kotlin-request-router:router-openapi-request-validator:0.9.7'
```

```kotlin
    val validator = OpenApiValidator("openapi.yml")

    @Test
    fun `should handle and validate request`() {
        val request = GET("/tests")
            .withHeaders(mapOf("Accept" to "application/json"))

        val response = testHandler.handleRequest(request, mockk())

        validator.assertValidRequest(request)
        validator.assertValidResponse(request, response)
        validator.assertValid(request, response)
    }
```

If you want to validate all the API interactions in your handler tests against the API specification you can use `io.moia.router.openapi.ValidatingRequestRouterWrapper`.
This a wrapper around your `RequestHandler` which transparently validates request and response.

```kotlin
    private val validatingRequestRouter = ValidatingRequestRouterWrapper(TestRequestHandler(), "openapi.yml")
    
    @Test
    fun `should return response on successful validation`() {
        val response = validatingRequestRouter
            .handleRequest(GET("/tests").withAcceptHeader("application/json"), mockk())

        then(response.statusCode).isEqualTo(200)
    }
```

