package io.moia.router

import assertk.assert
import assertk.assertions.containsAll
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import io.moia.router.Router.Companion.router
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RouterTest {

    @Test
    fun `should register get route with default accept header`() {
        val router = router {
            GET("/some") { r: Request<Unit> ->
                ResponseEntity.ok("""{"hello": "world", "request":"${r.body}"}""")
            }
        }

        assert(router.routes).hasSize(1)
        with(router.routes.first().requestPredicate) {
            assert(method).isEqualTo("GET")
            assert(pathPattern).isEqualTo("/some")
            assert(consumes).isEmpty()
            assert(produces).containsAll("application/json", "application/x-protobuf")
        }
    }

    @Test
    fun `should handle greedy path variables successfully`() {
        val router = router {
            POST("/some/{proxy+}") { r: Request<Unit> ->
                ResponseEntity.ok("""{"hello": "world", "request":"${r.body}"}""")
            }
        }
        assert(router.routes).hasSize(1)
        with(router.routes.first().requestPredicate) {
            assert(method).isEqualTo("POST")
            assertTrue(UriTemplate.from(pathPattern).matches("/some/sub/sub/sub/path"))
            assert(produces).containsAll("application/json", "application/x-protobuf")
        }
    }
}