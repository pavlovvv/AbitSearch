package ua.kpi.abitsearch.data.auth

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Додає заголовок Authorization: Bearer <token> до кожного запиту до API AbitSearch.
 * Якщо сервер відхилив токен (401), повідомляє про це, щоб застосунок завершив сесію
 * і показав форму входу. Запити без токена (вхід, реєстрація) цей механізм не зачіпає.
 */
class AuthInterceptor(
    private val tokenProvider: () -> String?,
    private val onUnauthorized: () -> Unit = {},
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider()
        if (token.isNullOrBlank()) return chain.proceed(chain.request())
        val response = chain.proceed(chain.request().newBuilder().header("Authorization", "Bearer $token").build())
        if (response.code == 401) onUnauthorized()
        return response
    }
}
