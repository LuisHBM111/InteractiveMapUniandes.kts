package com.uniandes.interactivemapuniandes.model.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class PreferencesRepository(private val context: Context) {

    fun languageFlow(): Flow<String> = context.dataStore.data.map { // Current language tag
        it[KEY_LANGUAGE] ?: "es-CO"
    }

    fun notificationsFlow(): Flow<Boolean> = context.dataStore.data.map {
        it[KEY_NOTIFICATIONS] ?: true
    }

    fun darkModeFlow(): Flow<Boolean> = context.dataStore.data.map {
        it[KEY_DARK_MODE] ?: false
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { it[KEY_LANGUAGE] = language }
    }

    suspend fun setNotifications(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATIONS] = enabled }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DARK_MODE] = enabled }
    }

    companion object {
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode_enabled")
    }
}
