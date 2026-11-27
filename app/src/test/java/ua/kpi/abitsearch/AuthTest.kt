package ua.kpi.abitsearch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64
import ua.kpi.abitsearch.data.auth.Jwt
import ua.kpi.abitsearch.domain.validation.AuthValidator

class AuthTest {

    /** Токен у форматі сервера: header.payload.signature (Base64URL), sub - рядок, як у java-jwt. */
    private fun token(payload: String): String {
        val enc = Base64.getUrlEncoder().withoutPadding()
        return enc.encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray()) + "." +
            enc.encodeToString(payload.toByteArray()) + ".signature"
    }

    @Test
    fun jwtPayloadIsDecoded() {
        val claims = Jwt.decode(token("""{"iss":"abitsearch","sub":"7","name":"Олексій","email":"student@abitsearch.ua","role":"USER","iat":1000,"exp":2000}"""))
        assertNotNull(claims)
        assertEquals(7L, claims!!.sub)
        assertEquals("USER", claims.role)
        assertEquals(2000L, claims.expiresAt)
    }

    @Test
    fun malformedTokenIsRejected() {
        assertNull(Jwt.decode("not-a-jwt"))
        assertNull(Jwt.decode("a.b"))
    }

    @Test
    fun emailValidation() {
        assertEquals("Введіть e-mail", AuthValidator.email(" "))
        assertNotNull(AuthValidator.email("name@"))
        assertNull(AuthValidator.email("name@example.com"))
    }

    @Test
    fun passwordValidation() {
        assertEquals("Мінімум 8 символів", AuthValidator.newPassword("abc1"))
        assertEquals("Додайте хоча б одну цифру", AuthValidator.newPassword("abcdefgh"))
        assertNull(AuthValidator.newPassword("abcdefg1"))
        assertEquals("Паролі не збігаються", AuthValidator.confirmPassword("abcdefg1", "abcdefg2"))
    }

    @Test
    fun nameValidation() {
        assertNull(AuthValidator.name("Олексій Павлов"))
        assertNull(AuthValidator.name("Мар'яна"))
        assertNotNull(AuthValidator.name("Олег2"))
    }
}
