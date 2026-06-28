package com.wkq.advertisingmachine.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wkq.advertisingmachine.model.SignageConfig
import com.wkq.advertisingmachine.model.SignageState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "signage_prefs")

class ConfigStore(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    companion object {
        private val KEY_CONFIG = stringPreferencesKey("signage_config_json")
        private val KEY_STATE = stringPreferencesKey("signage_state_json")
    }

    val configFlow: Flow<SignageConfig> = context.dataStore.data.map { preferences ->
        val jsonStr = preferences[KEY_CONFIG]
        if (jsonStr != null) {
            try {
                json.decodeFromString<SignageConfig>(jsonStr)
            } catch (e: Exception) {
                SignageConfig()
            }
        } else {
            SignageConfig()
        }
    }

    suspend fun saveConfig(config: SignageConfig) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CONFIG] = json.encodeToString(SignageConfig.serializer(), config)
        }
    }

    val stateFlow: Flow<SignageState?> = context.dataStore.data.map { preferences ->
        val jsonStr = preferences[KEY_STATE]
        if (jsonStr != null) {
            try {
                json.decodeFromString<SignageState>(jsonStr)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    suspend fun saveState(state: SignageState) {
        context.dataStore.edit { preferences ->
            preferences[KEY_STATE] = json.encodeToString(SignageState.serializer(), state)
        }
    }

    suspend fun clearState() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_STATE)
        }
    }
}
