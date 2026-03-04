package com.unistream.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "unistream_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_APP_PIN = "app_pin"
        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_AUTO_LOCK_TIMEOUT = "auto_lock_timeout_ms"
        private const val KEY_GALLERY_LOCK = "gallery_lock"
        private const val KEY_STREAMING_LOCK = "streaming_lock"
        private const val KEY_NOVEL_LOCK = "novel_lock"
        private const val KEY_HIDDEN_VAULT_PIN = "hidden_vault_pin"
        private const val KEY_FAKE_VAULT_PIN = "fake_vault_pin"
        private const val KEY_FAKE_VAULT_ENABLED = "fake_vault_enabled"

        const val AUTO_LOCK_IMMEDIATE = 0L
        const val AUTO_LOCK_1_MIN = 60_000L
        const val AUTO_LOCK_5_MIN = 300_000L
        const val AUTO_LOCK_15_MIN = 900_000L
        const val AUTO_LOCK_NEVER = -1L
    }

    // App Lock
    var appPin: String
        get() = prefs.getString(KEY_APP_PIN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_APP_PIN, value).apply()

    var isAppLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_LOCK_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, value).apply()

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    var autoLockTimeoutMs: Long
        get() = prefs.getLong(KEY_AUTO_LOCK_TIMEOUT, AUTO_LOCK_5_MIN)
        set(value) = prefs.edit().putLong(KEY_AUTO_LOCK_TIMEOUT, value).apply()

    // Module-level locks
    var isGalleryLocked: Boolean
        get() = prefs.getBoolean(KEY_GALLERY_LOCK, false)
        set(value) = prefs.edit().putBoolean(KEY_GALLERY_LOCK, value).apply()

    var isStreamingLocked: Boolean
        get() = prefs.getBoolean(KEY_STREAMING_LOCK, false)
        set(value) = prefs.edit().putBoolean(KEY_STREAMING_LOCK, value).apply()

    var isNovelLocked: Boolean
        get() = prefs.getBoolean(KEY_NOVEL_LOCK, false)
        set(value) = prefs.edit().putBoolean(KEY_NOVEL_LOCK, value).apply()

    // Hidden Vault
    var hiddenVaultPin: String
        get() = prefs.getString(KEY_HIDDEN_VAULT_PIN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_HIDDEN_VAULT_PIN, value).apply()

    var fakeVaultPin: String
        get() = prefs.getString(KEY_FAKE_VAULT_PIN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_FAKE_VAULT_PIN, value).apply()

    var isFakeVaultEnabled: Boolean
        get() = prefs.getBoolean(KEY_FAKE_VAULT_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_FAKE_VAULT_ENABLED, value).apply()

    fun verifyPin(input: String): PinVerificationResult {
        val realPin = appPin
        val fakePin = fakeVaultPin
        return when {
            realPin.isEmpty() -> PinVerificationResult.NOT_SET
            input == realPin -> PinVerificationResult.CORRECT
            isFakeVaultEnabled && input == fakePin -> PinVerificationResult.FAKE_VAULT
            else -> PinVerificationResult.INCORRECT
        }
    }

    fun verifyVaultPin(input: String): PinVerificationResult {
        val realPin = hiddenVaultPin
        val fakePin = fakeVaultPin
        return when {
            realPin.isEmpty() -> PinVerificationResult.NOT_SET
            input == realPin -> PinVerificationResult.CORRECT
            isFakeVaultEnabled && input == fakePin -> PinVerificationResult.FAKE_VAULT
            else -> PinVerificationResult.INCORRECT
        }
    }
}

enum class PinVerificationResult {
    CORRECT, INCORRECT, NOT_SET, FAKE_VAULT
}
