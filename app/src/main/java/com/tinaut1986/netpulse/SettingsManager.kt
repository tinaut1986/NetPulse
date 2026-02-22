package com.tinaut1986.netpulse

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import java.util.Locale

class SettingsManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _theme = mutableStateOf(prefs.getString("theme", "system") ?: "system")
    val theme: State<String> = _theme

    private val _language = mutableStateOf(prefs.getString("language", Locale.getDefault().language) ?: "en")
    val language: State<String> = _language

    fun setTheme(theme: String) {
        _theme.value = theme
        prefs.edit().putString("theme", theme).apply()
    }

    fun setLanguage(language: String) {
        _language.value = language
        prefs.edit().putString("language", language).apply()
    }
}
