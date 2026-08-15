package hanzner.zebrakapp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import hanzner.zebrakapp.entity.Role;
import hanzner.zebrakapp.entity.User;
import hanzner.zebrakapp.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("Test")
@Transactional
public class JwtSecurityTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private MockMvc mockMvc;
    private User testUser;
    private User disabledUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        testUser = userRepository.save(User.builder()
                .email("jwt_user_" + System.currentTimeMillis() + "@test.cz")
                .password(passwordEncoder.encode("Password123!"))
                .nickname("JwtUser")
                .role(Role.ROLE_USER)
                .active(true)
                .build());

        disabledUser = userRepository.save(User.builder()
                .email("disabled_" + System.currentTimeMillis() + "@test.cz")
                .password(passwordEncoder.encode("Password123!"))
                .nickname("DisabledUser")
                .role(Role.ROLE_USER)
                .active(false)
                .build());
    }

    @Test
    @DisplayName("Platný JWT v HttpOnly cookie úspěšně autentizuje požadavek")
    void testValidJwtInCookie_Authenticates() throws Exception {
        CustomUserDetails userDetails = new CustomUserDetails(testUser);
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

        String validToken = jwtTokenProvider.generateToken(auth);
        Cookie jwtCookie = new Cookie("jwt_token", validToken);

        mockMvc.perform(get("/api/auth/me").cookie(jwtCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.nickname").value(testUser.getNickname()));
    }

    @Test
    @DisplayName("Platný JWT v hlavičce Authorization: Bearer úspěšně autentizuje požadavek")
    void testValidJwtInHeader_Authenticates() throws Exception {
        CustomUserDetails userDetails = new CustomUserDetails(testUser);
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

        String validToken = jwtTokenProvider.generateToken(auth);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(testUser.getEmail()));
    }

    @Test
    @DisplayName("Podvržený JWT podepsaný jiným tajným klíčem je odmítnut (401)")
    void testTamperedSignatureJwt_IsRejected() throws Exception {
        SecretKey attackerKey = Keys.hmacShaKeyFor(
                "attackerSecretKeyThatIsAtLeast256BitsLongForTestingPurposes1234567890".getBytes(StandardCharsets.UTF_8)
        );

        String forgedToken = Jwts.builder()
                .subject(testUser.getEmail())
                .claim("userId", testUser.getId())
                .claim("roles", "ROLE_ADMIN") // Pokus o eskalaci práv na admina
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(attackerKey)
                .compact();

        Cookie forgedCookie = new Cookie("jwt_token", forgedToken);

        mockMvc.perform(get("/api/auth/me").cookie(forgedCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Expirovaný JWT token je odmítnut (401)")
    void testExpiredJwt_IsRejected() throws Exception {
        SecretKey appKey = Keys.hmacShaKeyFor(
                io.jsonwebtoken.io.Decoders.BASE64.decode(
                        "dGVzdFNlY3JldEtleUZvclplYnJha0FwcFRlc3RpbmdPbmx5TmVlZHNUb0JlQXRMZWFzdDI1NkJpdHNMb25nMTIzNDU2Nzg5MA=="
                )
        );

        // Token expiroval před 1 hodinou
        String expiredToken = Jwts.builder()
                .subject(testUser.getEmail())
                .claim("userId", testUser.getId())
                .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                .expiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(appKey)
                .compact();

        Cookie expiredCookie = new Cookie("jwt_token", expiredToken);

        mockMvc.perform(get("/api/auth/me").cookie(expiredCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("JWT pro deaktivovaného uživatele (active=false) je odmítnut (401)")
    void testDisabledUserJwt_IsRejected() throws Exception {
        CustomUserDetails userDetails = new CustomUserDetails(disabledUser);
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

        String token = jwtTokenProvider.generateToken(auth);
        Cookie cookie = new Cookie("jwt_token", token);

        mockMvc.perform(get("/api/auth/me").cookie(cookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Chybějící token na chráněném endpointu vrátí 401 Unauthorized")
    void testMissingTokenOnProtectedEndpoint_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/my-places"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }
}
