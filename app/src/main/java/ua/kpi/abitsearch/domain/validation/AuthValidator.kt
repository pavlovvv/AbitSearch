package ua.kpi.abitsearch.domain.validation

/**
 * Правила валідації форм входу та реєстрації (ПЗ 6).
 * Кожна функція повертає текст помилки або null, якщо значення коректне.
 * Чисті функції без залежностей від Android - легко покрити unit-тестами.
 */
object AuthValidator {

    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val NAME = Regex("^[\\p{L}][\\p{L} '’-]*$")
    const val MIN_PASSWORD = 8

    fun email(value: String): String? = when {
        value.isBlank() -> "Введіть e-mail"
        !EMAIL.matches(value.trim()) -> "Некоректний e-mail, наприклад name@example.com"
        else -> null
    }

    /** Пароль на екрані входу: перевіряємо лише, що його введено. */
    fun loginPassword(value: String): String? = if (value.isEmpty()) "Введіть пароль" else null

    fun name(value: String): String? = when {
        value.isBlank() -> "Введіть ім'я"
        value.trim().length < 2 -> "Ім'я занадто коротке"
        !NAME.matches(value.trim()) -> "Лише літери, пробіл, дефіс або апостроф"
        else -> null
    }

    fun newPassword(value: String): String? = when {
        value.isEmpty() -> "Придумайте пароль"
        value.length < MIN_PASSWORD -> "Мінімум $MIN_PASSWORD символів"
        value.none { it.isDigit() } -> "Додайте хоча б одну цифру"
        value.none { it.isLetter() } -> "Додайте хоча б одну літеру"
        else -> null
    }

    fun confirmPassword(password: String, confirm: String): String? = when {
        confirm.isEmpty() -> "Повторіть пароль"
        confirm != password -> "Паролі не збігаються"
        else -> null
    }

    /** Вимоги до пароля для живої підказки під полем: текст вимоги → виконана чи ні. */
    fun passwordChecklist(value: String): List<Pair<String, Boolean>> = listOf(
        "Щонайменше $MIN_PASSWORD символів" to (value.length >= MIN_PASSWORD),
        "Хоча б одна цифра" to value.any { it.isDigit() },
        "Хоча б одна літера" to value.any { it.isLetter() },
    )
}
