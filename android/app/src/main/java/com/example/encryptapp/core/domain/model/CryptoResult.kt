package com.example.encryptapp.core.domain.model

/**
 * Sealed class representing the result of a crypto operation.
 */
sealed class CryptoResult<out T> {
    data class Success<T>(val data: T) : CryptoResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : CryptoResult<Nothing>()
}
