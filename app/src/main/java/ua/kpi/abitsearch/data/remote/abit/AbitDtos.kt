package ua.kpi.abitsearch.data.remote.abit

import com.google.gson.annotations.SerializedName

/*
 * DTO відповідей сервера AbitSearch. Gson не знає про non-null типи Kotlin,
 * тому текстові поля, яких може не бути у відповіді, оголошено nullable.
 */

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
)

data class RegisterRequest(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
)

data class UserDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("role") val role: String?,
)

/** Відповідь на вхід і реєстрацію: JWT та дані користувача. */
data class TokenResponse(
    @SerializedName("token") val token: String?,
    @SerializedName("user") val user: UserDto?,
)

data class ImportLogDto(
    @SerializedName("startedAt") val startedAt: Long,
    @SerializedName("finishedAt") val finishedAt: Long?,
    @SerializedName("status") val status: String?,
    @SerializedName("programs") val programs: Int,
    @SerializedName("stats") val stats: Int,
    @SerializedName("message") val message: String?,
)

data class AdminStatsDto(
    @SerializedName("usersCount") val usersCount: Int,
    @SerializedName("adminsCount") val adminsCount: Int,
    @SerializedName("lastImport") val lastImport: ImportLogDto?,
    @SerializedName("importRunning") val importRunning: Boolean,
)

/** Тіло відповіді з повідомленням: {"message": "..."} */
data class ErrorDto(
    @SerializedName("message") val message: String?,
)

data class MetaDto(
    @SerializedName("source") val source: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("lastImportAt") val lastImportAt: Long?,
    @SerializedName("latestYear") val latestYear: Int?,
    @SerializedName("universities") val universities: Int,
    @SerializedName("programs") val programs: Int,
    @SerializedName("stats") val stats: Int,
)

data class UniversityDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String?,
    @SerializedName("shortName") val shortName: String?,
    @SerializedName("ownership") val ownership: String?,
    @SerializedName("region") val region: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("address") val address: String?,
    @SerializedName("site") val site: String?,
    @SerializedName("programsCount") val programsCount: Int,
)

data class ProgramDto(
    @SerializedName("id") val id: Long,
    @SerializedName("universityId") val universityId: Long,
    @SerializedName("universityName") val universityName: String?,
    @SerializedName("level") val level: String?,
    @SerializedName("base") val base: String?,
    @SerializedName("specialtyCode") val specialtyCode: String?,
    @SerializedName("specialtyName") val specialtyName: String?,
    @SerializedName("specialtyKey") val specialtyKey: String?,
    @SerializedName("programName") val programName: String?,
    @SerializedName("lastYear") val lastYear: Int,
    @SerializedName("budget") val budget: Int,
    @SerializedName("contract") val contract: Int,
)

data class YearStatDto(
    @SerializedName("year") val year: Int,
    @SerializedName("dayBudget") val dayBudget: Int,
    @SerializedName("dayContract") val dayContract: Int,
    @SerializedName("partBudget") val partBudget: Int,
    @SerializedName("partContract") val partContract: Int,
    @SerializedName("eveningBudget") val eveningBudget: Int,
    @SerializedName("eveningContract") val eveningContract: Int,
)

data class LicenseDto(
    @SerializedName("volume") val volume: Int,
    @SerializedName("accredited") val accredited: Boolean,
    @SerializedName("accreditationUntil") val accreditationUntil: String?,
)

data class ProgramDetailDto(
    @SerializedName("program") val program: ProgramDto,
    @SerializedName("stats") val stats: List<YearStatDto>?,
    @SerializedName("license") val license: LicenseDto?,
)

data class SpecialtyDto(
    @SerializedName("key") val key: String,
    @SerializedName("code") val code: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("level") val level: String?,
    @SerializedName("year") val year: Int,
    @SerializedName("programsCount") val programsCount: Int,
    @SerializedName("universitiesCount") val universitiesCount: Int,
    @SerializedName("budget") val budget: Int,
    @SerializedName("contract") val contract: Int,
)
