package br.com.guiabolso.events.model

import br.com.guiabolso.events.EventBuilderForTest
import br.com.guiabolso.events.json.MapperHolder
import br.com.guiabolso.events.model.EventErrorType.*
import br.com.guiabolso.events.model.EventErrorType.Companion.getErrorType
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EventErrorTypeTest {

    @Test
    fun testErrorMapping() {
        assertEquals(Generic, getErrorType("error"))
        assertEquals("error", getErrorType("error").typeName)

        assertEquals(NotFound, getErrorType("notFound"))
        assertEquals("notFound", getErrorType("notFound").typeName)

        assertEquals(Unauthorized, getErrorType("unauthorized"))
        assertEquals("unauthorized", getErrorType("unauthorized").typeName)

        assertEquals(Forbidden, getErrorType("forbidden"))
        assertEquals("forbidden", getErrorType("forbidden").typeName)

        assertEquals(Unknown("somethingElse"), getErrorType("somethingElse"))
        assertEquals("somethingElse", getErrorType("somethingElse").typeName)
    }

    @Test
    fun testJsonNullUserIdEvent() {
        val identity = MapperHolder.mapper.fromJson("""
            {
                "userId": null
            }
        """.trimIndent(), JsonObject::class.java)

        val event = EventBuilderForTest.buildRequestEvent().copy(identity = identity)

        assertNull(event.userId)
    }

    @Test
    fun testJsonNullOriginEvent() {
        val metadata = MapperHolder.mapper.fromJson("""
            {
                "origin": null
            }
        """.trimIndent(), JsonObject::class.java)

        val event = EventBuilderForTest.buildRequestEvent().copy(metadata = metadata)

        assertNull(event.origin)
    }

    @Test
    fun testNotNullUserIdEvent() {
        val identity = MapperHolder.mapper.fromJson("""
            {
                "userId": 123987
            }
        """.trimIndent(), JsonObject::class.java)

        val event = EventBuilderForTest.buildRequestEvent().copy(identity = identity)

        assertEquals(123987L, event.userId)
    }

    @Test
    fun testNotNullOriginEvent() {
        val metadata = MapperHolder.mapper.fromJson("""
            {
                "origin": "east"
            }
        """.trimIndent(), JsonObject::class.java)

        val event = EventBuilderForTest.buildRequestEvent().copy(metadata = metadata)

        assertEquals("east", event.origin)
    }
}