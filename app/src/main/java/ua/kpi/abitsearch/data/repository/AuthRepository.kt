package ua.kpi.abitsearch.data.repository

import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import ua.kpi.abitsearch.data.auth.AuthDataStore
import ua.kpi.abitsearch.data.auth.Jwt
import ua.kpi.abitsearch.data.remote.abit.AbitApi
import ua.kpi.abitsearch.data.remote.abit.AdminStatsDto
import ua.kpi.abitsearch.data.remote.abit.ErrorDto
import ua.kpi.abitsearch.data.remote.abit.LoginRequest
import ua.kpi.abitsearch.data.remote.abit.RegisterRequest
import ua.kpi.abitsearch.data.remote.abit.TokenResponse
import ua.kpi.abitsearch.data.remote.abit.UserDto
import ua.kpi.abitsearch.domain.model.AuthSession
import ua.kpi.abitsearch.domain.model.Role
import ua.kpi.abitsearch.domain.model.User

/** Результат входу або реєстрації. */
sealed interface AuthResult {
    data object Success : AuthResult
    data class Failure(val message: String) : AuthResult
}

/**
 * Автентифікація: вхід, реєстрація, вихід, стан сесії та захищені запити.
 * Стан сесії обчислюється з токена в DataStore, тож після перезапуску застосунку
 * користувач лишається в системі, поки токен не протермінується.
 */
class AuthRepository(
    private val api: AbitApi,
    private val store: AuthDataStore,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val gson = Gson()
    private val _notice = MutableStateFlow<String?>(null)

    /** Повідомлення для екрана входу, наприклад «Сесія завершилась». */
    val notice: StateFlow<String?> = _notice.asStateFlow()

    val session: Flow<AuthSession> = store.tokenFlow.map { token -> sessionFrom(token) }

    fun sessionFrom(token: String?): AuthSession {
        if (token.isNullOrBlank()) return AuthSession.LoggedOut
        val claims = Jwt.decode(token) ?: return AuthSession.LoggedOut
        if (claims.expiresAt * 1000 <= clock()) return AuthSession.Expired
        val user = User(claims.sub, claims.name.orEmpty(), claims.email.orEmpty(), Role.from(claims.role))
        return AuthSession.LoggedIn(user, expiresAt = claims.expiresAt * 1000)
    }

    suspend fun login(email: String, password: String): AuthResult =
        authenticate { api.login(LoginRequest(email.trim(), password)) }

    suspend fun register(name: String, email: String, password: String): AuthResult =
        authenticate { api.register(RegisterRequest(name.trim(), email.trim(), password)) }

    private suspend fun authenticate(call: suspend () -> TokenResponse): AuthResult {
        return try {
            val token = call().token
            if (token.isNullOrBlank()) {
                AuthResult.Failure("Сервер не повернув токен")
            } else {
                _notice.value = null
                store.saveToken(token)
                AuthResult.Success
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            AuthResult.Failure(e.serverMessage())
        }
    }

    /** Профіль із захищеного ресурсу GET /auth/me. На 401 сесія завершується. */
    suspend fun profile(): Result<User> = protected { api.me().toDomain() }

    /** Статистика лише для адміністратора: GET /admin/stats. */
    suspend fun adminStats(): Result<AdminStatsDto> = protected { api.adminStats() }

    /** Запуск імпорту ЄДЕБО на сервері (лише ADMIN): POST /admin/import. */
    suspend fun startImport(): Result<String> = protected { api.startImport().message ?: "Імпорт запущено" }

    private suspend fun <T> protected(call: suspend () -> T): Result<T> = try {
        Result.success(call())
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        if (e is HttpException && e.code() == 401) expireSession()
        Result.failure(Exception(e.serverMessage()))
    }

    suspend fun logout() {
        _notice.value = null
        store.clearToken()
    }

    /** Викликається, коли сервер відхилив токен або минув термін його дії. */
    suspend fun expireSession() {
        _notice.value = "Сесія завершилась. Увійдіть знову."
        store.clearToken()
    }

    /** Повідомлення з тіла відповіді сервера {"message": "..."} або загальне повідомлення. */
    private fun Exception.serverMessage(): String {
        if (this is HttpException) {
            val body = response()?.errorBody()?.string()
            val message = runCatching { gson.fromJson(body, ErrorDto::class.java)?.message }.getOrNull()
            if (!message.isNullOrBlank()) return message
        }
        return toUserMessage()
    }

    private fun UserDto.toDomain() = User(id, name.orEmpty(), email.orEmpty(), Role.from(role))
}
