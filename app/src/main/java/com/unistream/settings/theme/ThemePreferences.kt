package com.unistream.settings.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val seenSplashKey = stringPreferencesKey("has_seen_splash")

    val themeMode: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        ThemeMode.entries.firstOrNull { it.name == prefs[themeKey] } ?: ThemeMode.SYSTEM
    }

    val hasSeenSplash: Flow<Boolean> = context.themeDataStore.data.map { prefs ->
        prefs[seenSplashKey] == "true"
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { it[themeKey] = mode.name }
    }

    suspend fun setHasSeenSplash(value: Boolean) {
        context.themeDataStore.edit { it[seenSplashKey] = value.toString() }
    }
}
