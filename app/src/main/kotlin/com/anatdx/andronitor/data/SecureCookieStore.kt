package com.anatdx.andronitor.data

import android.content.Context
import android.util.Base64
import com.anatdx.andronitor.model.Account
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

class SecureCookieStore(context: Context) {
    private val preferences = context.getSharedPreferences("secure_credentials", Context.MODE_PRIVATE)
    private val keyAlias = "andronitor_cookie_key"

    fun saveCookie(accountId: String, cookie: String) {
        val encrypted = encrypt(cookie.toByteArray(StandardCharsets.UTF_8))
        preferences.edit().putString("cookie_$accountId", encrypted).apply()
    }

    fun getCookie(accountId: String): String? = preferences.getString("cookie_$accountId", null)
        ?.let { runCatching { String(decrypt(it), StandardCharsets.UTF_8) }.getOrNull() }

    fun deleteCookie(accountId: String) {
        preferences.edit().remove("cookie_$accountId").apply()
    }

    fun deleteAllCookies() {
        preferences.edit().remove("active_account_id").apply()
        preferences.all.keys.filter { it.startsWith("cookie_") }.forEach { key ->
            preferences.edit().remove(key).apply()
        }
    }

    fun activeAccountId(): String? = preferences.getString("active_account_id", null)

    fun setActiveAccountId(accountId: String?) {
        preferences.edit().apply {
            if (accountId == null) remove("active_account_id") else putString("active_account_id", accountId)
        }.apply()
    }

    fun attachCookies(accounts: List<Account>): List<Account> = accounts.map { it.copy() }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(keyAlias, null)
        if (existing is SecretKey) return existing
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
        }.generateKey().also {
            // The generated key is persisted by AndroidKeyStore under keyAlias.
        }
    }

    private fun encrypt(bytes: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val payload = cipher.iv + cipher.doFinal(bytes)
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): ByteArray {
        val payload = Base64.decode(value, Base64.NO_WRAP)
        require(payload.size > 12) { "Invalid encrypted cookie" }
        val iv = payload.copyOfRange(0, 12)
        val encrypted = payload.copyOfRange(12, payload.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted)
    }
}
