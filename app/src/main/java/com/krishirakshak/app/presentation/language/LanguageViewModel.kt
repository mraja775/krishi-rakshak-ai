package com.krishirakshak.app.presentation.language

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krishirakshak.app.util.LanguageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val languageManager: LanguageManager
) : ViewModel() {

    private val _selectedLanguage = MutableStateFlow(LanguageManager.Language.ENGLISH)
    val selectedLanguage: StateFlow<LanguageManager.Language> = _selectedLanguage.asStateFlow()

    init {
        loadSelectedLanguage()
    }

    private fun loadSelectedLanguage() {
        viewModelScope.launch {
            languageManager.selectedLanguage.collect { language ->
                _selectedLanguage.value = language
                Timber.d("Current language: ${language.displayName}")
            }
        }
    }

    fun selectLanguage(language: LanguageManager.Language) {
        _selectedLanguage.value = language
        Timber.d("Language selected: ${language.displayName}")
    }

    fun saveLanguageAndProceed() {
        viewModelScope.launch {
            try {
                languageManager.setLanguage(_selectedLanguage.value)
                Timber.d("Language saved: ${_selectedLanguage.value.displayName}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to save language")
            }
        }
    }
}
