package com.example.encryptapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.encryptapp.core.crypto.CryptoManager
import com.example.encryptapp.core.domain.model.Algorithm
import com.example.encryptapp.ui.components.LogEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TextEncryptState(
    val isProcessing: Boolean = false,
    val result: String? = null,
    val error: String? = null,
    val logEntries: List<LogEntry> = emptyList()
)

@HiltViewModel
class TextEncryptViewModel @Inject constructor(
    private val cryptoManager: CryptoManager
) : ViewModel() {

    private val _state = MutableStateFlow(TextEncryptState())
    val state: StateFlow<TextEncryptState> = _state.asStateFlow()

    fun encrypt(text: String, password: String, algorithm: Algorithm, iterations: Int) {
        if (text.isBlank()) return
        if (password.isBlank()) {
            _state.value = _state.value.copy(error = "Password is required")
            return
        }

        cryptoManager.iterations = iterations

        _state.value = _state.value.copy(isProcessing = true, error = null, result = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val encrypted = cryptoManager.encryptText(text.trimEnd(), password, algorithm)
                val entry = LogEntry("Encrypted ($algorithm):\n$encrypted")
                _state.value = _state.value.copy(
                    isProcessing = false,
                    result = encrypted,
                    logEntries = _state.value.logEntries + entry
                )
            } catch (e: Exception) {
                val entry = LogEntry("Error: ${e.message}")
                _state.value = _state.value.copy(
                    isProcessing = false,
                    error = e.message ?: "Encryption failed",
                    logEntries = _state.value.logEntries + entry
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
