package com.example.encryptapp.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.encryptapp.core.crypto.CryptoManager
import com.example.encryptapp.core.domain.model.Algorithm
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FileOpState(
    val status: String = "Ready",
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null,
    val elapsedMs: Long = 0L
)

@HiltViewModel
class FileOpsViewModel @Inject constructor(
    private val cryptoManager: CryptoManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _encState = MutableStateFlow(FileOpState())
    val encState: StateFlow<FileOpState> = _encState.asStateFlow()

    private val _decState = MutableStateFlow(FileOpState())
    val decState: StateFlow<FileOpState> = _decState.asStateFlow()

    fun encryptFile(
        inputUri: Uri,
        password: String,
        algorithm: Algorithm,
        iterations: Int,
        onComplete: (Boolean, String) -> Unit
    ) {
        if (password.isBlank()) {
            _encState.value = _encState.value.copy(error = "Password is required")
            onComplete(false, "")
            return
        }

        cryptoManager.iterations = iterations

        val context = this.context
        _encState.value = FileOpState(status = "Processing…", isProcessing = true, progress = 0f)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(inputUri)
                    ?: throw IllegalStateException("Cannot open input file")

                val fileSize = inputStream.available().toLong().coerceAtLeast(1)

                // Determine output path: same dir as input, with .enc extension
                val fileName = inputUri.lastPathSegment ?: "encrypted"
                val outputUri = Uri.parse("${inputUri}.enc")

                val outputStream = context.contentResolver.openOutputStream(outputUri, "wt")
                    ?: throw IllegalStateException("Cannot create output file")

                val startTime = System.currentTimeMillis()

                cryptoManager.encryptFile(
                    inputStream = inputStream,
                    outputStream = outputStream,
                    fileSize = fileSize,
                    password = password,
                    algorithm = algorithm,
                    onProgress = { processed, total ->
                        val pct = if (total > 0) processed.toFloat() / total else 0f
                        _encState.value = _encState.value.copy(progress = pct)
                    }
                )

                val elapsed = System.currentTimeMillis() - startTime
                _encState.value = FileOpState(
                    status = "Done!",
                    progress = 1f,
                    elapsedMs = elapsed
                )
                onComplete(true, outputUri.toString())

            } catch (e: Exception) {
                _encState.value = FileOpState(
                    status = "Error",
                    error = e.message ?: "Encryption failed"
                )
                onComplete(false, "")
            }
        }
    }

    fun decryptFile(
        inputUri: Uri,
        password: String,
        algorithm: Algorithm,
        iterations: Int,
        onComplete: (Boolean, String) -> Unit
    ) {
        if (password.isBlank()) {
            _decState.value = _decState.value.copy(error = "Password is required")
            onComplete(false, "")
            return
        }

        cryptoManager.iterations = iterations

        val context = this.context
        _decState.value = FileOpState(status = "Processing…", isProcessing = true, progress = 0f)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(inputUri)
                    ?: throw IllegalStateException("Cannot open input file")

                val fileSize = inputStream.available().toLong().coerceAtLeast(1)
                if (fileSize < 32) {
                    throw IllegalArgumentException("File too small to be a valid encrypted file")
                }

                val fileName = inputUri.lastPathSegment ?: "decrypted"
                val outputUri = Uri.parse("${inputUri}.dec")

                val outputStream = context.contentResolver.openOutputStream(outputUri, "wt")
                    ?: throw IllegalStateException("Cannot create output file")

                val startTime = System.currentTimeMillis()

                cryptoManager.decryptFile(
                    inputStream = inputStream,
                    outputStream = outputStream,
                    fileSize = fileSize,
                    password = password,
                    algorithm = algorithm,
                    onProgress = { processed, total ->
                        val pct = if (total > 0) processed.toFloat() / total else 0f
                        _decState.value = _decState.value.copy(progress = pct)
                    }
                )

                val elapsed = System.currentTimeMillis() - startTime
                _decState.value = FileOpState(
                    status = "Done!",
                    progress = 1f,
                    elapsedMs = elapsed
                )
                onComplete(true, outputUri.toString())

            } catch (e: Exception) {
                _decState.value = FileOpState(
                    status = "Error",
                    error = e.message ?: "Decryption failed"
                )
                onComplete(false, "")
            }
        }
    }

    fun clearEncError() { _encState.value = _encState.value.copy(error = null) }
    fun clearDecError() { _decState.value = _decState.value.copy(error = null) }
}
