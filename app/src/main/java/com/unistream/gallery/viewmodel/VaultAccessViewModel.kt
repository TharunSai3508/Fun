package com.unistream.gallery.viewmodel

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import com.unistream.core.security.BiometricAuthManager
import com.unistream.core.security.PinVerificationResult
import com.unistream.core.security.SecurityPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class VaultAccessViewModel @Inject constructor(
    private val securityPreferences: SecurityPreferences,
    private val biometricAuthManager: BiometricAuthManager
) : ViewModel() {

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val hasVaultPin: Boolean
        get() = securityPreferences.hiddenVaultPin.isNotBlank()

    val biometricAvailable: Boolean
        get() = biometricAuthManager.getBiometricStatus() == BiometricAuthManager.BiometricStatus.AVAILABLE

    fun setupVaultPin(pin: String, confirmPin: String): Boolean {
        return when {
            pin.length < 4 -> {
                _errorMessage.value = "PIN must be at least 4 digits"
                false
            }
            pin != confirmPin -> {
                _errorMessage.value = "PINs do not match"
                false
            }
            else -> {
                securityPreferences.hiddenVaultPin = pin
                _errorMessage.value = null
                true
            }
        }
    }

    fun authenticateWithPin(pin: String): Boolean {
        return when (securityPreferences.verifyVaultPin(pin)) {
            PinVerificationResult.CORRECT -> {
                _isUnlocked.value = true
                _errorMessage.value = null
                true
            }
            PinVerificationResult.FAKE_VAULT -> {
                _errorMessage.value = "Decoy vault mode detected"
                false
            }
            PinVerificationResult.NOT_SET -> {
                _errorMessage.value = "Vault PIN not setup"
                false
            }
            PinVerificationResult.INCORRECT -> {
                _errorMessage.value = "Wrong PIN"
                false
            }
        }
    }

    fun authenticateWithBiometric(context: Context) {
        val activity = context as? FragmentActivity ?: return
        biometricAuthManager.authenticate(
            activity = activity,
            title = "Unlock Hidden Vault",
            subtitle = "Authenticate to access private media",
            onSuccess = {
                _isUnlocked.value = true
                _errorMessage.value = null
            },
            onError = { _, message ->
                _errorMessage.value = message
            }
        )
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
