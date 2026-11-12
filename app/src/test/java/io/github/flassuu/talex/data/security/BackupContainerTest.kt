package io.github.flassuu.talex.data.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class BackupContainerTest {

    private val database = ByteArray(512) { (it % 251).toByte() }
    private val metadata = """{"version":1,"algorithm":"argon2id"}""".toByteArray()

    @Test
    fun roundTripsBothEntries() {
        val container = ByteArrayOutputStream()
        BackupContainer.write(container, ByteArrayInputStream(database), ByteArrayInputStream(metadata))

        val dbOut = ByteArrayOutputStream()
        val metaOut = ByteArrayOutputStream()
        val version = BackupContainer.read(ByteArrayInputStream(container.toByteArray()), dbOut, metaOut)

        assertEquals(BackupContainer.FORMAT_VERSION, version)
        assertEquals(database.toList(), dbOut.toByteArray().toList())
        assertEquals(metadata.toList(), metaOut.toByteArray().toList())
    }

    @Test
    fun rejectsANonContainer() {
        assertThrows(BackupFormatException::class.java) {
            BackupContainer.read(
                ByteArrayInputStream("not a zip".toByteArray()),
                ByteArrayOutputStream(),
                ByteArrayOutputStream(),
            )
        }
    }
}
