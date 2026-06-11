package com.example.encryptapp.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.encryptapp.core.domain.model.Algorithm

/**
 * Dropdown selector for AES algorithm (AES-128, AES-192, AES-256).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlgorithmDropdown(
    selected: Algorithm,
    onAlgorithmSelected: (Algorithm) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Algorithm.entries.forEach { algo ->
                DropdownMenuItem(
                    text = { Text(algo.displayName) },
                    onClick = {
                        onAlgorithmSelected(algo)
                        expanded = false
                    }
                )
            }
        }
    }
}
