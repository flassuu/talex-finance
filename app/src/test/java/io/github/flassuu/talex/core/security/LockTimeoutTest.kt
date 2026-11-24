package io.github.flassuu.talex.core.security

import org.junit.Assert.assertEquals
import org.junit.Test

class LockTimeoutTest {

    @Test
    fun knownValuesParseByEnumName() {
        assertEquals(LockTimeout.IMMEDIATELY, LockTimeout.fromName("IMMEDIATELY"))
        assertEquals(LockTimeout.FIFTEEN_MINUTES, LockTimeout.fromName("FIFTEEN_MINUTES"))
    }

    @Test
    fun unknownOrMissingValueFallsBackToTheDefault() {
        assertEquals(LockTimeout.DEFAULT, LockTimeout.fromName(null))
        assertEquals(LockTimeout.DEFAULT, LockTimeout.fromName("FOREVER"))
    }

    @Test
    fun millisMatchTheLabels() {
        assertEquals(0L, LockTimeout.IMMEDIATELY.millis)
        assertEquals(30_000L, LockTimeout.THIRTY_SECONDS.millis)
        assertEquals(60_000L, LockTimeout.ONE_MINUTE.millis)
        assertEquals(300_000L, LockTimeout.FIVE_MINUTES.millis)
        assertEquals(900_000L, LockTimeout.FIFTEEN_MINUTES.millis)
    }
}
