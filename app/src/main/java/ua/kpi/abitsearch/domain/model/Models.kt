package ua.kpi.abitsearch.domain.model

/** Заклад вищої освіти. Джерело: реєстр ЄДЕБО, імпортований сервером AbitSearch. */
data class University(
    val id: Long,
    val name: String,
    val shortName: String,
    val region: String,
    val ownership: Ownership,
    val site: String?,
    val address: String?,
    /** Кількість освітніх програм з набором в останньому році. */
    val programsCount: Int,
)

/**
 * Освітня програма закладу: відкриті дані ЄДЕБО про зарахування.
 * budget / contract - кількість зарахованих в останньому році набору (lastYear).
 */
data class Program(
    val id: Long,
    val universityId: Long,
    val universityName: String,
    val level: EducationLevel,
    val base: String,
    val specialtyCode: String,
    val specialtyName: String,
    val specialtyKey: String,
    val name: String,
    val lastYear: Int,
    val budget: Int,
    val contract: Int,
    val license: License?,
) {
    val total: Int get() = budget + contract
}

/** Ліцензований обсяг спеціальності в закладі та акредитація. */
data class License(val volume: Int, val accredited: Boolean, val accreditationUntil: String?)

/** Зарахування за рік у розрізі форм навчання. */
data class YearStat(
    val year: Int,
    val dayBudget: Int,
    val dayContract: Int,
    val partBudget: Int,
    val partContract: Int,
    val eveningBudget: Int,
    val eveningContract: Int,
) {
    val budget: Int get() = dayBudget + partBudget + eveningBudget
    val contract: Int get() = dayContract + partContract + eveningContract
    val total: Int get() = budget + contract
}

/** Спеціальність останнього набору: скільки закладів і програм, скільки зараховано. */
data class Specialty(
    val key: String,
    val code: String,
    val name: String,
    val level: EducationLevel,
    val year: Int,
    val programsCount: Int,
    val universitiesCount: Int,
    val budget: Int,
    val contract: Int,
) {
    val total: Int get() = budget + contract
}

data class Favorite(
    val type: FavoriteType,
    val refId: Long,
    val title: String,
    val subtitle: String,
    val addedAt: Long,
)

enum class Ownership(val title: String) {
    STATE("Державна"), COMMUNAL("Комунальна"), PRIVATE("Приватна");

    companion object {
        /** ЄДЕБО повертає форму фінансування текстом, наприклад «Державна». */
        fun fromEdbo(text: String?): Ownership = when {
            text == null -> STATE
            text.contains("риват", ignoreCase = true) -> PRIVATE
            text.contains("омунал", ignoreCase = true) -> COMMUNAL
            else -> STATE
        }
    }
}

enum class EducationLevel(val title: String) {
    BACHELOR("Бакалавр"), MASTER("Магістр");

    companion object {
        fun from(value: String?): EducationLevel = entries.firstOrNull { it.name == value } ?: BACHELOR
    }
}

enum class FavoriteType { PROGRAM, UNIVERSITY }

enum class ThemeMode(val title: String) { SYSTEM("Системна"), LIGHT("Світла"), DARK("Темна") }

/** Регіон України з кодом, який використовує API ЄДЕБО (параметр rg). */
data class Region(val code: String, val name: String)

/** Роль користувача, закодована в JWT (claim "role"). */
enum class Role(val title: String) {
    USER("Користувач"), ADMIN("Адміністратор");

    companion object {
        fun from(value: String?): Role = entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: USER
    }
}

/** Користувач застосунку. */
data class User(
    val id: Long,
    val name: String,
    val email: String,
    val role: Role,
)

/** Стан сесії: обчислюється з токена, збереженого в DataStore. */
sealed interface AuthSession {
    data object Loading : AuthSession
    data object LoggedOut : AuthSession
    /** Токен є, але термін його дії минув - потрібен повторний вхід. */
    data object Expired : AuthSession
    data class LoggedIn(val user: User, val expiresAt: Long) : AuthSession
}
