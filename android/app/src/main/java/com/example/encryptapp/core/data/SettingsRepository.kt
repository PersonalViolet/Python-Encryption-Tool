package com.example.encryptapp.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.encryptapp.core.domain.model.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Persistent application settings backed by Jetpack DataStore.
 * Replaces settings.json from the Python desktop app.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val ENC_OUTPUT_PATH = stringPreferencesKey("enc_output_path")
        val DEC_OUTPUT_PATH = stringPreferencesKey("dec_output_path")
        val LANGUAGE = stringPreferencesKey("language")
        val ITERATIONS = intPreferencesKey("iterations")
    }

    /** Observe all settings as a reactive Flow. */
    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            encOutputPath = prefs[Keys.ENC_OUTPUT_PATH] ?: "",
            decOutputPath = prefs[Keys.DEC_OUTPUT_PATH] ?: "",
            language = prefs[Keys.LANGUAGE] ?: "en",
            iterations = prefs[Keys.ITERATIONS] ?: 10000
        )
    }

    suspend fun updateEncOutputPath(path: String) {
        context.dataStore.edit { it[Keys.ENC_OUTPUT_PATH] = path }
    }

    suspend fun updateDecOutputPath(path: String) {
        context.dataStore.edit { it[Keys.DEC_OUTPUT_PATH] = path }
    }

    suspend fun updateLanguage(language: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language }
    }

    suspend fun updateIterations(iterations: Int) {
        context.dataStore.edit { it[Keys.ITERATIONS] = iterations }
    }
}
