package com.example.encryptapp.viewmodel

import com.example.encryptapp.core.crypto.CryptoManager
import com.example.encryptapp.core.data.SettingsRepository
import com.example.encryptapp.core.domain.model.Algorithm
import com.example.encryptapp.core.domain.model.Settings
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HomeViewModelTest {

    @Test
    fun `initial state has empty password`() = runTest {
        val repo = mockk<SettingsRepository> {
            every { settings } returns flowOf(Settings(iterations = 10000, language = "en"))
        }
        val viewModel = HomeViewModel(repo)
        assertEquals("", viewModel.password.value)
    }

    @Test
    fun `initial algorithm is AES-256`() = runTest {
        val repo = mockk<SettingsRepository> {
            every { settings } returns flowOf(Settings())
        }
        val viewModel = HomeViewModel(repo)
        assertEquals(Algorithm.AES_256, viewModel.algorithm.value)
    }

    @Test
    fun `setting password updates state`() = runTest {
        val repo = mockk<SettingsRepository> {
            every { settings } returns flowOf(Settings())
        }
        val viewModel = HomeViewModel(repo)
        viewModel.setPassword("mypassword")
        assertEquals("mypassword", viewModel.password.value)
    }

    @Test
    fun `isPasswordValid returns true for non-empty password`() = runTest {
        val repo = mockk<SettingsRepository> {
            every { settings } returns flowOf(Settings())
        }
        val viewModel = HomeViewModel(repo)
        viewModel.setPassword("test")
        assertTrue(viewModel.isPasswordValid())
    }

    @Test
    fun `isPasswordValid returns false for empty password`() = runTest {
        val repo = mockk<SettingsRepository> {
            every { settings } returns flowOf(Settings())
        }
        val viewModel = HomeViewModel(repo)
        assertFalse(viewModel.isPasswordValid())
    }

    @Test
    fun `setting algorithm updates state`() = runTest {
        val repo = mockk<SettingsRepository> {
            every { settings } returns flowOf(Settings())
        }
        val viewModel = HomeViewModel(repo)
        viewModel.setAlgorithm(Algorithm.AES_128)
        assertEquals(Algorithm.AES_128, viewModel.algorithm.value)
    }

    @Test
    fun `valid iterations parse correctly`() = runTest {
        val repo = mockk<SettingsRepository> {
            every { settings } returns flowOf(Settings())
        }
        val viewModel = HomeViewModel(repo)
        viewModel.setIterations("50000")
        val result = viewModel.validateIterations()
        assertTrue(result is IterationsValidation.Valid)
        assertEquals(50000, (result as IterationsValidation.Valid).value)
    }

    @Test
    fun `empty iterations is invalid`() = runTest {
        val repo = mockk<SettingsRepository> {
            every { settings } returns flowOf(Settings())
        }
        val viewModel = HomeViewModel(repo)
        viewModel.setIterations("")
        assertTrue(viewModel.validateIterations() is IterationsValidation.Empty)
    }

    @Test
    fun `non-numeric iterations is invalid`() = runTest {
        val repo = mockk<SettingsRepository> {
            every { settings } returns flowOf(Settings())
        }
        val viewModel = HomeViewModel(repo)
        viewModel.setIterations("abc")
        assertTrue(viewModel.validateIterations() is IterationsValidation.InvalidFormat)
    }

    @Test
    fun `zero iterations is invalid`() = runTest {
        val repo = mockk<SettingsRepository> {
            every { settings } returns flowOf(Settings())
        }
        val viewModel = HomeViewModel(repo)
        viewModel.setIterations("0")
        assertTrue(viewModel.validateIterations() is IterationsValidation.ZeroOrNegative)
    }

    @Test
    fun `iterations over max is invalid`() = runTest {
        val repo = mockk<SettingsRepository> {
            every { settings } returns flowOf(Settings())
        }
        val viewModel = HomeViewModel(repo)
        viewModel.setIterations("200000000")
        assertTrue(viewModel.validateIterations() is IterationsValidation.ExceedsMax)
    }

    @Test
    fun `getValidIterations returns parsed value`() = runTest {
        val repo = mockk<SettingsRepository> {
            every { settings } returns flowOf(Settings())
        }
        val viewModel = HomeViewModel(repo)
        viewModel.setIterations("12345")
        assertEquals(12345, viewModel.getValidIterations())
    }

    @Test
    fun `getValidIterations returns null for invalid`() = runTest {
        val repo = mockk<SettingsRepository> {
            every { settings } returns flowOf(Settings())
        }
        val viewModel = HomeViewModel(repo)
        viewModel.setIterations("abc")
        assertNull(viewModel.getValidIterations())
    }
}

class TextEncryptViewModelTest {

    @Test
    fun `encrypt success updates state with result`() = runTest {
        val crypto = mockk<CryptoManager>()
        every { crypto.iterations = any() } just Runs
        every { crypto.encryptText("hello", "pwd", Algorithm.AES_256) } returns "ENCRYPTED_BASE64"

        val viewModel = TextEncryptViewModel(crypto)
        viewModel.encrypt("hello", "pwd", Algorithm.AES_256, 10000)

        assertEquals("ENCRYPTED_BASE64", viewModel.state.value.result)
        assertFalse(viewModel.state.value.isProcessing)
        assertEquals(1, viewModel.state.value.logEntries.size)
    }

    @Test
    fun `encrypt with blank text does nothing`() = runTest {
        val crypto = mockk<CryptoManager>()
        val viewModel = TextEncryptViewModel(crypto)
        viewModel.encrypt("  ", "pwd", Algorithm.AES_256, 10000)

        assertNull(viewModel.state.value.result)
        assertFalse(viewModel.state.value.isProcessing)
    }

    @Test
    fun `encrypt error updates state with error`() = runTest {
        val crypto = mockk<CryptoManager>()
        every { crypto.iterations = any() } just Runs
        every { crypto.encryptText(any(), any(), any()) } throws RuntimeException("Crypto error")

        val viewModel = TextEncryptViewModel(crypto)
        viewModel.encrypt("text", "pwd", Algorithm.AES_256, 10000)

        assertNotNull(viewModel.state.value.error)
        assertTrue(viewModel.state.value.error!!.contains("Crypto error"))
    }

    @Test
    fun `clearError removes error from state`() = runTest {
        val crypto = mockk<CryptoManager>()
        every { crypto.iterations = any() } just Runs
        every { crypto.encryptText(any(), any(), any()) } throws RuntimeException("err")

        val viewModel = TextEncryptViewModel(crypto)
        viewModel.encrypt("text", "pwd", Algorithm.AES_256, 10000)
        assertNotNull(viewModel.state.value.error)

        viewModel.clearError()
        assertNull(viewModel.state.value.error)
    }
}

class TextDecryptViewModelTest {

    @Test
    fun `decrypt success updates state`() = runTest {
        val crypto = mockk<CryptoManager>()
        every { crypto.iterations = any() } just Runs
        every { crypto.decryptText("CIPHERTEXT", "pwd", Algorithm.AES_256) } returns "plaintext"

        val viewModel = TextDecryptViewModel(crypto)
        viewModel.decrypt("CIPHERTEXT", "pwd", Algorithm.AES_256, 10000)

        assertEquals("plaintext", viewModel.state.value.result)
        assertFalse(viewModel.state.value.isProcessing)
    }

    @Test
    fun `decrypt with blank input does nothing`() = runTest {
        val crypto = mockk<CryptoManager>()
        val viewModel = TextDecryptViewModel(crypto)
        viewModel.decrypt("", "pwd", Algorithm.AES_256, 10000)

        assertNull(viewModel.state.value.result)
    }

    @Test
    fun `decrypt error updates state with error`() = runTest {
        val crypto = mockk<CryptoManager>()
        every { crypto.iterations = any() } just Runs
        every { crypto.decryptText(any(), any(), any()) } throws IllegalArgumentException("Bad format")

        val viewModel = TextDecryptViewModel(crypto)
        viewModel.decrypt("bad-data", "pwd", Algorithm.AES_256, 10000)

        assertNotNull(viewModel.state.value.error)
    }
}
