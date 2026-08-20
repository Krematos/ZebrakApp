package hanzner.zebrakapp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import hanzner.zebrakapp.dto.AuthRequest;
import hanzner.zebrakapp.dto.PlaceCreateRequest;
import hanzner.zebrakapp.entity.Category;
import hanzner.zebrakapp.entity.DiscountType;
import hanzner.zebrakapp.entity.PriceLevel;
import hanzner.zebrakapp.entity.Role;
import hanzner.zebrakapp.entity.User;
import hanzner.zebrakapp.repository.UserRepository;
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

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("Test")
@Transactional
public class CsrfSecurityTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private User testUser;
    private String userRawPassword;
    private Cookie authCookie;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        userRawPassword = "Pass_" + UUID.randomUUID();
        testUser = userRepository.save(User.builder()
                .email("csrf_user_" + UUID.randomUUID() + "@test.cz")
                .password(passwordEncoder.encode(userRawPassword))
                .nickname("CsrfUser_" + System.currentTimeMillis())
                .role(Role.ROLE_USER)
                .active(true)
                .build());

        AuthRequest loginReq = AuthRequest.builder()
                .email(testUser.getEmail())
                .password(userRawPassword)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        authCookie = result.getResponse().getCookie("jwt_token");
    }

    @Test
    @DisplayName("GET request vygeneruje XSRF-TOKEN cookie pro frontend")
    void testGetRequestGeneratesXsrfCookie() throws Exception {
        mockMvc.perform(get("/api/metadata/categories"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false));
    }

    @Test
    @DisplayName("Chráněný POST požadavek BEZ CSRF tokenu je odmítnut s kódem 403 Forbidden")
    void testMutatingRequestWithoutCsrf_IsForbidden() throws Exception {
        PlaceCreateRequest request = PlaceCreateRequest.builder()
                .title("Testovací Místo")
                .category(Category.FOOD)
                .priceLevel(PriceLevel.LOW)
                .discountType(DiscountType.PERMANENT)
                .address("Ulice 1")
                .city("Praha")
                .latitude(50.08)
                .longitude(14.42)
                .build();

        // Požadavek s platnou auth cookie, ale BEZ CSRF tokenu
        mockMvc.perform(post("/api/places")
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Chráněný POST požadavek S platným CSRF tokenem projde úspěšně (200 OK)")
    void testMutatingRequestWithCsrf_Succeeds() throws Exception {
        PlaceCreateRequest request = PlaceCreateRequest.builder()
                .title("Bezpečně Vytvořené Místo")
                .category(Category.FOOD)
                .priceLevel(PriceLevel.LOW)
                .discountType(DiscountType.PERMANENT)
                .address("Ulice 1")
                .city("Praha")
                .latitude(50.08)
                .longitude(14.42)
                .build();

        // Požadavek s platnou auth cookie A S CSRF tokenem
        mockMvc.perform(post("/api/places")
                        .with(csrf())
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Bezpečně Vytvořené Místo"));
    }

    @Test
    @DisplayName("Veřejné endpointy (login, register, hlasování) fungují i bez CSRF tokenu (výjimka pro veřejný přístup)")
    void testExemptedEndpointsWorkWithoutCsrf() throws Exception {
        AuthRequest loginReq = AuthRequest.builder()
                .email(testUser.getEmail())
                .password(userRawPassword)
                .build();

        // Login bez CSRF
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("jwt_token"));
    }
}
