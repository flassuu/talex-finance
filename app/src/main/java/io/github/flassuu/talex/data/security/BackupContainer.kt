package io.github.flassuu.talex.data.security

import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * The single-file backup container from ADR-0028: a ZIP holding the encrypted
 * database, its KDF sidecar and a tiny manifest with the container format version.
 */
object BackupContainer {

    const val FORMAT_VERSION = 1
    const val DATABASE_ENTRY = "talex.db"
    const val METADATA_ENTRY = "talex.db.meta"

    private const val MANIFEST_ENTRY = "manifest"
    private const val MANIFEST_PREFIX = "format="

    fun write(output: OutputStream, database: InputStream, metadata: InputStream) {
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write("$MANIFEST_PREFIX$FORMAT_VERSION\n".toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry(DATABASE_ENTRY))
            database.copyTo(zip)
            zip.closeEntry()
            zip.putNextEntry(ZipEntry(METADATA_ENTRY))
            metadata.copyTo(zip)
            zip.closeEntry()
        }
    }

    /**
     * Streams the two entries into the given sinks and returns the manifest version.
     * Throws [BackupFormatException] when the archive is not a Talex container.
     */
    fun read(input: InputStream, database: OutputStream, metadata: OutputStream): Int {
        var version = -1
        var sawDatabase = false
        var sawMetadata = false
        try {
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when (entry.name) {
                        MANIFEST_ENTRY -> {
                            val text = zip.readBytes().toString(Charsets.UTF_8).trim()
                            version = text.removePrefix(MANIFEST_PREFIX).trim().toIntOrNull() ?: -1
                        }
                        DATABASE_ENTRY -> {
                            zip.copyTo(database)
                            sawDatabase = true
                        }
                        METADATA_ENTRY -> {
                            zip.copyTo(metadata)
                            sawMetadata = true
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (e: BackupFormatException) {
            throw e
        } catch (e: Exception) {
            throw BackupFormatException(e.message ?: "Invalid backup archive")
        }
        if (!sawDatabase || !sawMetadata) throw BackupFormatException("Missing backup entries")
        // An older container without a manifest is treated as version 1.
        return if (version < 0) 1 else version
    }
}

class BackupFormatException(message: String) : Exception(message)
