package com.example.encryptapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.encryptapp.ui.components.LogEntry
import com.example.encryptapp.ui.components.ResultLog

/**
 * Left panel: text encryption.
 * Mirrors create_text_section(..., "text_enc_title", "btn_encrypt", ...) in Python app.
 */
@Composable
fun TextEncryptPanel(
    inputText: String,
    onInputChange: (String) -> Unit,
    isProcessing: Boolean,
    logEntries: List<LogEntry>,
    error: String?,
    onEncrypt: () -> Unit,
    onClearError: () -> Unit,
    title: String,
    historyLabel: String,
    inputLabel: String,
    btnLabel: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Result log
            ResultLog(
                entries = logEntries,
                label = historyLabel,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Error display
            if (error != null) {
                Snackbar(
                    modifier = Modifier.padding(bottom = 4.dp),
                    action = {
                        TextButton(onClick = onClearError) { Text("Dismiss") }
                    }
                ) {
                    Text(error)
                }
            }

            // Input area
            Text(
                text = inputLabel,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 200.dp),
                maxLines = 8,
                textStyle = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Encrypt button
            Button(
                onClick = onEncrypt,
                enabled = !isProcessing && inputText.isNotBlank(),
                modifier = Modifier.align(Alignment.End)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(btnLabel)
            }
        }
    }
}
