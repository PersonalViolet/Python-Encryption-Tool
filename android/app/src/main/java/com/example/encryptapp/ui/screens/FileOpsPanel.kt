package com.example.encryptapp.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.encryptapp.ui.components.FilePickerCard
import com.example.encryptapp.ui.viewmodel.FileOpState

/**
 * Right panel: file encryption and decryption operations.
 * Contains two sub-sections: File Encryption (top) and File Decryption (bottom).
 * Shows output path with copy button on success.
 */
@Composable
fun FileOpsPanel(
    // Encrypt
    encFileUri: Uri?,
    encFileName: String,
    onEncFilePicked: (Uri) -> Unit,
    encState: FileOpState,
    onEncryptFile: () -> Unit,
    // Decrypt
    decFileUri: Uri?,
    decFileName: String,
    onDecFilePicked: (Uri) -> Unit,
    decState: FileOpState,
    onDecryptFile: () -> Unit,
    // Labels
    title: String,
    encTitle: String,
    selectFileLabel: String,
    dragInfoLabel: String,
    btnEncFileLabel: String,
    decTitle: String,
    btnDecFileLabel: String,
    modifier: Modifier = Modifier,
    // Copy labels
    copyLabel: String = "Copy",
    copySuccessLabel: String = "Copied to clipboard",
    outputPathLabel: String = "Output:"
) {
    val context = LocalContext.current

    Card(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // ── File Encryption ──
            FileOpSubPanel(
                title = encTitle,
                fileUri = encFileUri,
                fileName = encFileName,
                onFilePicked = onEncFilePicked,
                state = encState,
                onAction = onEncryptFile,
                selectFileLabel = selectFileLabel,
                dragInfoLabel = dragInfoLabel,
                btnLabel = btnEncFileLabel,
                copyLabel = copyLabel,
                copySuccessLabel = copySuccessLabel,
                outputPathLabel = outputPathLabel,
                context = context
            )

            // ── File Decryption ──
            FileOpSubPanel(
                title = decTitle,
                fileUri = decFileUri,
                fileName = decFileName,
                onFilePicked = onDecFilePicked,
                state = decState,
                onAction = onDecryptFile,
                selectFileLabel = selectFileLabel,
                dragInfoLabel = dragInfoLabel,
                btnLabel = btnDecFileLabel,
                copyLabel = copyLabel,
                copySuccessLabel = copySuccessLabel,
                outputPathLabel = outputPathLabel,
                context = context
            )
        }
    }
}

@Composable
private fun FileOpSubPanel(
    title: String,
    fileUri: Uri?,
    fileName: String,
    onFilePicked: (Uri) -> Unit,
    state: FileOpState,
    onAction: () -> Unit,
    selectFileLabel: String,
    dragInfoLabel: String,
    btnLabel: String,
    copyLabel: String,
    copySuccessLabel: String,
    outputPathLabel: String,
    context: Context
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            FilePickerCard(
                fileUri = fileUri,
                fileName = fileName,
                onFilePicked = onFilePicked,
                selectLabel = selectFileLabel,
                infoLabel = dragInfoLabel
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onAction,
                enabled = !state.isProcessing && fileName.isNotEmpty(),
                modifier = Modifier.align(Alignment.End)
            ) {
                if (state.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(btnLabel)
            }

            // Progress
            if (state.progress > 0f) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Status
            if (state.status != "Ready") {
                Text(
                    text = state.status,
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        state.error != null -> MaterialTheme.colorScheme.error
                        state.progress >= 1f -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // Error
            if (state.error != null) {
                Text(
                    text = state.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Output path with copy button (shown on success)
            if (state.outputPath != null && state.progress >= 1f) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$outputPathLabel ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = state.outputPath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("output_path", state.outputPath))
                            Toast.makeText(context, copySuccessLabel, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = copyLabel,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
