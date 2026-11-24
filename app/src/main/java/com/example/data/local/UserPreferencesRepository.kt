package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "se_fue_la_luz_prefs")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NOTIFICATION_PERMISSION_ASKED = booleanPreferencesKey("notification_permission_asked")
        val TRACKING_START_MILLIS = longPreferencesKey("tracking_start_millis")
    }

    val themeMode: Flow<AppThemeMode> = context.dataStore.data.map { prefs ->
        val stored = prefs[Keys.THEME_MODE]
        AppThemeMode.entries.firstOrNull { it.name == stored } ?: AppThemeMode.SYSTEM
    }

    val hasAskedNotificationPermission: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.NOTIFICATION_PERMISSION_ASKED] ?: false
    }

    val trackingStartMillis: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[Keys.TRACKING_START_MILLIS]
    }

    suspend fun ensureTrackingStartInitialized(nowMillis: Long = System.currentTimeMillis()): Long {
        var resolved = nowMillis
        context.dataStore.edit { prefs ->
            val existing = prefs[Keys.TRACKING_START_MILLIS]
            if (existing == null) {
                prefs[Keys.TRACKING_START_MILLIS] = nowMillis
                resolved = nowMillis
            } else {
                resolved = existing
            }
        }
        return resolved
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun markNotificationPermissionAsked() {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATION_PERMISSION_ASKED] = true
        }
    }

    suspend fun clearTrackingStart() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.TRACKING_START_MILLIS)
        }
    }
}
