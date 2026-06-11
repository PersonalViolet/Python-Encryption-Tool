package com.example.encryptapp.core.crypto

/**
 * Cryptographic constants matching crypto_manager.py.
 */
object CryptoConstants {
    const val SALT_SIZE = 16
    const val IV_SIZE = 16
    const val DEFAULT_ITERATIONS = 10000
    const val CHUNK_SIZE = 64 * 1024  // 64KB
    const val MAX_ITERATIONS = 100_000_000L
}
