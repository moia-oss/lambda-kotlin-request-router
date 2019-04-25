package io.moia.router

import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test

class NoOpPermissionHandlerTest {

    @Test
    fun `should always return true`() {
        val handler = NoOpPermissionHandler()

        then(handler.hasAnyRequiredPermission(setOf("any"))).isTrue()
    }
}