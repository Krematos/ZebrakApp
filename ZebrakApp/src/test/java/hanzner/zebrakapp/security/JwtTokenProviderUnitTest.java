package hanzner.zebrakapp.security;

import hanzner.zebrakapp.entity.Role;
import hanzner.zebrakapp.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtTokenProvider Unit Testy")
class JwtTokenProviderUnitTest {

    private static final String VALID_SECRET_STRING = "VerySecretKeyForTestingJwtTokenProviderNeedsToBeLongEnough123456!";
    private static final String VALID_BASE64_SECRET = Base64.getEncoder().encodeToString(VALID_SECRET_STRING.getBytes(StandardCharsets.UTF_8));
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    private JwtTokenProvider jwtTokenProvider;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(VALID_BASE64_SECRET, EXPIRATION_MS);

        User user = User.builder()
                .id(1L)
                .email("token.user@example.cz")
                .nickname("TokenUser")
                .role(Role.ROLE_USER)
                .active(true)
                .build();
        CustomUserDetails userDetails = new CustomUserDetails(user);
        authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @Nested
    @DisplayName("Testy konstruktoru a konfigurace")
    class ConstructorTests {

        @Test
        @DisplayName("Vyhodí IllegalArgumentException při null nebo prázdném secret")
        void testConstructor_BlankSecret_ThrowsException() {
            assertThrows(IllegalArgumentException.class, () -> new JwtTokenProvider(null, 1000));
            assertThrows(IllegalArgumentException.class, () -> new JwtTokenProvider("", 1000));
            assertThrows(IllegalArgumentException.class, () -> new JwtTokenProvider("   ", 1000));
        }

        @Test
        @DisplayName("Vyhodí IllegalArgumentException při klíči kratším než 32 bajtů (256 bitů)")
        void testConstructor_ShortKey_ThrowsException() {
            String shortSecret = Base64.getEncoder().encodeToString("shortKey123".getBytes(StandardCharsets.UTF_8));
            assertThrows(IllegalArgumentException.class, () -> new JwtTokenProvider(shortSecret, 1000));
        }
    }

    @Nested
    @DisplayName("Generování a validace tokenu")
    class TokenOperationsTests {

        @Test
        @DisplayName("Úspěšně vygeneruje platný token a extrahuje z něj e-mail")
        void testGenerateAndParseToken() {
            String token = jwtTokenProvider.generateToken(authentication);

            assertNotNull(token);
            assertFalse(token.isBlank());
            assertTrue(jwtTokenProvider.validateToken(token));

            String email = jwtTokenProvider.getEmailFromToken(token);
            assertEquals("token.user@example.cz", email);
        }

        @Test
        @DisplayName("validateToken vrátí false pro neplatný token")
        void testValidateToken_Invalid() {
            assertFalse(jwtTokenProvider.validateToken("invalid.token.structure"));
            assertFalse(jwtTokenProvider.validateToken(""));
            assertFalse(jwtTokenProvider.validateToken(null));
        }

        @Test
        @DisplayName("validateToken vrátí false pro token podepsaný jiným klíčem")
        void testValidateToken_WrongSignature() {
            String otherSecret = Base64.getEncoder().encodeToString("AnotherVerySecretKeyForTestingJwtTokens1234567890!".getBytes(StandardCharsets.UTF_8));
            JwtTokenProvider otherProvider = new JwtTokenProvider(otherSecret, EXPIRATION_MS);

            String foreignToken = otherProvider.generateToken(authentication);
            assertFalse(jwtTokenProvider.validateToken(foreignToken));
        }
    }

    @Nested
    @DisplayName("Vytváření cookies")
    class CookieTests {

        @Test
        @DisplayName("createJwtCookie vytvoří HttpOnly cookie se správným maxAge a path")
        void testCreateJwtCookie() {
            ResponseCookie cookie = jwtTokenProvider.createJwtCookie("mock.jwt.token");

            assertNotNull(cookie);
            assertEquals("jwt_token", cookie.getName());
            assertEquals("mock.jwt.token", cookie.getValue());
            assertTrue(cookie.isHttpOnly());
            assertEquals("/", cookie.getPath());
            assertEquals(EXPIRATION_MS / 1000, cookie.getMaxAge().getSeconds());
            assertEquals("Lax", cookie.getSameSite());
        }

        @Test
        @DisplayName("createCleanJwtCookie vytvoří mazací cookie s maxAge 0")
        void testCreateCleanJwtCookie() {
            ResponseCookie cookie = jwtTokenProvider.createCleanJwtCookie();

            assertNotNull(cookie);
            assertEquals("jwt_token", cookie.getName());
            assertEquals("", cookie.getValue());
            assertTrue(cookie.isHttpOnly());
            assertEquals(0, cookie.getMaxAge().getSeconds());
        }
    }
}
