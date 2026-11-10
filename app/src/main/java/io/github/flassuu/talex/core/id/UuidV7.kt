package io.github.flassuu.talex.core.id

import java.security.SecureRandom
import java.util.UUID

/**
 * Generates time-ordered UUIDv7 identifiers (D-033). Stored as text in SQLite.
 */
object UuidV7 {

    private val random = SecureRandom()

    fun generate(): String = generate(System.currentTimeMillis())

    fun generate(timestampMillis: Long): String {
        val timestamp = timestampMillis and 0xFFFFFFFFFFFFL

        val randA = (random.nextInt() and 0x0FFF).toLong()
        val mostSigBits = (timestamp shl 16) or 0x7000L or randA

        var leastSigBits = random.nextLong()
        // Variant 10: clear both top bits, then set the most significant one.
        leastSigBits = (leastSigBits and 0x3FFFFFFFFFFFFFFFL) or Long.MIN_VALUE

        return UUID(mostSigBits, leastSigBits).toString()
    }
}
