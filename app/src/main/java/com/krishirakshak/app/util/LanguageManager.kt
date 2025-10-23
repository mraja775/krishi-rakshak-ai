package com.krishirakshak.app.util

import android.content.Context
import android.content.res.Configuration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class LanguageManager @Inject constructor(
    private val context: Context
) {
    private val LANGUAGE_KEY = stringPreferencesKey("selected_language")

    /**
     * Available languages in the app
     */
    enum class Language(val code: String, val displayName: String) {
        ENGLISH("en", "English"),
        HINDI("hi", "हिंदी"),
        TAMIL("ta", "தமிழ்"),
        KANNADA("kn", "ಕನ್ನಡ");

        companion object {
            fun fromCode(code: String): Language {
                return values().find { it.code == code } ?: ENGLISH
            }
        }
    }

    /**
     * Get the current selected language
     */
    val selectedLanguage: Flow<Language> = context.dataStore.data.map { preferences ->
        val code = preferences[LANGUAGE_KEY] ?: Language.ENGLISH.code
        Language.fromCode(code)
    }

    /**
     * Set the selected language
     */
    suspend fun setLanguage(language: Language) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language.code
        }
    }

    /**
     * Apply language to context
     */
    fun applyLanguage(context: Context, language: Language): Context {
        val locale = Locale(language.code)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }

    /**
     * Get current language code
     */
    suspend fun getCurrentLanguageCode(): String {
        var code = Language.ENGLISH.code
        context.dataStore.data.collect { preferences ->
            code = preferences[LANGUAGE_KEY] ?: Language.ENGLISH.code
        }
        return code
    }
}
