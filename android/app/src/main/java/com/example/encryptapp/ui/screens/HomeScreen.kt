package com.example.encryptapp.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.encryptapp.ui.components.LogEntry
import com.example.encryptapp.ui.viewmodel.FileOpState
import com.example.encryptapp.ui.viewmodel.FileOpsViewModel
import com.example.encryptapp.ui.viewmodel.HomeViewModel
import com.example.encryptapp.ui.viewmodel.IterationsValidation
import com.example.encryptapp.ui.viewmodel.TextDecryptViewModel
import com.example.encryptapp.ui.viewmodel.TextEncryptViewModel

/**
 * Main screen with responsive layout:
 * - Wide (>=840dp): three-column horizontal layout
 * - Narrow (<840dp): TabRow with single-column layout per tab
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = hiltViewModel(),
    textEncryptViewModel: TextEncryptViewModel = hiltViewModel(),
    textDecryptViewModel: TextDecryptViewModel = hiltViewModel(),
    fileOpsViewModel: FileOpsViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Collect shared state
    val password by homeViewModel.password.collectAsStateWithLifecycle()
    val algorithm by homeViewModel.algorithm.collectAsStateWithLifecycle()
    val iterations by homeViewModel.iterations.collectAsStateWithLifecycle()
    val language by homeViewModel.language.collectAsStateWithLifecycle()
    val settings by homeViewModel.settings.collectAsStateWithLifecycle()

    // Collect per-panel state
    val encTextState by textEncryptViewModel.state.collectAsStateWithLifecycle()
    val decTextState by textDecryptViewModel.state.collectAsStateWithLifecycle()
    val encFileState by fileOpsViewModel.encState.collectAsStateWithLifecycle()
    val decFileState by fileOpsViewModel.decState.collectAsStateWithLifecycle()

    // Local input state
    var encInputText by remember { mutableStateOf("") }
    var decInputText by remember { mutableStateOf("") }

    // File URIs
    var encFileUri by remember { mutableStateOf<Uri?>(null) }
    var encFileName by remember { mutableStateOf("") }
    var decFileUri by remember { mutableStateOf<Uri?>(null) }
    var decFileName by remember { mutableStateOf("") }

    val isZh = language == "zh"

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart
    ) {
        val isWide = maxWidth >= 840.dp

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isZh) "Python 加密工具 (PBKDF2 + AES)" else "Encryption Tool (PBKDF2 + AES)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Shared helper to validate and get iterations
                fun validateAndGetIterations(): Int? {
                    val v = homeViewModel.validateIterations()
                    return if (v is IterationsValidation.Valid) {
                        v.value
                    } else {
                        Toast.makeText(
                            context,
                            if (isZh) "无效的迭代次数！" else "Invalid iterations!",
                            Toast.LENGTH_SHORT
                        ).show()
                        null
                    }
                }

                // Control bar
                ControlPanel(
                    password = password,
                    onPasswordChange = { homeViewModel.setPassword(it) },
                    onConfirmPassword = {
                        if (password.isNotEmpty()) {
                            Toast.makeText(context, if (isZh) "密码已确认" else "Password confirmed", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, if (isZh) "密码为空！" else "Password is empty!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    algorithm = algorithm,
                    onAlgorithmChange = { homeViewModel.setAlgorithm(it) },
                    iterations = iterations,
                    onIterationsChange = { homeViewModel.setIterations(it) },
                    onConfirmIterations = {
                        when (val v = homeViewModel.validateIterations()) {
                            is IterationsValidation.Valid -> {
                                homeViewModel.saveIterations()
                                Toast.makeText(context, if (isZh) "迭代次数已确认。" else "Iterations confirmed.", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                Toast.makeText(context, if (isZh) "无效的迭代次数！" else "Invalid iterations!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    language = language,
                    onLanguageToggle = { homeViewModel.setLanguage(it) },
                    encOutputPath = settings.encOutputPath,
                    onEncDirSelected = { /* SAF URI — path is saved via DataStore */ },
                    decOutputPath = settings.decOutputPath,
                    onDecDirSelected = { /* SAF URI */ },
                    passwordLabel = if (isZh) "密码:" else "Password:",
                    confirmPwdLabel = if (isZh) "确认密码" else "Confirm Pwd",
                    algorithmLabel = if (isZh) "算法:" else "Algorithm:",
                    langLabel = if (isZh) "语言:" else "Lang:",
                    iterationsLabel = if (isZh) "迭代次数:" else "Iterations:",
                    confirmIterLabel = if (isZh) "确认" else "Confirm",
                    encOutLabel = if (isZh) "设置加密输出" else "Set Enc Out",
                    decOutLabel = if (isZh) "设置解密输出" else "Set Dec Out"
                )

                // Main content
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextEncryptPanel(
                            inputText = encInputText,
                            onInputChange = { encInputText = it },
                            isProcessing = encTextState.isProcessing,
                            logEntries = encTextState.logEntries,
                            error = encTextState.error,
                            onEncrypt = {
                                validateAndGetIterations()?.let { iters ->
                                    textEncryptViewModel.encrypt(encInputText, password, algorithm, iters)
                                }
                            },
                            onClearError = { textEncryptViewModel.clearError() },
                            title = if (isZh) "文本加密" else "Text Encryption",
                            historyLabel = if (isZh) "历史/结果:" else "History/Results:",
                            inputLabel = if (isZh) "输入文本:" else "Input Text:",
                            btnLabel = if (isZh) "加密" else "Encrypt",
                            modifier = Modifier.weight(1f)
                        )

                        TextDecryptPanel(
                            inputText = decInputText,
                            onInputChange = { decInputText = it },
                            isProcessing = decTextState.isProcessing,
                            logEntries = decTextState.logEntries,
                            error = decTextState.error,
                            onDecrypt = {
                                validateAndGetIterations()?.let { iters ->
                                    textDecryptViewModel.decrypt(decInputText, password, algorithm, iters)
                                }
                            },
                            onClearError = { textDecryptViewModel.clearError() },
                            title = if (isZh) "文本解密" else "Text Decryption",
                            historyLabel = if (isZh) "历史/结果:" else "History/Results:",
                            inputLabel = if (isZh) "输入文本:" else "Input Text:",
                            btnLabel = if (isZh) "解密" else "Decrypt",
                            modifier = Modifier.weight(1f)
                        )

                        FileOpsPanel(
                            encFileUri = encFileUri,
                            encFileName = encFileName,
                            onEncFilePicked = { uri ->
                                encFileUri = uri
                                encFileName = uri.lastPathSegment ?: ""
                            },
                            encState = encFileState,
                            onEncryptFile = {
                                encFileUri?.let { uri ->
                                    validateAndGetIterations()?.let { iters ->
                                        fileOpsViewModel.encryptFile(uri, password, algorithm, iters) { success, path ->
                                            if (success) {
                                                val msg = if (isZh) "保存至: $path" else "Saved to: $path"
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                }
                            },
                            decFileUri = decFileUri,
                            decFileName = decFileName,
                            onDecFilePicked = { uri ->
                                decFileUri = uri
                                decFileName = uri.lastPathSegment ?: ""
                            },
                            decState = decFileState,
                            onDecryptFile = {
                                decFileUri?.let { uri ->
                                    validateAndGetIterations()?.let { iters ->
                                        fileOpsViewModel.decryptFile(uri, password, algorithm, iters) { success, path ->
                                            if (success) {
                                                val msg = if (isZh) "保存至: $path" else "Saved to: $path"
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                }
                            },
                            title = if (isZh) "文件操作" else "File Operations",
                            encTitle = if (isZh) "文件加密" else "File Encryption",
                            selectFileLabel = if (isZh) "选择文件" else "Select File",
                            dragInfoLabel = if (isZh) "点击选择文件:" else "Tap to select a file:",
                            btnEncFileLabel = if (isZh) "加密文件" else "Encrypt File",
                            decTitle = if (isZh) "文件解密" else "File Decryption",
                            btnDecFileLabel = if (isZh) "解密文件" else "Decrypt File",
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    NarrowLayout(
                        encInputText = encInputText,
                        onEncInputChange = { encInputText = it },
                        isEncProcessing = encTextState.isProcessing,
                        encLogEntries = encTextState.logEntries,
                        encError = encTextState.error,
                        onEncrypt = {
                            validateAndGetIterations()?.let { iters ->
                                textEncryptViewModel.encrypt(encInputText, password, algorithm, iters)
                            }
                        },
                        onClearEncError = { textEncryptViewModel.clearError() },
                        decInputText = decInputText,
                        onDecInputChange = { decInputText = it },
                        isDecProcessing = decTextState.isProcessing,
                        decLogEntries = decTextState.logEntries,
                        decError = decTextState.error,
                        onDecrypt = {
                            validateAndGetIterations()?.let { iters ->
                                textDecryptViewModel.decrypt(decInputText, password, algorithm, iters)
                            }
                        },
                        onClearDecError = { textDecryptViewModel.clearError() },
                        encFileUri = encFileUri,
                        encFileName = encFileName,
                        onEncFilePicked = { uri ->
                            encFileUri = uri
                            encFileName = uri.lastPathSegment ?: ""
                        },
                        encFileState = encFileState,
                        onEncryptFile = {
                            encFileUri?.let { uri ->
                                validateAndGetIterations()?.let { iters ->
                                    fileOpsViewModel.encryptFile(uri, password, algorithm, iters) { _, _ -> }
                                }
                            }
                        },
                        decFileUri = decFileUri,
                        decFileName = decFileName,
                        onDecFilePicked = { uri ->
                            decFileUri = uri
                            decFileName = uri.lastPathSegment ?: ""
                        },
                        decFileState = decFileState,
                        onDecryptFile = {
                            decFileUri?.let { uri ->
                                validateAndGetIterations()?.let { iters ->
                                    fileOpsViewModel.decryptFile(uri, password, algorithm, iters) { _, _ -> }
                                }
                            }
                        },
                        isZh = isZh
                    )
                }
            }
        }
    }
}

/**
 * Tab-based layout for narrow screens (phones).
 */
@Composable
private fun NarrowLayout(
    encInputText: String,
    onEncInputChange: (String) -> Unit,
    isEncProcessing: Boolean,
    encLogEntries: List<LogEntry>,
    encError: String?,
    onEncrypt: () -> Unit,
    onClearEncError: () -> Unit,
    decInputText: String,
    onDecInputChange: (String) -> Unit,
    isDecProcessing: Boolean,
    decLogEntries: List<LogEntry>,
    decError: String?,
    onDecrypt: () -> Unit,
    onClearDecError: () -> Unit,
    encFileUri: Uri?,
    encFileName: String,
    onEncFilePicked: (Uri) -> Unit,
    encFileState: FileOpState,
    onEncryptFile: () -> Unit,
    decFileUri: Uri?,
    decFileName: String,
    onDecFilePicked: (Uri) -> Unit,
    decFileState: FileOpState,
    onDecryptFile: () -> Unit,
    isZh: Boolean
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = if (isZh) listOf("文本加密", "文本解密", "文件操作") else listOf("Encrypt", "Decrypt", "Files")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTab) {
            0 -> TextEncryptPanel(
                inputText = encInputText,
                onInputChange = onEncInputChange,
                isProcessing = isEncProcessing,
                logEntries = encLogEntries,
                error = encError,
                onEncrypt = onEncrypt,
                onClearError = onClearEncError,
                title = if (isZh) "文本加密" else "Text Encryption",
                historyLabel = if (isZh) "历史/结果:" else "History/Results:",
                inputLabel = if (isZh) "输入文本:" else "Input Text:",
                btnLabel = if (isZh) "加密" else "Encrypt",
                modifier = Modifier.fillMaxSize()
            )
            1 -> TextDecryptPanel(
                inputText = decInputText,
                onInputChange = onDecInputChange,
                isProcessing = isDecProcessing,
                logEntries = decLogEntries,
                error = decError,
                onDecrypt = onDecrypt,
                onClearError = onClearDecError,
                title = if (isZh) "文本解密" else "Text Decryption",
                historyLabel = if (isZh) "历史/结果:" else "History/Results:",
                inputLabel = if (isZh) "输入文本:" else "Input Text:",
                btnLabel = if (isZh) "解密" else "Decrypt",
                modifier = Modifier.fillMaxSize()
            )
            2 -> FileOpsPanel(
                encFileUri = encFileUri,
                encFileName = encFileName,
                onEncFilePicked = onEncFilePicked,
                encState = encFileState,
                onEncryptFile = onEncryptFile,
                decFileUri = decFileUri,
                decFileName = decFileName,
                onDecFilePicked = onDecFilePicked,
                decState = decFileState,
                onDecryptFile = onDecryptFile,
                title = if (isZh) "文件操作" else "File Operations",
                encTitle = if (isZh) "文件加密" else "File Encryption",
                selectFileLabel = if (isZh) "选择文件" else "Select File",
                dragInfoLabel = if (isZh) "点击选择文件:" else "Tap to select a file:",
                btnEncFileLabel = if (isZh) "加密文件" else "Encrypt File",
                decTitle = if (isZh) "文件解密" else "File Decryption",
                btnDecFileLabel = if (isZh) "解密文件" else "Decrypt File",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
