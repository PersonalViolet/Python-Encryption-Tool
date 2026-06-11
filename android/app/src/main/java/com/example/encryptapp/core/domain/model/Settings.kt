package com.example.encryptapp.core.domain.model

/**
 * Application settings, persisted via DataStore.
 * Mirrors settings.json keys from the Python app.
 */
data class Settings(
    val encOutputPath: String = "",
    val decOutputPath: String = "",
    val language: String = "en",
    val iterations: Int = 10000
)
