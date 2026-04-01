package com.unistream.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unistream.settings.theme.ThemePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val themePreferences: ThemePreferences
) : ViewModel() {
    val hasSeenSplash: StateFlow<Boolean> = themePreferences.hasSeenSplash
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun markSeen() {
        viewModelScope.launch { themePreferences.setHasSeenSplash(true) }
    }
}
