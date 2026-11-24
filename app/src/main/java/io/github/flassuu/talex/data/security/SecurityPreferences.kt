package io.github.flassuu.talex.data.security

import android.content.Context
import io.github.flassuu.talex.core.security.LockTimeout

/**
 * Plaintext preferences that the lock decision needs before the database is
 * open (ADR-0030). They contain no secrets, so they live outside the encrypted
 * store, next to the language choice.
 */
object SecurityPreferences {

    private const val PREFS = "talex.security"
    private const val KEY_LOCK_TIMEOUT = "lock_timeout"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"

    fun lockTimeout(context: Context): LockTimeout =
        LockTimeout.fromName(prefs(context).getString(KEY_LOCK_TIMEOUT, null))

    fun setLockTimeout(context: Context, timeout: LockTimeout) {
        prefs(context).edit().putString(KEY_LOCK_TIMEOUT, timeout.name).apply()
    }

    /** Biometric unlock is offered by default; the user can turn it off. */
    fun isBiometricEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, true)

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
