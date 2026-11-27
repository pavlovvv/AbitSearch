package ua.kpi.abitsearch.data.auth

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.util.Base64

/** Корисне навантаження (payload) JWT, який видає сервер AbitSearch. */
data class JwtClaims(
    @SerializedName("sub") val sub: Long,
    @SerializedName("name") val name: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("role") val role: String?,
    @SerializedName("iat") val issuedAt: Long,
    @SerializedName("exp") val expiresAt: Long,
)

/**
 * JWT складається з трьох частин Base64URL: header.payload.signature.
 * Клієнт лише читає payload (роль, ім'я, термін дії), щоб показати потрібний UI без запиту.
 * Підпис перевіряє сервер на кожному захищеному запиті: ключ підпису є тільки в нього.
 */
object Jwt {
    private val gson = Gson()
    private val decoder = Base64.getUrlDecoder()

    /** Читає payload без перевірки підпису. null - якщо рядок не схожий на JWT. */
    fun decode(token: String): JwtClaims? = runCatching {
        val parts = token.split(".")
        require(parts.size == 3)
        gson.fromJson(String(decoder.decode(parts[1])), JwtClaims::class.java)
    }.getOrNull()
}
