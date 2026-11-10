package io.github.flassuu.talex.data.security

import android.util.Base64
import org.json.JSONObject

/**
 * The portable header that makes the Argon2id key derivable before the database is
 * unlocked. It lives in a small sidecar file next to the encrypted database
 * (ADR-0019). It contains no secret: the salt and cost parameters are public.
 */
data class KdfMetadata(
    val version: Int,
    val algorithm: String,
    val memoryKib: Int,
    val iterations: Int,
    val parallelism: Int,
    val saltBase64: String,
    val hashLengthBytes: Int,
) {

    fun salt(): ByteArray = Base64.decode(saltBase64, Base64.NO_WRAP)

    fun toJson(): String = JSONObject().apply {
        put("version", version)
        put("algorithm", algorithm)
        put("memoryKib", memoryKib)
        put("iterations", iterations)
        put("parallelism", parallelism)
        put("salt", saltBase64)
        put("hashLengthBytes", hashLengthBytes)
    }.toString(2)

    companion object {
        const val CURRENT_VERSION = 1
        const val ALGORITHM = "argon2id"

        // Provisional, tuned on target devices later (ADR-0007).
        const val MEMORY_KIB = 64 * 1024
        const val ITERATIONS = 3
        const val PARALLELISM = 1
        const val HASH_LENGTH = 32

        fun create(salt: ByteArray): KdfMetadata = KdfMetadata(
            version = CURRENT_VERSION,
            algorithm = ALGORITHM,
            memoryKib = MEMORY_KIB,
            iterations = ITERATIONS,
            parallelism = PARALLELISM,
            saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP),
            hashLengthBytes = HASH_LENGTH,
        )

        fun fromJson(text: String): KdfMetadata {
            val json = JSONObject(text)
            return KdfMetadata(
                version = json.getInt("version"),
                algorithm = json.getString("algorithm"),
                memoryKib = json.getInt("memoryKib"),
                iterations = json.getInt("iterations"),
                parallelism = json.getInt("parallelism"),
                saltBase64 = json.getString("salt"),
                hashLengthBytes = json.getInt("hashLengthBytes"),
            )
        }
    }
}
