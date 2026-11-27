package ua.kpi.abitsearch.data.repository

/** Результат операції оновлення: успіх або помилка з повідомленням для користувача. */
sealed interface RefreshResult {
    data object Success : RefreshResult
    data object Skipped : RefreshResult
    data class Error(val message: String, val hasCache: Boolean) : RefreshResult
}

fun Throwable.toUserMessage(): String = when (this) {
    is java.net.UnknownHostException, is java.net.ConnectException -> "Немає з'єднання з сервером AbitSearch"
    is java.net.SocketTimeoutException -> "Сервер не відповідає"
    is retrofit2.HttpException -> when (code()) {
        401 -> "Сесія завершилась, увійдіть знову"
        403 -> "Недостатньо прав"
        404 -> "Дані не знайдено"
        else -> "Помилка сервера (${code()})"
    }
    is com.google.gson.JsonParseException -> "Неочікуваний формат даних"
    else -> "Не вдалося завантажити дані"
}
