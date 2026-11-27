package ua.kpi.abitsearch.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ua.kpi.abitsearch.domain.model.ThemeMode

private val Context.dataStore by preferencesDataStore(name = "settings")

/** Налаштування користувача у DataStore (ключ-значення, асинхронно, без блокування UI). */
class SettingsRepository(private val context: Context) {

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK
    }

    val lastSync: Flow<Long?> = context.dataStore.data.map { it[LAST_SYNC] }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[THEME] = mode.name }
    }

    suspend fun setLastSync(time: Long) {
        context.dataStore.edit { it[LAST_SYNC] = time }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme_mode")
        val LAST_SYNC = longPreferencesKey("last_sync")
    }
}
