package io.github.flassuu.talex.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the app-managed passphrase of the demo profile (ADR-0031). The
 * passphrase is wrapped by an app-private Keystore key without user
 * authentication, so the demo opens on launch; it never protects real data.
 */
@Singleton
class DemoProfileStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    fun isActive(): Boolean =
        prefs.contains(KEY_CIPHERTEXT) &&
            prefs.contains(KEY_IV) &&
            runCatching { keyStore.containsAlias(ALIAS) }.getOrDefault(false)

    fun activate(passphrase: String): Boolean = runCatching {
        val secretKey = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val ciphertext = cipher.doFinal(passphrase.toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString(KEY_CIPHERTEXT, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            .apply()
        true
    }.getOrDefault(false)

    fun passphrase(): CharArray? = runCatching {
        val iv = prefs.getString(KEY_IV, null) ?: return null
        val ciphertext = prefs.getString(KEY_CIPHERTEXT, null) ?: return null
        val secretKey = keyStore.getKey(ALIAS, null) as SecretKey
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(
                Cipher.DECRYPT_MODE,
                secretKey,
                GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)),
            )
        }
        String(
            cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)),
            Charsets.UTF_8,
        ).toCharArray()
    }.getOrNull()

    fun clear() {
        prefs.edit().clear().apply()
        runCatching { keyStore.deleteEntry(ALIAS) }
    }

    private fun getOrCreateKey(): SecretKey {
        val existing = runCatching { keyStore.getKey(ALIAS, null) as? SecretKey }.getOrNull()
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return generator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val ALIAS = "talex.demo.key.v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val PREFS = "talex.demo"
        private const val KEY_IV = "iv"
        private const val KEY_CIPHERTEXT = "ciphertext"
    }
}
