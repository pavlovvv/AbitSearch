package ua.kpi.abitsearch.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.authDataStore by preferencesDataStore(name = "auth")

/**
 * Зберігання JWT у DataStore (як у ПЗ 11).
 * cachedToken - копія в пам'яті для AuthInterceptor: OkHttp викликає інтерцептор синхронно,
 * і читати DataStore там через runBlocking небажано.
 */
class AuthDataStore(context: Context) {
    private val store = context.authDataStore

    val tokenFlow: Flow<String?> = store.data.map { it[TOKEN] }

    @Volatile
    var cachedToken: String? = null
        private set

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            tokenFlow.collect { cachedToken = it }
        }
    }

    suspend fun saveToken(token: String) {
        cachedToken = token
        store.edit { it[TOKEN] = token }
    }

    suspend fun clearToken() {
        cachedToken = null
        store.edit { it.remove(TOKEN) }
    }

    private companion object {
        val TOKEN = stringPreferencesKey("jwt")
    }
}
