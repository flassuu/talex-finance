package io.github.flassuu.talex.data.security

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Derives the 32-byte database key from the master passphrase with Argon2id
 * (ADR-0007). The passphrase is used in memory only and never persisted.
 */
@Singleton
class Argon2KeyDeriver @Inject constructor() {

    private val argon2 = Argon2Kt()

    fun derive(passphrase: CharArray, metadata: KdfMetadata): ByteArray {
        val result = argon2.hash(
            mode = Argon2Mode.ARGON2_ID,
            password = passphrase.concatToString().encodeToByteArray(),
            salt = metadata.salt(),
            tCostInIterations = metadata.iterations,
            mCostInKibibyte = metadata.memoryKib,
            parallelism = metadata.parallelism,
            hashLengthInBytes = metadata.hashLengthBytes,
        )
        return result.rawHashAsByteArray()
    }
}
