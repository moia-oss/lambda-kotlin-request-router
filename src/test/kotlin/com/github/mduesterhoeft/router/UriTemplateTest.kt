package com.github.mduesterhoeft.router

import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
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
}