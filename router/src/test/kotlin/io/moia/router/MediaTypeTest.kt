package io.moia.router

import com.google.common.net.MediaType
import isCompatibleWith
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test

class MediaTypeTest {

    @Test
    fun `should match`() {
        then(MediaType.parse("application/json").isCompatibleWith(MediaType.parse("application/json"))).isTrue()
    }

    @Test
    fun `should match subtype wildcard`() {
        then(MediaType.parse("application/json").isCompatibleWith(MediaType.parse("application/*"))).isTrue()
    }

    @Test
    fun `should not match subtype wildcard in different tpye`() {
        then(MediaType.parse("application/json").isCompatibleWith(MediaType.parse("image/*"))).isFalse()
    }

    @Test
    fun `should match wildcard`() {
        then(MediaType.parse("application/json").isCompatibleWith(MediaType.parse("*/*"))).isTrue()
    }

    @Test
    fun `should match wildcard structured syntax suffix`() {
        then(MediaType.parse("application/*+json").isCompatibleWith(MediaType.parse("application/vnd.moia+json"))).isTrue()
        then(MediaType.parse("application/*+json").isCompatibleWith(MediaType.parse("application/vnd.moia.v1+json"))).isTrue()
    }

    @Test
    fun `should not match wildcard structured syntax suffix on non suffix type`() {
        then(MediaType.parse("application/*+json").isCompatibleWith(MediaType.parse("application/json"))).isFalse()
    }

    @Test
    fun `should not match wildcard structured syntax suffix on differnt suffix`() {
        then(MediaType.parse("application/*+json").isCompatibleWith(MediaType.parse("application/*+x-protobuf"))).isFalse()
    }
}