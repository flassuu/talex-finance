package io.github.flassuu.talex.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PassphrasePolicyTest {

    @Test
    fun acceptsExactlyTheMinimumLength() {
        assertTrue(PassphrasePolicy.isLongEnough("12345678"))
        assertTrue(PassphrasePolicy.isLongEnough("12345678".toCharArray()))
    }

    @Test
    fun rejectsShorterPassphrases() {
        assertFalse(PassphrasePolicy.isLongEnough("1234567"))
        assertFalse(PassphrasePolicy.isLongEnough("1234567".toCharArray()))
    }

    @Test
    fun countsUnicodeCharactersAsCharacters() {
        assertTrue(PassphrasePolicy.isLongEnough("пароль12"))
    }
}
