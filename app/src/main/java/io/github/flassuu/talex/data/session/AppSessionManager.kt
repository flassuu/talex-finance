package io.github.flassuu.talex.data.session

import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.data.db.DatabaseProvider
import io.github.flassuu.talex.data.security.Argon2KeyDeriver
import io.github.flassuu.talex.data.security.DatabaseFactory
import io.github.flassuu.talex.data.security.BiometricKeyStore
import io.github.flassuu.talex.data.security.DatabaseFileStore
import io.github.flassuu.talex.data.security.KdfMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val fileStore: DatabaseFileStore,
    private val keyDeriver: Argon2KeyDeriver,
    private val databaseFactory: DatabaseFactory,
    private val provider: DatabaseProvider,
    private val biometricKeyStore: BiometricKeyStore,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(
        if (fileStore.hasMetadata()) LockState.LOCKED else LockState.NOT_INITIALIZED,
    )
    val state: StateFlow<LockState> = _state.asStateFlow()

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
            if (biometricKeyStore.isAvailable() && !biometricKeyStore.hasStoredKey()) {
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

    fun hasBiometricKey(): Boolean = biometricKeyStore.hasStoredKey()

    fun enableBiometric(): Boolean {
        val key = currentKey ?: return false
        return biometricKeyStore.store(key.copyOf())
    }

    fun decryptBiometricKey(): ByteArray? = biometricKeyStore.decryptStoredKey()

    fun disableBiometric() = biometricKeyStore.clear()

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
