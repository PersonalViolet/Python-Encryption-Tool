package com.example.encryptapp.viewmodel

import app.cash.turbine.test
import com.example.encryptapp.core.crypto.CryptoManager
import com.example.encryptapp.core.data.SettingsRepository
import com.example.encryptapp.core.domain.model.Algorithm
import com.example.encryptapp.core.domain.model.Settings
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var viewModel: HomeViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        settingsRepository = mockk {
            every { settings } returns flowOf(Settings(iterations = 10000, language = "en"))
            coEvery { updateIterations(any()) } just Runs
            coEvery { updateLanguage(any()) } just Runs
        }
        Dispatchers.setMain(testDispatcher)
        viewModel = HomeViewModel(settingsRepository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty password`() = runTest {
        assertEquals("", viewModel.password.value)
    }

    @Test
    fun `initial algorithm is AES-256`() = runTest {
        assertEquals(Algorithm.AES_256, viewModel.algorithm.value)
    }

    @Test
    fun `setting password updates state`() = runTest {
        viewModel.setPassword("mypassword")
        assertEquals("mypassword", viewModel.password.value)
    }

    @Test
    fun `isPasswordValid returns true for non-empty password`() = runTest {
        viewModel.setPassword("test")
        assertTrue(viewModel.isPasswordValid())
    }

    @Test
    fun `isPasswordValid returns false for empty password`() = runTest {
        viewModel.setPassword("")
        assertFalse(viewModel.isPasswordValid())
    }

    @Test
    fun `setting algorithm updates state`() = runTest {
        viewModel.setAlgorithm(Algorithm.AES_128)
        assertEquals(Algorithm.AES_128, viewModel.algorithm.value)
    }

    @Test
    fun `valid iterations parse correctly`() = runTest {
        viewModel.setIterations("50000")
        val result = viewModel.validateIterations()
        assertTrue(result is IterationsValidation.Valid)
        assertEquals(50000, (result as IterationsValidation.Valid).value)
    }

    @Test
    fun `empty iterations is invalid`() = runTest {
        viewModel.setIterations("")
        assertTrue(viewModel.validateIterations() is IterationsValidation.Empty)
    }

    @Test
    fun `non-numeric iterations is invalid`() = runTest {
        viewModel.setIterations("abc")
        assertTrue(viewModel.validateIterations() is IterationsValidation.InvalidFormat)
    }

    @Test
    fun `negative iterations is invalid`() = runTest {
        viewModel.setIterations("-5")
        assertTrue(viewModel.validateIterations() is IterationsValidation.InvalidFormat)
    }

    @Test
    fun `zero iterations is invalid`() = runTest {
        viewModel.setIterations("0")
        assertTrue(viewModel.validateIterations() is IterationsValidation.ZeroOrNegative)
    }

    @Test
    fun `iterations over max is invalid`() = runTest {
        viewModel.setIterations("200000000")
        assertTrue(viewModel.validateIterations() is IterationsValidation.ExceedsMax)
    }

    @Test
    fun `getValidIterations returns parsed value`() = runTest {
        viewModel.setIterations("12345")
        assertEquals(12345, viewModel.getValidIterations())
    }

    @Test
    fun `getValidIterations returns null for invalid`() = runTest {
        viewModel.setIterations("abc")
        assertNull(viewModel.getValidIterations())
    }

    @Test
    fun `setLanguage updates state and persists`() = runTest {
        viewModel.setLanguage("zh")
        assertEquals("zh", viewModel.language.value)
        coVerify { settingsRepository.updateLanguage("zh") }
    }

    @Test
    fun `saveIterations persists valid iterations`() = runTest {
        viewModel.setIterations("50000")
        viewModel.saveIterations()
        advanceUntilIdle()
        coVerify { settingsRepository.updateIterations(50000) }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TextEncryptViewModelTest {

    private lateinit var cryptoManager: CryptoManager
    private lateinit var viewModel: TextEncryptViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        cryptoManager = mockk()
        Dispatchers.setMain(testDispatcher)
        viewModel = TextEncryptViewModel(cryptoManager)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `encrypt success updates state with result`() = runTest {
        every { cryptoManager.iterations = any() } just Runs
        every { cryptoManager.encryptText("hello", "pwd", Algorithm.AES_256) } returns "ENCRYPTED_BASE64"

        viewModel.encrypt("hello", "pwd", Algorithm.AES_256, 10000)
        advanceUntilIdle()

        assertEquals("ENCRYPTED_BASE64", viewModel.state.value.result)
        assertFalse(viewModel.state.value.isProcessing)
        assertEquals(1, viewModel.state.value.logEntries.size)
    }

    @Test
    fun `encrypt with blank text does nothing`() = runTest {
        viewModel.encrypt("  ", "pwd", Algorithm.AES_256, 10000)
        advanceUntilIdle()

        assertNull(viewModel.state.value.result)
        assertFalse(viewModel.state.value.isProcessing)
    }

    @Test
    fun `encrypt error updates state with error`() = runTest {
        every { cryptoManager.iterations = any() } just Runs
        every { cryptoManager.encryptText(any(), any(), any()) } throws RuntimeException("Crypto error")

        viewModel.encrypt("text", "pwd", Algorithm.AES_256, 10000)
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.error)
        assertTrue(viewModel.state.value.error!!.contains("Crypto error"))
    }

    @Test
    fun `clearError removes error from state`() = runTest {
        every { cryptoManager.iterations = any() } just Runs
        every { cryptoManager.encryptText(any(), any(), any()) } throws RuntimeException("err")

        viewModel.encrypt("text", "pwd", Algorithm.AES_256, 10000)
        advanceUntilIdle()
        assertNotNull(viewModel.state.value.error)

        viewModel.clearError()
        assertNull(viewModel.state.value.error)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TextDecryptViewModelTest {

    private lateinit var cryptoManager: CryptoManager
    private lateinit var viewModel: TextDecryptViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        cryptoManager = mockk()
        Dispatchers.setMain(testDispatcher)
        viewModel = TextDecryptViewModel(cryptoManager)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `decrypt success updates state`() = runTest {
        every { cryptoManager.iterations = any() } just Runs
        every { cryptoManager.decryptText("CIPHERTEXT", "pwd", Algorithm.AES_256) } returns "plaintext"

        viewModel.decrypt("CIPHERTEXT", "pwd", Algorithm.AES_256, 10000)
        advanceUntilIdle()

        assertEquals("plaintext", viewModel.state.value.result)
        assertFalse(viewModel.state.value.isProcessing)
    }

    @Test
    fun `decrypt with blank input does nothing`() = runTest {
        viewModel.decrypt("", "pwd", Algorithm.AES_256, 10000)
        advanceUntilIdle()

        assertNull(viewModel.state.value.result)
    }

    @Test
    fun `decrypt error updates state with error`() = runTest {
        every { cryptoManager.iterations = any() } just Runs
        every { cryptoManager.decryptText(any(), any(), any()) } throws IllegalArgumentException("Bad format")

        viewModel.decrypt("bad-data", "pwd", Algorithm.AES_256, 10000)
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.error)
    }
}
