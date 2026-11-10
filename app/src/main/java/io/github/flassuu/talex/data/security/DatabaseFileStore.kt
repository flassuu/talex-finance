package io.github.flassuu.talex.data.security

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the encrypted database file and its sidecar KDF metadata inside internal
 * storage. Both are excluded from system backup (D-036, D-037).
 */
@Singleton
class DatabaseFileStore @Inject constructor(
    @ApplicationContext context: Context,
) {

    val databaseFile: File = context.getDatabasePath(DATABASE_NAME)
    val metadataFile: File = File(databaseFile.parentFile, "$DATABASE_NAME.meta")

    fun hasMetadata(): Boolean = metadataFile.exists()

    fun hasDatabase(): Boolean = databaseFile.exists()

    fun readMetadata(): KdfMetadata? = runCatching {
        metadataFile.takeIf { it.exists() }?.readText()?.let(KdfMetadata::fromJson)
    }.getOrNull()

    fun writeMetadata(metadata: KdfMetadata) {
        metadataFile.parentFile?.mkdirs()
        val temp = File(metadataFile.parentFile, "$DATABASE_NAME.meta.tmp")
        temp.writeText(metadata.toJson())
        if (!temp.renameTo(metadataFile)) {
            metadataFile.delete()
            check(temp.renameTo(metadataFile)) { "Could not persist KDF metadata" }
        }
    }

    fun deleteMetadata() {
        metadataFile.delete()
        File(metadataFile.path + ".tmp").delete()
    }

    fun deleteAll() {
        databaseFile.delete()
        File(databaseFile.path + "-wal").delete()
        File(databaseFile.path + "-shm").delete()
        File(databaseFile.path + "-journal").delete()
        deleteMetadata()
    }

    companion object {
        const val DATABASE_NAME = "talex.db"
        const val SIDECAR_SUFFIX = ".meta"
    }
}
