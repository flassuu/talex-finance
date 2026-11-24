package io.github.flassuu.talex.data.session

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.core.security.PassphrasePolicy
import io.github.flassuu.talex.data.db.DatabaseProvider
import io.github.flassuu.talex.data.db.TalexDatabase
import io.github.flassuu.talex.data.security.Argon2KeyDeriver
import io.github.flassuu.talex.data.security.BackupContainer
import io.github.flassuu.talex.data.security.DatabaseFactory
import io.github.flassuu.talex.data.security.BiometricKeyStore
import io.github.flassuu.talex.data.security.DatabaseFileStore
import io.github.flassuu.talex.data.security.KdfMetadata
import io.github.flassuu.talex.data.security.SecurityPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

enum class LockState {
    NOT_INITIALIZED,
    LOCKED,
    UNLOCKED,
}

/**
 * Owns the unlocked database for the process lifetime. The derived key and the open
 * database exist in memory only; locking closes the database and wipes the key.
 */
@Singleton
class AppSessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileStore: DatabaseFileStore,
    private val keyDeriver: Argon2KeyDeriver,
    private val databaseFactory: DatabaseFactory,
    private val provider: DatabaseProvider,
    private val biometricKeyStore: BiometricKeyStore,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(LockState.LOCKED)
    val state: StateFlow<LockState> = _state.asStateFlow()

    init {
        recoverInterruptedRekey()
        _state.value = if (fileStore.hasMetadata()) LockState.LOCKED else LockState.NOT_INITIALIZED
    }

    @Volatile
    private var currentKey: ByteArray? = null

    suspend fun setup(passphrase: CharArray): Outcome<Unit> = withContext(Dispatchers.Default) {
        try {
            val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
            val metadata = KdfMetadata.create(salt)
            val key = keyDeriver.derive(passphrase, metadata)
            fileStore.writeMetadata(metadata)
            try {
                openInto(key)
            } catch (t: Throwable) {
                fileStore.deleteMetadata()
                throw t
            }
            Outcome.Success(Unit)
        } catch (t: Throwable) {
            Outcome.Failure(AppError.CRYPTO_FAILED)
        } finally {
            passphrase.fill('\u0000')
        }
    }

    suspend fun unlock(passphrase: CharArray): Outcome<Unit> = withContext(Dispatchers.Default) {
        try {
            val metadata = fileStore.readMetadata()
                ?: return@withContext Outcome.Failure(AppError.DATABASE_LOCKED)
            val key = keyDeriver.derive(passphrase, metadata)
            openInto(key)
            confirmRekey()
            if (SecurityPreferences.isBiometricEnabled(context) &&
                biometricKeyStore.isAvailable() &&
                !biometricKeyStore.hasStoredKey()
            ) {
                biometricKeyStore.store(key.copyOf())
            }
            _state.value = LockState.UNLOCKED
            Outcome.Success(Unit)
        } catch (t: Throwable) {
            Outcome.Failure(AppError.WRONG_PASSPHRASE)
        } finally {
            passphrase.fill('\u0000')
        }
    }

    suspend fun unlockWithKey(key: ByteArray): Outcome<Unit> = withContext(Dispatchers.Default) {
        try {
            openInto(key)
            confirmRekey()
            _state.value = LockState.UNLOCKED
            Outcome.Success(Unit)
        } catch (t: Throwable) {
            Outcome.Failure(AppError.CRYPTO_FAILED)
        }
    }

    /** Called after first-run setup finishes its follow-up writes (for example the base currency). */
    fun markUnlocked() {
        if (_state.value != LockState.UNLOCKED) _state.value = LockState.UNLOCKED
    }

    fun lock() {
        if (_state.value != LockState.UNLOCKED) return
        _state.value = LockState.LOCKED

        val key = currentKey
        currentKey = null
        val database = provider.get()
        provider.set(null)

        scope.launch(Dispatchers.IO) {
            runCatching { database?.close() }
            key?.fill(0)
        }
    }

    fun isBiometricAvailable(): Boolean = biometricKeyStore.isAvailable()

    fun isBiometricEnabled(): Boolean = SecurityPreferences.isBiometricEnabled(context)

    fun hasBiometricKey(): Boolean = biometricKeyStore.hasStoredKey()

    fun enableBiometric(): Boolean {
        val key = currentKey ?: return false
        val stored = biometricKeyStore.store(key.copyOf())
        if (stored) SecurityPreferences.setBiometricEnabled(context, true)
        return stored
    }

    fun decryptBiometricKey(): ByteArray? = biometricKeyStore.decryptStoredKey()

    fun disableBiometric() {
        biometricKeyStore.clear()
        SecurityPreferences.setBiometricEnabled(context, false)
    }

    /**
     * Changes the master passphrase by exporting a copy encrypted with the new
     * key and swapping it in (ADR-0029). The app locks afterwards so the user
     * proves the new passphrase and every data flow binds to the new file.
     */
    suspend fun changePassphrase(current: CharArray, new: CharArray): Outcome<Unit> =
        withContext(Dispatchers.Default) {
            var newKey: ByteArray? = null
            try {
                if (!PassphrasePolicy.isLongEnough(new)) {
                    return@withContext Outcome.Failure(AppError.PASSPHRASE_TOO_SHORT)
                }
                val oldKey = currentKey ?: return@withContext Outcome.Failure(AppError.DATABASE_LOCKED)
                val oldMetadata = fileStore.readMetadata()
                    ?: return@withContext Outcome.Failure(AppError.DATABASE_LOCKED)
                val database = provider.get()
                    ?: return@withContext Outcome.Failure(AppError.DATABASE_LOCKED)

                val derivedOld = keyDeriver.derive(current, oldMetadata)
                val matches = MessageDigest.isEqual(derivedOld, oldKey)
                derivedOld.fill(0)
                if (!matches) return@withContext Outcome.Failure(AppError.WRONG_PASSPHRASE)

                val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
                val newMetadata = KdfMetadata.create(salt)
                newKey = keyDeriver.derive(new, newMetadata)

                exportRekeyedCopy(database, newKey)
                closeDatabase()
                installRekeyedCopy(oldMetadata, newMetadata)

                val restoreBiometric = SecurityPreferences.isBiometricEnabled(context) &&
                    biometricKeyStore.isAvailable()
                biometricKeyStore.clear()
                val keyToOpen = newKey
                newKey = null
                openInto(keyToOpen)
                fileStore.rekeyBackupFile.delete()
                fileStore.rekeyOldMetadataFile.delete()
                if (restoreBiometric) biometricKeyStore.store(keyToOpen.copyOf())
                // Locking proves the new passphrase and rebinds every data flow.
                lock()
                Outcome.Success(Unit)
            } catch (t: Throwable) {
                runCatching { rollbackPendingRekey() }
                if (provider.get() == null && currentKey != null && fileStore.databaseFile.exists()) {
                    val oldKey = currentKey ?: return@withContext Outcome.Failure(AppError.UNKNOWN)
                    runCatching { openInto(oldKey) }
                }
                Outcome.Failure(AppError.UNKNOWN)
            } finally {
                newKey?.fill(0)
                current.fill('\u0000')
                new.fill('\u0000')
            }
        }

    /** Writes the encrypted snapshot to [uri] as a single-file container (ADR-0028). */
    suspend fun exportBackup(uri: Uri): Outcome<Unit> = withContext(Dispatchers.IO) {
        try {
            val database = provider.get() ?: return@withContext Outcome.Failure(AppError.DATABASE_LOCKED)
            // Fold the WAL back into the main file so the copy is complete.
            database.openHelper.writableDatabase
                .query("PRAGMA wal_checkpoint(TRUNCATE)")
                .use { it.moveToFirst() }
            val metadata = fileStore.readMetadata()
                ?: return@withContext Outcome.Failure(AppError.BACKUP_INVALID)
            val stream = context.contentResolver.openOutputStream(uri)
                ?: return@withContext Outcome.Failure(AppError.BACKUP_INVALID)
            stream.use { output ->
                fileStore.databaseFile.inputStream().use { db ->
                    metadata.toJson().byteInputStream().use { meta ->
                        BackupContainer.write(output, db, meta)
                    }
                }
            }
            Outcome.Success(Unit)
        } catch (t: Throwable) {
            Outcome.Failure(AppError.UNKNOWN)
        }
    }

    /**
     * Replaces the working database with the snapshot from [uri]. The passphrase is
     * proven against the snapshot before anything on disk changes.
     */
    suspend fun restoreBackup(uri: Uri, passphrase: CharArray): Outcome<Unit> =
        withContext(Dispatchers.IO) {
            val tempDir = File(context.cacheDir, "restore-${System.currentTimeMillis()}")
            try {
                if (!tempDir.mkdirs() && !tempDir.isDirectory) {
                    return@withContext Outcome.Failure(AppError.BACKUP_INVALID)
                }
                val tempDb = File(tempDir, BackupContainer.DATABASE_ENTRY)
                val tempMeta = File(tempDir, BackupContainer.METADATA_ENTRY)
                val input = context.contentResolver.openInputStream(uri)
                    ?: return@withContext Outcome.Failure(AppError.BACKUP_INVALID)
                val containerVersion = input.use { source ->
                    tempDb.outputStream().use { dbOut ->
                        tempMeta.outputStream().use { metaOut ->
                            BackupContainer.read(source, dbOut, metaOut)
                        }
                    }
                }
                if (containerVersion > BackupContainer.FORMAT_VERSION) {
                    return@withContext Outcome.Failure(AppError.BACKUP_VERSION_UNSUPPORTED)
                }
                val metadata = runCatching { KdfMetadata.fromJson(tempMeta.readText()) }.getOrNull()
                    ?: return@withContext Outcome.Failure(AppError.BACKUP_INVALID)
                if (metadata.version > KdfMetadata.CURRENT_VERSION ||
                    metadata.algorithm != KdfMetadata.ALGORITHM
                ) {
                    return@withContext Outcome.Failure(AppError.BACKUP_VERSION_UNSUPPORTED)
                }

                // Prove the passphrase opens the snapshot before touching live data.
                val key = keyDeriver.derive(passphrase, metadata)
                val valid = runCatching {
                    val probe = SQLiteDatabase.openDatabase(
                        tempDb.absolutePath,
                        key,
                        null,
                        SQLiteDatabase.OPEN_READONLY,
                        null,
                    )
                    try {
                        probe.rawQuery("SELECT count(*) FROM sqlite_master", null)
                            .use { it.moveToFirst() }
                    } finally {
                        probe.close()
                    }
                }.isSuccess
                if (!valid) return@withContext Outcome.Failure(AppError.WRONG_PASSPHRASE)

                val current = provider.get()
                runCatching {
                    val database = current ?: return@runCatching
                    database.openHelper.writableDatabase
                        .query("PRAGMA wal_checkpoint(TRUNCATE)")
                        .use { it.moveToFirst() }
                }
                provider.set(null)
                runCatching { current?.close() }

                val staged = File(fileStore.databaseFile.path + ".staged")
                tempDb.copyTo(staged, overwrite = true)
                listOf("-wal", "-shm", "-journal").forEach { suffix ->
                    File(fileStore.databaseFile.path + suffix).delete()
                }
                if (!staged.renameTo(fileStore.databaseFile)) {
                    staged.delete()
                    // Put the previous database back into service; nothing was lost.
                    currentKey?.let { runCatching { openInto(it) } }
                    return@withContext Outcome.Failure(AppError.BACKUP_INVALID)
                }
                fileStore.writeMetadata(metadata)
                // The old wrapped key belongs to the previous database; drop it.
                biometricKeyStore.clear()
                // Force a fresh unlock so every data flow re-binds to the restored file.
                val oldKey = currentKey
                currentKey = null
                oldKey?.fill(0)
                key.fill(0)
                _state.value = LockState.LOCKED
                Outcome.Success(Unit)
            } catch (t: Throwable) {
                Outcome.Failure(AppError.BACKUP_INVALID)
            } finally {
                passphrase.fill('\u0000')
                tempDir.deleteRecursively()
            }
        }

    /**
     * Finishes or rolls back a passphrase change that was interrupted by a crash
     * (ADR-0029). Runs before the first unlock, when only files are available.
     */
    private fun recoverInterruptedRekey() {
        val marker = fileStore.rekeyOldMetadataFile
        if (!marker.exists()) {
            fileStore.rekeyTempFile.delete()
            fileStore.rekeyBackupFile.delete()
            return
        }
        val oldMetadata = runCatching { KdfMetadata.fromJson(marker.readText()) }.getOrNull()
        if (oldMetadata == null) {
            marker.delete()
            return
        }
        val currentMetadata = fileStore.readMetadata()
        if (currentMetadata == oldMetadata) {
            rollbackPendingRekey()
        }
        // Otherwise the new sidecar is in place; the previous copy is kept until
        // the new passphrase opens the database, then removed by confirmRekey().
    }

    private fun rollbackPendingRekey() {
        val marker = fileStore.rekeyOldMetadataFile
        if (!marker.exists()) return
        val oldMetadata = runCatching { KdfMetadata.fromJson(marker.readText()) }.getOrNull() ?: return
        fileStore.rekeyTempFile.delete()
        // Only replace the database when the previous copy was actually moved
        // aside; otherwise the current file is still the original one.
        if (fileStore.rekeyBackupFile.exists()) {
            fileStore.databaseFile.delete()
            File(fileStore.databaseFile.path + "-wal").delete()
            File(fileStore.databaseFile.path + "-shm").delete()
            File(fileStore.databaseFile.path + "-journal").delete()
            fileStore.rekeyBackupFile.renameTo(fileStore.databaseFile)
        }
        fileStore.writeMetadata(oldMetadata)
        marker.delete()
    }

    /** Removes the rollback copy once the new passphrase has opened the file. */
    private fun confirmRekey() {
        if (fileStore.rekeyOldMetadataFile.exists()) {
            fileStore.rekeyBackupFile.delete()
            fileStore.rekeyOldMetadataFile.delete()
        }
    }

    private fun exportRekeyedCopy(database: TalexDatabase, newKey: ByteArray) {
        val temp = fileStore.rekeyTempFile
        temp.delete()
        val sql = database.openHelper.writableDatabase as? SQLiteDatabase
            ?: error("SQLCipher database expected")
        sql.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
        val hex = newKey.joinToString("") { "%02x".format(it) }
        val escapedPath = temp.absolutePath.replace("'", "''")
        sql.rawExecSQL("ATTACH DATABASE '$escapedPath' AS rekeyed KEY \"x'$hex'\"")
        try {
            sql.rawExecSQL("SELECT sqlcipher_export('rekeyed')")
        } finally {
            runCatching { sql.rawExecSQL("DETACH DATABASE rekeyed") }
        }
    }

    private fun installRekeyedCopy(oldMetadata: KdfMetadata, newMetadata: KdfMetadata) {
        fileStore.rekeyBackupFile.delete()
        File(fileStore.databaseFile.path + "-wal").delete()
        File(fileStore.databaseFile.path + "-shm").delete()
        File(fileStore.databaseFile.path + "-journal").delete()
        fileStore.rekeyOldMetadataFile.writeText(oldMetadata.toJson())
        check(fileStore.databaseFile.renameTo(fileStore.rekeyBackupFile)) {
            "Could not move the old database"
        }
        check(fileStore.rekeyTempFile.renameTo(fileStore.databaseFile)) {
            "Could not install the re-keyed database"
        }
        fileStore.writeMetadata(newMetadata)
    }

    private fun closeDatabase() {
        val database = provider.get()
        provider.set(null)
        runCatching { database?.close() }
    }

    private fun openInto(key: ByteArray) {
        val database = databaseFactory.build(key)
        try {
            // Forces the first read, so a wrong key fails here instead of on a later query.
            database.openHelper.writableDatabase
        } catch (t: Throwable) {
            database.close()
            throw t
        }
        provider.set(database)
        currentKey = key
    }

    companion object {
        const val SALT_BYTES = 16
    }
}
