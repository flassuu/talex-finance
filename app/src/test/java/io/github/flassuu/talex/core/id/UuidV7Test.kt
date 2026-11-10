package io.github.flassuu.talex.core.id

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class UuidV7Test {

    @Test
    fun generatesVersion7WithIetfVariant() {
        repeat(100) {
            val uuid = UUID.fromString(UuidV7.generate())
            assertEquals(7, uuid.version())
            assertEquals(2, uuid.variant())
        }
    }

    @Test
    fun idsAreUnique() {
        val ids = (1..10_000).map { UuidV7.generate() }.toSet()
        assertEquals(10_000, ids.size)
    }

    @Test
    fun sortsByEmbeddedTimestamp() {
        assertTrue(UuidV7.generate(1_000_000L) < UuidV7.generate(2_000_000L))
    }
}
