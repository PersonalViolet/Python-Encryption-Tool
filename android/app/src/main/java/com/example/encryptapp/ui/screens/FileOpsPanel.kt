package com.example.encryptapp.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.encryptapp.ui.components.FilePickerCard
import com.example.encryptapp.ui.viewmodel.FileOpState

/**
 * Right panel: file encryption and decryption operations.
 * Contains two sub-sections: File Encryption (top) and File Decryption (bottom).
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
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // ── File Encryption ──
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = encTitle,
                        style = MaterialTheme.typography.titleSmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FilePickerCard(
                        fileUri = encFileUri,
                        fileName = encFileName,
                        onFilePicked = onEncFilePicked,
                        selectLabel = selectFileLabel,
                        infoLabel = dragInfoLabel
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onEncryptFile,
                        enabled = !encState.isProcessing && encFileName.isNotEmpty(),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        if (encState.isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(btnEncFileLabel)
                    }

                    // Progress
                    if (encState.progress > 0f) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { encState.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Status
                    if (encState.status != "Ready") {
                        Text(
                            text = encState.status,
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                encState.error != null -> MaterialTheme.colorScheme.error
                                encState.progress >= 1f -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            // ── File Decryption ──
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = decTitle,
                        style = MaterialTheme.typography.titleSmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FilePickerCard(
                        fileUri = decFileUri,
                        fileName = decFileName,
                        onFilePicked = onDecFilePicked,
                        selectLabel = selectFileLabel,
                        infoLabel = dragInfoLabel
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onDecryptFile,
                        enabled = !decState.isProcessing && decFileName.isNotEmpty(),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        if (decState.isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(btnDecFileLabel)
                    }

                    // Progress
                    if (decState.progress > 0f) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { decState.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Status
                    if (decState.status != "Ready") {
                        Text(
                            text = decState.status,
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                decState.error != null -> MaterialTheme.colorScheme.error
                                decState.progress >= 1f -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}
