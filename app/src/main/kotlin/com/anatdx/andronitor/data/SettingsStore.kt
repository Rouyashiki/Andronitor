package com.anatdx.andronitor.data

import android.content.Context
import com.anatdx.andronitor.model.AppSettings

class SettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("andronitor_settings", Context.MODE_PRIVATE)

    fun load(): AppSettings {
        val raw = preferences.getString(KEY, null) ?: return AppSettings()
        return AppSettings.fromJsonString(raw)
    }

    fun save(settings: AppSettings) {
        preferences.edit().putString(KEY, settings.toJsonString()).apply()
    }

    companion object {
        private const val KEY = "settings_json"
    }
}
