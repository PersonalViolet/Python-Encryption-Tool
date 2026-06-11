package com.example.encryptapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.encryptapp.core.crypto.CryptoConstants
import com.example.encryptapp.core.data.SettingsRepository
import com.example.encryptapp.core.domain.model.Algorithm
import com.example.encryptapp.core.domain.model.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Shared ViewModel for global state: password, algorithm, iterations, language.
 * All panels read from this ViewModel.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // ── Password ──
    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    fun setPassword(pwd: String) { _password.value = pwd }

    fun isPasswordValid(): Boolean = _password.value.isNotEmpty()

    // ── Algorithm ──
    private val _algorithm = MutableStateFlow(Algorithm.AES_256)
    val algorithm: StateFlow<Algorithm> = _algorithm.asStateFlow()

    fun setAlgorithm(algo: Algorithm) { _algorithm.value = algo }

    // ── Iterations ──
    private val _iterations = MutableStateFlow("10000")
    val iterations: StateFlow<String> = _iterations.asStateFlow()

    fun setIterations(value: String) { _iterations.value = value }

    fun getValidIterations(): Int? {
        val value = _iterations.value.toLongOrNull() ?: return null
        if (value <= 0 || value > CryptoConstants.MAX_ITERATIONS) return null
        return value.toInt()
    }

    fun validateIterations(): IterationsValidation {
        val value = _iterations.value
        if (value.isBlank()) return IterationsValidation.Empty
        val num = value.toLongOrNull()
            ?: return IterationsValidation.InvalidFormat
        return when {
            num <= 0 -> IterationsValidation.ZeroOrNegative
            num > CryptoConstants.MAX_ITERATIONS -> IterationsValidation.ExceedsMax
            else -> IterationsValidation.Valid(num.toInt())
        }
    }

    // ── Language ──
    private val _language = MutableStateFlow("en")
    val language: StateFlow<String> = _language.asStateFlow()

    fun setLanguage(lang: String) {
        _language.value = lang
        viewModelScope.launch { settingsRepository.updateLanguage(lang) }
    }

    // ── Settings ──
    val settings: StateFlow<Settings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Settings())

    fun loadIterationsFromSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collect { s ->
                _iterations.value = s.iterations.toString()
                _language.value = s.language
            }
        }
    }

    fun saveIterations() {
        viewModelScope.launch {
            getValidIterations()?.let { settingsRepository.updateIterations(it) }
        }
    }

    init {
        loadIterationsFromSettings()
    }
}

sealed class IterationsValidation {
    data class Valid(val value: Int) : IterationsValidation()
    data object Empty : IterationsValidation()
    data object InvalidFormat : IterationsValidation()
    data object ZeroOrNegative : IterationsValidation()
    data object ExceedsMax : IterationsValidation()
}
