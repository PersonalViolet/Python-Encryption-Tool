package com.example.encryptapp.ui.screens

import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.encryptapp.core.crypto.CryptoConstants
import com.example.encryptapp.core.domain.model.Algorithm
import com.example.encryptapp.ui.components.AlgorithmDropdown
import com.example.encryptapp.ui.components.IterationsField
import com.example.encryptapp.ui.components.OutputDirSelector
import com.example.encryptapp.ui.components.PasswordField

/**
 * Top control bar: password, algorithm, iterations, language, output dirs.
 * Mirrors the top_frame in the Python Tkinter app.
 */
@Composable
fun ControlPanel(
    password: String,
    onPasswordChange: (String) -> Unit,
    onConfirmPassword: () -> Unit,
    algorithm: Algorithm,
    onAlgorithmChange: (Algorithm) -> Unit,
    iterations: String,
    onIterationsChange: (String) -> Unit,
    onConfirmIterations: () -> Unit,
    language: String,
    onLanguageToggle: (String) -> Unit,
    encOutputPath: String,
    onEncDirSelected: (Uri) -> Unit,
    decOutputPath: String,
    onDecDirSelected: (Uri) -> Unit,
    passwordLabel: String,
    confirmPwdLabel: String,
    algorithmLabel: String,
    langLabel: String,
    iterationsLabel: String,
    confirmIterLabel: String,
    encOutLabel: String,
    decOutLabel: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Password
            PasswordField(
                password = password,
                onPasswordChange = onPasswordChange,
                label = passwordLabel,
                modifier = Modifier.width(180.dp)
            )

            IconButton(
                onClick = onConfirmPassword,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = confirmPwdLabel, modifier = Modifier.size(18.dp))
            }

            // Algorithm
            AlgorithmDropdown(
                selected = algorithm,
                onAlgorithmSelected = onAlgorithmChange,
                label = algorithmLabel,
                modifier = Modifier.width(140.dp)
            )

            // Iterations
            IterationsField(
                iterations = iterations,
                onIterationsChange = onIterationsChange,
                label = iterationsLabel,
                modifier = Modifier.width(160.dp)
            )

            IconButton(
                onClick = onConfirmIterations,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = confirmIterLabel, modifier = Modifier.size(18.dp))
            }

            // Language toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(langLabel, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.width(4.dp))
                SegmentedButton(
                    selected = language,
                    options = listOf("EN" to "en", "ZH" to "zh"),
                    onSelect = onLanguageToggle
                )
            }

            // Output dirs
            OutputDirSelector(
                currentPath = encOutputPath,
                onDirectorySelected = onEncDirSelected,
                buttonLabel = encOutLabel
            )

            OutputDirSelector(
                currentPath = decOutputPath,
                onDirectorySelected = onDecDirSelected,
                buttonLabel = decOutLabel
            )
        }
    }
}

@Composable
private fun SegmentedButton(
    selected: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    Row {
        options.forEach { (label, value) ->
            val isSelected = selected == value
            FilledTonalButton(
                onClick = { onSelect(value) },
                colors = if (isSelected) {
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    ButtonDefaults.filledTonalButtonColors()
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
