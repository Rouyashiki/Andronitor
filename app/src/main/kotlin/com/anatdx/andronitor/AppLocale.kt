package com.anatdx.andronitor

import android.content.Context
import android.content.res.Configuration
import com.anatdx.andronitor.data.SettingsStore
import java.util.Locale

object AppLocale {
    fun wrap(context: Context): Context {
        val language = SettingsStore(context).load().language
        val locale = when (language) {
            LANGUAGE_ENGLISH -> Locale.ENGLISH
            LANGUAGE_SIMPLIFIED_CHINESE -> Locale.SIMPLIFIED_CHINESE
            else -> return context
        }
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return context.createConfigurationContext(configuration)
    }

    const val LANGUAGE_SYSTEM = "system"
    const val LANGUAGE_ENGLISH = "en"
    const val LANGUAGE_SIMPLIFIED_CHINESE = "zh-CN"
}
