package com.unistream.settings.viewmodel

import androidx.lifecycle.ViewModel
import com.unistream.core.security.BiometricAuthManager
import com.unistream.core.security.SecurityPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class SecuritySettingsState(
    val isAppLockEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = true,
    val hasAppPin: Boolean = false,
    val biometricAvailable: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val securityPreferences: SecurityPreferences,
    private val biometricAuthManager: BiometricAuthManager
) : ViewModel() {

    private val _securityState = MutableStateFlow(SecuritySettingsState())
    val securityState: StateFlow<SecuritySettingsState> = _securityState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _securityState.value = SecuritySettingsState(
            isAppLockEnabled = securityPreferences.isAppLockEnabled,
            isBiometricEnabled = securityPreferences.isBiometricEnabled,
            hasAppPin = securityPreferences.appPin.isNotBlank(),
            biometricAvailable = biometricAuthManager.getBiometricStatus() == BiometricAuthManager.BiometricStatus.AVAILABLE
        )
    }

    fun setAppLockEnabled(enabled: Boolean) {
        if (enabled && securityPreferences.appPin.isBlank() && !securityPreferences.isBiometricEnabled) {
            _securityState.update { it.copy(errorMessage = "Set a PIN or enable biometric before turning on app lock") }
            return
        }
        securityPreferences.isAppLockEnabled = enabled
        refresh()
        _securityState.update { it.copy(successMessage = if (enabled) "App lock enabled" else "App lock disabled") }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        if (enabled && biometricAuthManager.getBiometricStatus() != BiometricAuthManager.BiometricStatus.AVAILABLE) {
            _securityState.update { it.copy(errorMessage = "Biometric not available on this device") }
            return
        }
        securityPreferences.isBiometricEnabled = enabled
        refresh()
    }

    fun saveAppPin(pin: String, confirmPin: String): Boolean {
        return when {
            pin.length < 4 -> {
                _securityState.update { it.copy(errorMessage = "PIN must be at least 4 digits") }
                false
            }
            pin != confirmPin -> {
                _securityState.update { it.copy(errorMessage = "PINs do not match") }
                false
            }
            else -> {
                securityPreferences.appPin = pin
                _securityState.update { it.copy(errorMessage = null, successMessage = "PIN saved") }
                refresh()
                true
            }
        }
    }

    fun clearMessages() {
        _securityState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
