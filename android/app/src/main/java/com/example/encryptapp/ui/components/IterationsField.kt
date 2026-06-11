package com.example.encryptapp.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.example.encryptapp.core.crypto.CryptoConstants

/**
 * Input field for PBKDF2 iteration count with validation.
 * Enforces 0 < value <= MAX_ITERATIONS (100,000,000).
 */
@Composable
fun IterationsField(
    iterations: String,
    onIterationsChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    onConfirm: () -> Unit = {}
) {
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun validate(value: String): String? {
        if (value.isBlank()) return null  // Allow empty during typing
        val num = value.toLongOrNull()
        return when {
            num == null -> "Invalid number"
            num <= 0 -> "Must be > 0"
            num > CryptoConstants.MAX_ITERATIONS -> "Max: ${CryptoConstants.MAX_ITERATIONS}"
            else -> null
        }
    }

    OutlinedTextField(
        value = iterations,
        onValueChange = { newValue ->
            // Only allow digits
            val filtered = newValue.filter { it.isDigit() }
            onIterationsChange(filtered)
            errorMessage = validate(filtered)
        },
        label = { Text(label) },
        singleLine = true,
        isError = errorMessage != null,
        supportingText = errorMessage?.let { err -> { Text(err, color = MaterialTheme.colorScheme.error) } },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { onConfirm() }
        ),
        modifier = modifier.fillMaxWidth()
    )
}
