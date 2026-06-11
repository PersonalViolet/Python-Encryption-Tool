package com.example.encryptapp.core.crypto

import com.example.encryptapp.core.domain.model.Algorithm
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Stateless crypto operations manager.
 * Mirrors crypto_manager.py CryptoManager class.
 *
 * Uses javax.crypto (JCA) built into Android — no external crypto dependencies.
 *
 * Output format for text:  base64(salt + iv + ciphertext)
 * Output format for files: [salt][iv][encrypted_stream]
 *
 * NOTE: Java "AES/CBC/PKCS5Padding" is PKCS7 for AES's 16-byte block size
 * and produces byte-identical output to Python's PKCS7 + raw AES-CBC.
 */
class CryptoManager {

    /** Iteration count for PBKDF2. Overridable at runtime (mirrors Python self.ITERATIONS). */
    var iterations: Int = CryptoConstants.DEFAULT_ITERATIONS

    private val secureRandom = SecureRandom()

    // ── Key Derivation ──────────────────────────────────────────────

    /**
     * Derive an AES key from password + salt using PBKDF2-HMAC-SHA256.
     * Must produce byte-identical keys to Python's PBKDF2HMAC(SHA256).
     */
    private fun deriveKey(password: String, salt: ByteArray, keyLength: Int): ByteArray {
        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            iterations,
            keyLength * 8  // convert bytes to bits
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    // ── Text Encryption ─────────────────────────────────────────────

    /**
     * Encrypt plaintext string → base64(salt + iv + ciphertext).
     * @param text Plaintext UTF-8 string.
     * @param password User passphrase.
     * @param algorithm AES variant.
     * @return Base64-encoded encrypted payload.
     */
    fun encryptText(text: String, password: String, algorithm: Algorithm): String {
        if (text.isEmpty()) return ""

        val keyLen = algorithm.keyLengthBytes
        val salt = ByteArray(CryptoConstants.SALT_SIZE).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(CryptoConstants.IV_SIZE).also { secureRandom.nextBytes(it) }

        val key = deriveKey(password, salt, keyLen)
        val secretKey = SecretKeySpec(key, "AES")

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))

        val ciphertext = cipher.doFinal(text.toByteArray(Charsets.UTF_8))

        // Combine: salt + iv + ciphertext, then base64 encode
        val combined = salt + iv + ciphertext
        return Base64.getEncoder().encodeToString(combined)
    }

    /**
     * Decrypt base64(salt + iv + ciphertext) → plaintext string.
     * @param encryptedB64 Base64-encoded encrypted payload.
     * @param password User passphrase.
     * @param algorithm AES variant.
     * @return Decrypted UTF-8 string.
     * @throws IllegalArgumentException if format is invalid.
     * @throws javax.crypto.BadPaddingException if password is wrong or data is corrupted.
     */
    fun decryptText(encryptedB64: String, password: String, algorithm: Algorithm): String {
        if (encryptedB64.isEmpty()) return ""

        val combined = Base64.getDecoder().decode(encryptedB64)

        val headerSize = CryptoConstants.SALT_SIZE + CryptoConstants.IV_SIZE
        if (combined.size < headerSize) {
            throw IllegalArgumentException("Invalid encrypted data format: too short")
        }

        val salt = combined.copyOfRange(0, CryptoConstants.SALT_SIZE)
        val iv = combined.copyOfRange(
            CryptoConstants.SALT_SIZE,
            CryptoConstants.SALT_SIZE + CryptoConstants.IV_SIZE
        )
        val ciphertext = combined.copyOfRange(headerSize, combined.size)

        val keyLen = algorithm.keyLengthBytes
        val key = deriveKey(password, salt, keyLen)
        val secretKey = SecretKeySpec(key, "AES")

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))

        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, Charsets.UTF_8)
    }

    // ── File Encryption ─────────────────────────────────────────────

    /**
     * Encrypt a file from [inputStream] to [outputStream].
     * Format: [salt (16)][iv (16)][encrypted stream with PKCS7 padding]
     *
     * Uses CipherOutputStream for streaming — handles chunked padding
     * internally, matching Python's chunked padder + encryptor pattern.
     *
     * @param inputStream Source plaintext stream (not closed by this method).
     * @param outputStream Destination for encrypted data (not closed by this method).
     * @param password User passphrase.
     * @param algorithm AES variant.
     * @param onProgress Optional progress callback (bytesProcessed, totalBytes).
     */
    fun encryptFile(
        inputStream: InputStream,
        outputStream: OutputStream,
        fileSize: Long,
        password: String,
        algorithm: Algorithm,
        onProgress: ((processed: Long, total: Long) -> Unit)? = null
    ) {
        val keyLen = algorithm.keyLengthBytes
        val salt = ByteArray(CryptoConstants.SALT_SIZE).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(CryptoConstants.IV_SIZE).also { secureRandom.nextBytes(it) }

        val key = deriveKey(password, salt, keyLen)
        val secretKey = SecretKeySpec(key, "AES")

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))

        // Write header: salt + iv
        outputStream.write(salt)
        outputStream.write(iv)

        // Stream encryption through CipherOutputStream
        val cipherOutputStream = CipherOutputStream(outputStream, cipher)
        val buffer = ByteArray(CryptoConstants.CHUNK_SIZE)
        var processed: Long = 0

        try {
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                cipherOutputStream.write(buffer, 0, bytesRead)
                processed += bytesRead
                onProgress?.invoke(processed, fileSize)
            }
            cipherOutputStream.close()  // Triggers doFinal() which pads and finalizes
        } catch (e: Exception) {
            try { cipherOutputStream.close() } catch (_: Exception) {}
            throw e
        }
    }

    /**
     * Decrypt a file from [inputStream] to [outputStream].
     * Reads [salt (16)][iv (16)] header, then decrypts the remaining stream.
     *
     * @param inputStream Source encrypted stream (not closed by this method).
     * @param outputStream Destination for decrypted data (not closed by this method).
     * @param password User passphrase.
     * @param algorithm AES variant.
     * @param onProgress Optional progress callback (bytesProcessed, totalBytes).
     */
    fun decryptFile(
        inputStream: InputStream,
        outputStream: OutputStream,
        fileSize: Long,
        password: String,
        algorithm: Algorithm,
        onProgress: ((processed: Long, total: Long) -> Unit)? = null
    ) {
        val headerSize = CryptoConstants.SALT_SIZE + CryptoConstants.IV_SIZE
        if (fileSize < headerSize) {
            throw IllegalArgumentException("File too small to be a valid encrypted file")
        }

        val salt = ByteArray(CryptoConstants.SALT_SIZE)
        val iv = ByteArray(CryptoConstants.IV_SIZE)

        // Read header
        var bytesRead = inputStream.read(salt)
        if (bytesRead < CryptoConstants.SALT_SIZE) {
            throw IllegalArgumentException("Failed to read salt from file header")
        }
        bytesRead = inputStream.read(iv)
        if (bytesRead < CryptoConstants.IV_SIZE) {
            throw IllegalArgumentException("Failed to read IV from file header")
        }

        val keyLen = algorithm.keyLengthBytes
        val key = deriveKey(password, salt, keyLen)
        val secretKey = SecretKeySpec(key, "AES")

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))

        // Stream decryption through CipherInputStream
        // The cipher stream handles un-padding internally via doFinal()
        val cipherInputStream = CipherInputStream(inputStream, cipher)
        val buffer = ByteArray(CryptoConstants.CHUNK_SIZE)
        var processed: Long = headerSize.toLong()

        try {
            while (true) {
                val n = cipherInputStream.read(buffer)
                if (n == -1) break
                outputStream.write(buffer, 0, n)
                processed += n
                onProgress?.invoke(processed, fileSize)
            }
        } catch (e: Exception) {
            try { cipherInputStream.close() } catch (_: Exception) {}
            try { outputStream.close() } catch (_: Exception) {}
            throw e
        }
    }
}
