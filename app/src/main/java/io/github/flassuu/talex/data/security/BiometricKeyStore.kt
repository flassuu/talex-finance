package io.github.flassuu.talex.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores a Keystore-wrapped copy of the database key so biometrics can unlock it
 * locally. This is convenience only: the master passphrase remains the root of
 * trust and still opens the file if the wrapped copy is invalidated (ADR-0004).
 */
@Singleton
class BiometricKeyStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    fun isAvailable(): Boolean {
        val manager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        return manager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun hasStoredKey(): Boolean = prefs.contains(KEY_CIPHERTEXT) &&
        prefs.contains(KEY_IV) &&
        runCatching { keyStore.containsAlias(ALIAS) }.getOrDefault(false)

    fun store(dbKey: ByteArray): Boolean = runCatching {
        val secretKey = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val ciphertext = cipher.doFinal(dbKey)
        prefs.edit()
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString(KEY_CIPHERTEXT, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            .apply()
        true
    }.getOrDefault(false)

    fun createDecryptCipher(): Cipher? {
        val ivBase64 = prefs.getString(KEY_IV, null) ?: return null
        return runCatching {
            val secretKey = keyStore.getKey(ALIAS, null) as SecretKey
            Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, Base64.decode(ivBase64, Base64.NO_WRAP)))
            }
        }.getOrNull()
    }

    fun decrypt(cipher: Cipher): ByteArray? {
        val ciphertext = prefs.getString(KEY_CIPHERTEXT, null) ?: return null
        return runCatching { cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)) }.getOrNull()
    }

    fun decryptStoredKey(): ByteArray? {
        val cipher = createDecryptCipher() ?: return null
        return decrypt(cipher)
    }

    fun clear() {
        prefs.edit().clear().apply()
        runCatching { keyStore.deleteEntry(ALIAS) }
        // Remove the alias used by the first Phase 1 build.
        runCatching { keyStore.deleteEntry(LEGACY_ALIAS) }
    }

    private fun getOrCreateKey(): SecretKey {
        val existing = runCatching { keyStore.getKey(ALIAS, null) as? SecretKey }.getOrNull()
        if (existing != null) return existing

        runCatching { keyStore.deleteEntry(ALIAS) }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                // The key stays inside the app sandbox. BiometricPrompt is the app-level
                // gate before this key is used; it is convenience, not the root of trust.
                .build(),
        )
        return generator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val ALIAS = "talex.biometric.key.v2"
        private const val LEGACY_ALIAS = "talex.biometric.key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val PREFS = "talex.biometric"
        private const val KEY_IV = "iv"
        private const val KEY_CIPHERTEXT = "ciphertext"
    }
}
