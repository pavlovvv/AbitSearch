package ua.kpi.abitsearch.data.remote.abit

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * REST API сервера AbitSearch (модуль server, Node.js + Express).
 * Сервер імпортує відкриті дані ЄДЕБО у свою БД і віддає їх застосунку в зручному вигляді.
 * Усі методи, крім входу й реєстрації, вимагають заголовок Authorization: Bearer <JWT>
 * (його додає AuthInterceptor).
 */
interface AbitApi {

    // ---------- Автентифікація ----------
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): TokenResponse

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): TokenResponse

    @GET("auth/me")
    suspend fun me(): UserDto

    // ---------- Дані ----------
    @GET("api/meta")
    suspend fun meta(): MetaDto

    @GET("api/universities")
    suspend fun universities(): List<UniversityDto>

    @GET("api/universities/{id}/programs")
    suspend fun programsOfUniversity(@Path("id") universityId: Long): List<ProgramDto>

    @GET("api/programs/{id}")
    suspend fun program(@Path("id") id: Long): ProgramDetailDto

    /** Програми спеціальності в усіх закладах (останній набір). */
    @GET("api/programs")
    suspend fun programsOfSpecialty(@Query("specialty") key: String, @Query("level") level: String): List<ProgramDto>

    @GET("api/specialties")
    suspend fun specialties(@Query("level") level: String): List<SpecialtyDto>

    // ---------- Адміністрування (роль ADMIN) ----------
    @GET("admin/stats")
    suspend fun adminStats(): AdminStatsDto

    @POST("admin/import")
    suspend fun startImport(): ErrorDto
}
