package io.moia.router

import com.google.common.net.MediaType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlainTextSerializationHandlerTest {

    @Test
    fun `should support text`() {
        assertTrue(PlainTextSerializationHandler().supports(MediaType.parse("text/plain"), "some"))
        assertTrue(PlainTextSerializationHandler(listOf(MediaType.parse("text/csv"))).supports(MediaType.parse("text/csv"), "some"))
    }

    @Test
    fun `should not support anything else than text`() {
        assertFalse(PlainTextSerializationHandler().supports(MediaType.parse("application/json"), "some"))
        assertFalse(PlainTextSerializationHandler().supports(MediaType.parse("image/jpeg"), "some"))
    }

    @Test
    fun `should serialize string`() {
        assertEquals("some", PlainTextSerializationHandler().serialize(MediaType.parse("text/plain"), "some"))
    }
}