package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "natiq_settings")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val API_KEY = stringPreferencesKey("gemini_api_key")
        val SELECTED_MODEL = stringPreferencesKey("selected_model")
        val CHUNK_SIZE = intPreferencesKey("chunk_size")
        val SILENCE_MS = intPreferencesKey("silence_ms")
        val APPLY_SPEED_EXPORT = booleanPreferencesKey("apply_speed_export")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    val apiKeyFlow: Flow<String> = context.dataStore.data.map { preferences ->
        val userKey = preferences[PreferencesKeys.API_KEY] ?: ""
        if (userKey.isNotBlank()) {
            userKey
        } else {
            // Check BuildConfig if provided in .env
            try {
                val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
                val envKey = field.get(null) as? String ?: ""
                envKey.trim()
            } catch (e: Exception) {
                ""
            }
        }
    }

    val selectedModelFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SELECTED_MODEL] ?: "gemini-3.8-flash-tts"
    }

    val chunkSizeFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CHUNK_SIZE] ?: 1200
    }

    val silenceMsFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SILENCE_MS] ?: 200
    }

    val applySpeedExportFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.APPLY_SPEED_EXPORT] ?: true
    }

    val appLanguageFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.APP_LANGUAGE] ?: "ar"
    }

    suspend fun saveApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.API_KEY] = key.trim()
        }
    }

    suspend fun saveSelectedModel(modelId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_MODEL] = modelId
        }
    }

    suspend fun saveChunkSize(size: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CHUNK_SIZE] = size.coerceIn(400, 3000)
        }
    }

    suspend fun saveSilenceMs(ms: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SILENCE_MS] = ms.coerceIn(50, 1000)
        }
    }

    suspend fun saveApplySpeedExport(apply: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APPLY_SPEED_EXPORT] = apply
        }
    }

    suspend fun saveAppLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE] = lang
        }
    }
}
