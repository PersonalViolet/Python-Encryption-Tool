package com.example.encryptapp.core.domain.model

/**
 * Supported AES algorithms with their key lengths in bytes.
 * Mirrors crypto_manager.py ALGORITHMS dict.
 */
enum class Algorithm(val displayName: String, val keyLengthBytes: Int) {
    AES_128("AES-128", 16),
    AES_192("AES-192", 24),
    AES_256("AES-256", 32);

    companion object {
        fun fromDisplayName(name: String): Algorithm {
            return entries.find { it.displayName == name } ?: AES_256
        }
    }
}
