package com.unistream.core.security

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val securityPreferences: SecurityPreferences,
    private val biometricAuthManager: BiometricAuthManager
) : ViewModel() {

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    init {
        viewModelScope.launch {
            val hasPin = securityPreferences.appPin.isNotBlank()
            val biometricAvailable = biometricAuthManager.getBiometricStatus() == BiometricAuthManager.BiometricStatus.AVAILABLE
            val hasBiometricPath = securityPreferences.isBiometricEnabled && biometricAvailable
            _isLocked.value = securityPreferences.isAppLockEnabled && (hasPin || hasBiometricPath)
            _isInitializing.value = false
        }
    }

    fun authenticate(activity: FragmentActivity) {

        if (!securityPreferences.isBiometricEnabled) return

        when (biometricAuthManager.getBiometricStatus()) {

            BiometricAuthManager.BiometricStatus.AVAILABLE -> {

                biometricAuthManager.authenticate(
                    activity = activity,
                    onSuccess = { _isLocked.value = false },
                    onError = { _, message -> _authError.value = message }
                )
            }

            BiometricAuthManager.BiometricStatus.NOT_ENROLLED ->
                _authError.value = "No biometric enrolled"

            BiometricAuthManager.BiometricStatus.HARDWARE_UNAVAILABLE ->
                _authError.value = "Biometric hardware unavailable"

            BiometricAuthManager.BiometricStatus.NOT_SUPPORTED ->
                _authError.value = "Biometric not supported"
        }
    }

    fun authenticateWithPin(pin: String): Boolean {
        val result = securityPreferences.verifyPin(pin)
        return when (result) {
            PinVerificationResult.CORRECT -> {
                _isLocked.value = false
                true
            }
            PinVerificationResult.NOT_SET -> {
                _authError.value = "PIN not set. Configure app lock in Settings."
                false
            }
            PinVerificationResult.FAKE_VAULT -> false
            PinVerificationResult.INCORRECT -> {
                _authError.value = "Incorrect PIN"
                false
            }
        }
    }

    fun lock() {
        if (!securityPreferences.isAppLockEnabled) return

        val hasPin = securityPreferences.appPin.isNotBlank()
        val biometricAvailable = biometricAuthManager.getBiometricStatus() == BiometricAuthManager.BiometricStatus.AVAILABLE
        val hasBiometricPath = securityPreferences.isBiometricEnabled && biometricAvailable
        if (hasPin || hasBiometricPath) {
            _isLocked.value = true
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }
}
