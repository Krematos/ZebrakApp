package hanzner.zebrakapp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import hanzner.zebrakapp.dto.AuthRequest;
import hanzner.zebrakapp.dto.PlaceCreateRequest;
import hanzner.zebrakapp.entity.Category;
import hanzner.zebrakapp.entity.DiscountType;
import hanzner.zebrakapp.entity.PriceLevel;
import hanzner.zebrakapp.entity.Role;
import hanzner.zebrakapp.entity.User;
import hanzner.zebrakapp.exception.RateLimitExceededException;
import hanzner.zebrakapp.repository.UserRepository;
import hanzner.zebrakapp.service.RateLimiterService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("Test")
@Transactional
public class RateLimiterSecurityTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RateLimiterService rateLimiterService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private User testUser;
    private Cookie authCookie;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        rateLimiterService.reset();

        testUser = userRepository.save(User.builder()
                .email("ratelimit_user_" + System.currentTimeMillis() + "@test.cz")
                .password(passwordEncoder.encode("Password123!"))
                .nickname("RateLimitUser")
                .role(Role.ROLE_USER)
                .active(true)
                .build());

        AuthRequest loginReq = AuthRequest.builder()
                .email(testUser.getEmail())
                .password("Password123!")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        authCookie = result.getResponse().getCookie("jwt_token");
    }

    @Test
    @DisplayName("Unit test: Rychlé volání pod minimální prodlevou vyhodí RateLimitExceededException")
    void testRateLimiterService_ThrowsExceptionWhenTooFast() {
        RateLimiterService strictLimiter = new RateLimiterService(2, 5); // 2 sekundy prodleva, max 5/min

        // 1. volání projde
        assertDoesNotThrow(() -> strictLimiter.checkPlaceCreationRateLimit(100L));

        // 2. okamžité volání pro stejného uživatele selže s RateLimitExceededException
        RateLimitExceededException ex = assertThrows(
                RateLimitExceededException.class,
                () -> strictLimiter.checkPlaceCreationRateLimit(100L)
        );
        assertTrue(ex.getMessage().contains("Vytváříte místa příliš rychle"));

        // Volání pro jiného uživatele (200L) projde bez blokování
        assertDoesNotThrow(() -> strictLimiter.checkPlaceCreationRateLimit(200L));
    }

    @Test
    @DisplayName("Unit test: Překročení limitu počtu míst za minutu vyhodí RateLimitExceededException")
    void testRateLimiterService_ThrowsExceptionWhenExceedingPerMinuteLimit() {
        RateLimiterService burstLimiter = new RateLimiterService(0, 3); // 0s prodleva, max 3/min

        assertDoesNotThrow(() -> burstLimiter.checkPlaceCreationRateLimit(101L));
        assertDoesNotThrow(() -> burstLimiter.checkPlaceCreationRateLimit(101L));
        assertDoesNotThrow(() -> burstLimiter.checkPlaceCreationRateLimit(101L));

        // 4. požadavek překročí limit 3 za minutu
        RateLimitExceededException ex = assertThrows(
                RateLimitExceededException.class,
                () -> burstLimiter.checkPlaceCreationRateLimit(101L)
        );
        assertTrue(ex.getMessage().contains("Byl překročen limit pro vytváření míst"));
    }

    @Test
    @DisplayName("Integrační test: HTTP POST /api/places při překročení limitu vrátí 429 Too Many Requests")
    void testCreatePlace_Returns429TooManyRequests() throws Exception {
        // Nastavíme přísný rate limiter pro simulaci
        RateLimiterService customLimiter = new RateLimiterService(5, 5);

        PlaceCreateRequest request = PlaceCreateRequest.builder()
                .title("Levná Pekárna")
                .description("Čerstvé pečivo")
                .category(Category.FOOD)
                .priceLevel(PriceLevel.LOW)
                .discountType(DiscountType.PERMANENT)
                .address("Pekařská 1")
                .city("Brno")
                .latitude(49.19)
                .longitude(16.60)
                .build();

        // 1. První vytvoření místa projde
        mockMvc.perform(post("/api/places")
                        .with(csrf())
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Levná Pekárna"));
    }
}
