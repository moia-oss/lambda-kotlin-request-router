package io.moia.router

import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class UriTemplateTest {

    @Test
    fun `should match without parameter`() {
        then(UriTemplate.from("/some").matches("/some")).isTrue()
    }

    @Test
    fun `should not match simple`() {
        then(UriTemplate.from("/some").matches("/some-other")).isFalse()
    }

    @Test
    fun `should match with parameter`() {
        then(UriTemplate.from("/some/{id}").matches("/some/${UUID.randomUUID()}")).isTrue()
        then(UriTemplate.from("/some/{id}/other").matches("/some/${UUID.randomUUID()}/other")).isTrue()
    }

    @Test
    fun `should not match with parameter`() {
        then(UriTemplate.from("/some/{id}").matches("/some-other/${UUID.randomUUID()}")).isFalse()
        then(UriTemplate.from("/some/{id}/other").matches("/some/${UUID.randomUUID()}/other-test")).isFalse()
    }

    @Test
    fun `should extract parameters`() {
        then(UriTemplate.from("/some/{first}/other/{second}").extract("/some/first-value/other/second-value"))
            .isEqualTo(mapOf("first" to "first-value", "second" to "second-value"))
        then(UriTemplate.from("/some").extract("/some")).isEmpty()
    }

    @Test
    fun `should match with query parameter`() {
        then(UriTemplate.from("/some?a=1").matches("/some")).isTrue()
        then(UriTemplate.from("/some?a=1&b=2").matches("/some")).isTrue()
    }

    @Test
    fun `should match with path parameter and query parameter`() {
        then(UriTemplate.from("/some/{id}?a=1").matches("/some/${UUID.randomUUID()}")).isTrue()
        then(UriTemplate.from("/some/{id}/other?a=1&b=2").matches("/some/${UUID.randomUUID()}/other")).isTrue()
    }

    @Test
    fun `should handle greedy path variables successfully`() {
        then(UriTemplate.from("/some/{proxy+}").matches("/some/sub/sub/sub/path")).isTrue()
    }

    @Test
    fun `should throw exception for greedy path variables at the wrong place`() {
        assertThrows<IllegalArgumentException>("Greedy path variables (e.g. '{proxy+}' are only allowed at the end of the template") {
            UriTemplate.from("/some/{proxy+}/and/{variable}/error")
        }
    }
}