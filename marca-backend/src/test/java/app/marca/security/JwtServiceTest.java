package app.marca.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    // HS256 要求密钥至少 256 位（32 字符），随便写短了会在构造时直接抛异常
    private static final String SECRET = "unit-test-secret-key-please-32chars!!";

    @Test
    void issueThenParse_roundTrip_returnsSameUserIdAndEmail() {
        JwtService jwtService = new JwtService(SECRET, 7);

        String token = jwtService.issue(42L, "user@example.com");
        UserPrincipal principal = jwtService.parse(token);

        assertEquals(42L, principal.id());
        assertEquals("user@example.com", principal.email());
    }

    @Test
    void parse_expiredToken_throwsJwtException() {
        // expirationDays = -1：签发时 expiration 就已经在 issuedAt 之前，必然过期
        JwtService jwtService = new JwtService(SECRET, -1);
        String token = jwtService.issue(1L, "a@b.com");

        assertThrows(JwtException.class, () -> jwtService.parse(token));
    }

    @Test
    void parse_malformedToken_throwsJwtException() {
        JwtService jwtService = new JwtService(SECRET, 7);

        assertThrows(JwtException.class, () -> jwtService.parse("not-a-real-token"));
    }

    @Test
    void parse_tokenSignedWithDifferentSecret_throwsJwtException() {
        JwtService issuer = new JwtService(SECRET, 7);
        JwtService verifier = new JwtService("a-completely-different-secret-32chars", 7);
        String token = issuer.issue(1L, "a@b.com");

        assertThrows(JwtException.class, () -> verifier.parse(token));
    }
}
