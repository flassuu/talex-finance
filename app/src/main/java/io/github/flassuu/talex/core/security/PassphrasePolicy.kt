package io.github.flassuu.talex.core.security

/**
 * Rules for the master passphrase. The passphrase is the root of the portable
 * file (ADR-0004), so the minimum length is shared by setup and the change flow.
 */
object PassphrasePolicy {

    const val MIN_LENGTH = 8

    fun isLongEnough(passphrase: CharSequence): Boolean = passphrase.length >= MIN_LENGTH

    fun isLongEnough(passphrase: CharArray): Boolean = passphrase.size >= MIN_LENGTH
}
