package io.moia.router

import assertk.assert
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
            assert(produces).isEqualTo(setOf("application/json"))
        }
    }

    @Test
    fun `should register get route with specific content types`() {
        val router = router {
            POST("/some") { r: Request<Unit> ->
                ResponseEntity.ok("""{"hello": "world", "request":"${r.body}"}""")
            }
                .producing("text/plain")
                .consuming("text/plain")
        }

        assert(router.routes).hasSize(1)
        with(router.routes.first().requestPredicate) {
            assert(method).isEqualTo("POST")
            assert(pathPattern).isEqualTo("/some")
            assert(consumes).isEqualTo(setOf("text/plain"))
            assert(produces).isEqualTo(setOf("text/plain"))
        }
    }

    @Test
    fun `should register get route with custom default content types`() {
        val router = router {

            defaultConsuming = setOf("text/plain")
            defaultProducing = setOf("text/plain")

            POST("/some") { r: Request<Unit> ->
                ResponseEntity.ok("""{"hello": "world", "request":"${r.body}"}""")
            }
        }

        assert(router.routes).hasSize(1)
        with(router.routes.first().requestPredicate) {
            assert(method).isEqualTo("POST")
            assert(pathPattern).isEqualTo("/some")
            assert(consumes).isEqualTo(setOf("text/plain"))
            assert(produces).isEqualTo(setOf("text/plain"))
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
            assertTrue(UriTemplate.from(pathPattern).matches("/some/sub/sub/sub/path"))
        }
    }
}