package hanzner.zebrakapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import hanzner.zebrakapp.dto.AuthRequest;
import hanzner.zebrakapp.dto.PlaceUpdateRequest;
import hanzner.zebrakapp.dto.RegisterRequest;
import hanzner.zebrakapp.entity.*;
import hanzner.zebrakapp.repository.PlaceRepository;
import hanzner.zebrakapp.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("Test")
@Transactional
public class SecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private User regularUser1;
    private User regularUser2;
    private User adminUser;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        regularUser1 = userRepository.save(User.builder()
                .email("user1_" + System.currentTimeMillis() + "@test.cz")
                .password(passwordEncoder.encode("password123"))
                .nickname("Uzivatel 1")
                .role(Role.ROLE_USER)
                .active(true)
                .build());

        regularUser2 = userRepository.save(User.builder()
                .email("user2_" + System.currentTimeMillis() + "@test.cz")
                .password(passwordEncoder.encode("password123"))
                .nickname("Uzivatel 2")
                .role(Role.ROLE_USER)
                .active(true)
                .build());

        adminUser = userRepository.save(User.builder()
                .email("admin_" + System.currentTimeMillis() + "@test.cz")
                .password(passwordEncoder.encode("admin123"))
                .nickname("Hlavni Admin")
                .role(Role.ROLE_ADMIN)
                .active(true)
                .build());
    }

    private Cookie loginAndGetCookie(String email, String password) throws Exception {
        AuthRequest loginReq = AuthRequest.builder()
                .email(email)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("jwt_token"))
                .andExpect(cookie().httpOnly("jwt_token", true))
                .andReturn();

        return result.getResponse().getCookie("jwt_token");
    }

    @Nested
    @DisplayName("1. Testy HttpOnly Cookie autentizace a CSRF")
    class HttpOnlyCookieTests {

        @Test
        @DisplayName("Registrace vystaví validní HttpOnly cookie a přihlásí uživatele")
        void testRegisterSetsHttpOnlyCookie() throws Exception {
            RegisterRequest req = RegisterRequest.builder()
                    .email("novy_" + System.currentTimeMillis() + "@test.cz")
                    .password("heslo123")
                    .nickname("NovyTester")
                    .build();

            MvcResult result = mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists("jwt_token"))
                    .andExpect(cookie().httpOnly("jwt_token", true))
                    .andExpect(cookie().path("jwt_token", "/"))
                    .andReturn();

            Cookie cookie = result.getResponse().getCookie("jwt_token");

            // Přístup k chráněnému endpointu pomocí cookie
            mockMvc.perform(get("/api/auth/me").cookie(cookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nickname").value("NovyTester"));
        }


        @Test
        @DisplayName("Neplatný nebo pozměněný JWT token v cookie vrátí 401 Unauthorized")
        void testTamperedCookieReturnsUnauthorized() throws Exception {
            Cookie fakeCookie = new Cookie("jwt_token", "invalid.jwt.token.here");

            mockMvc.perform(get("/api/auth/me").cookie(fakeCookie))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Odhlášení (logout) zneplatní a vymaže cookie (maxAge = 0)")
        void testLogoutClearsCookie() throws Exception {
            Cookie userCookie = loginAndGetCookie(regularUser1.getEmail(), "password123");

            // 1. Ověříme, že s cookie jsme přihlášeni
            mockMvc.perform(get("/api/auth/me").cookie(userCookie))
                    .andExpect(status().isOk());

            // 2. Provedeme logout
            mockMvc.perform(post("/api/auth/logout"))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists("jwt_token"))
                    .andExpect(cookie().maxAge("jwt_token", 0))
                    .andExpect(jsonPath("$.message").value("Úspěšně odhlášeno"));
        }

        @Test
        @DisplayName("Chybná přihlašovací data vrátí 401 s chybovou zprávou")
        void testInvalidCredentialsReturnUnauthorized() throws Exception {
            AuthRequest invalidReq = AuthRequest.builder()
                    .email(regularUser1.getEmail())
                    .password("spatneHeslo")
                    .build();

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidReq)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Nesprávný e-mail nebo heslo"));
        }
    }

    @Nested
    @DisplayName("2. Testy Role-Based Access Control (RBAC)")
    class RoleBasedAccessControlTests {

        @Test
        @DisplayName("Běžný uživatel (ROLE_USER) nemá přístup do admin sekce -> 403 Forbidden")
        void testRegularUserDeniedFromAdminEndpoints() throws Exception {
            Cookie userCookie = loginAndGetCookie(regularUser1.getEmail(), "password123");

            mockMvc.perform(get("/api/admin/places/pending").cookie(userCookie))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED_ACCESS"));
        }

        @Test
        @DisplayName("Nepřihlášený uživatel (Host) nemá přístup do admin sekce -> 401 Unauthorized")
        void testAnonymousDeniedFromAdminEndpoints() throws Exception {
            mockMvc.perform(get("/api/admin/places/pending"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Administrátor (ROLE_ADMIN) má přístup do admin sekce -> 200 OK")
        void testAdminAllowedToAdminEndpoints() throws Exception {
            Cookie adminCookie = loginAndGetCookie(adminUser.getEmail(), "admin123");

            mockMvc.perform(get("/api/admin/places/pending").cookie(adminCookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20));
        }

        @Test
        @DisplayName("Uživatel nemůže editovat místo vytvořené jiným uživatelem -> 403 Forbidden")
        void testUserCannotEditAnotherUsersPlace() throws Exception {
            // Vytvoříme místo patřící user1
            Place placeUser1 = placeRepository.save(Place.builder()
                    .title("Sekáč U Pepy")
                    .description("Popis")
                    .category(Category.SECOND_HAND)
                    .priceLevel(PriceLevel.LOW)
                    .discountType(DiscountType.PERMANENT)
                    .address("Hlavní 1")
                    .city("Brno")
                    .latitude(49.195)
                    .longitude(16.606)
                    .status(PlaceStatus.APPROVED)
                    .user(regularUser1)
                    .build());

            // Přihlásíme se jako user2
            Cookie user2Cookie = loginAndGetCookie(regularUser2.getEmail(), "password123");

            PlaceUpdateRequest updateReq = PlaceUpdateRequest.builder()
                    .title("Hacknutý název")
                    .description("Nový popis")
                    .category(Category.SECOND_HAND)
                    .priceLevel(PriceLevel.LOW)
                    .discountType(DiscountType.PERMANENT)
                    .address("Hlavní 1")
                    .city("Brno")
                    .latitude(49.195)
                    .longitude(16.606)
                    .build();

            mockMvc.perform(put("/api/places/" + placeUser1.getId())
                            .with(csrf())
                            .cookie(user2Cookie)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED_ACCESS"));
        }

        @Test
        @DisplayName("Admin může editovat libovolné místo i cizího uživatele -> 200 OK")
        void testAdminCanEditAnyPlace() throws Exception {
            Place placeUser1 = placeRepository.save(Place.builder()
                    .title("Sekáč U Pepy")
                    .description("Původní popis")
                    .category(Category.SECOND_HAND)
                    .priceLevel(PriceLevel.LOW)
                    .discountType(DiscountType.PERMANENT)
                    .address("Hlavní 1")
                    .city("Brno")
                    .latitude(49.195)
                    .longitude(16.606)
                    .status(PlaceStatus.APPROVED)
                    .user(regularUser1)
                    .build());

            Cookie adminCookie = loginAndGetCookie(adminUser.getEmail(), "admin123");

            PlaceUpdateRequest updateReq = PlaceUpdateRequest.builder()
                    .title("Sekáč U Pepy - Prověřeno")
                    .description("Upraveno administrátorem")
                    .category(Category.SECOND_HAND)
                    .priceLevel(PriceLevel.LOW)
                    .discountType(DiscountType.PERMANENT)
                    .address("Hlavní 1")
                    .city("Brno")
                    .latitude(49.195)
                    .longitude(16.606)
                    .build();

            mockMvc.perform(put("/api/places/" + placeUser1.getId())
                            .with(csrf())
                            .cookie(adminCookie)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Sekáč U Pepy - Prověřeno"));
        }
    }

    @Nested
    @DisplayName("3. Testy veřejně přístupných endpointů a Swagger dokumentace")
    class PublicEndpointsTests {

        @Test
        @DisplayName("Swagger OpenAPI specifikace a UI jsou veřejně dostupné bez autentizace")
        void testSwaggerEndpointsArePublic() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.openapi").exists());

            mockMvc.perform(get("/swagger-ui/index.html"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Veřejné vyhledávání míst je přístupné bez přihlášení")
        void testSearchPlacesIsPublic() throws Exception {
            mockMvc.perform(get("/api/places"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20));
        }

        @Test
        @DisplayName("Hlasování o ověření místa je přístupné i nepřihlášeným uživatelům")
        void testAnonymousVerificationVoting() throws Exception {
            Place place = placeRepository.save(Place.builder()
                    .title("Levné Ovocné Centrum")
                    .category(Category.FOOD)
                    .priceLevel(PriceLevel.VERY_LOW)
                    .discountType(DiscountType.PERMANENT)
                    .address("Polní 123")
                    .city("Brno")
                    .latitude(49.1951)
                    .longitude(16.6068)
                    .status(PlaceStatus.APPROVED)
                    .user(regularUser1)
                    .votesActive(1)
                    .votesClosed(0)
                    .build());

            mockMvc.perform(post("/api/places/" + place.getId() + "/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"vote\":\"STILL_OPEN\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userVote").value("STILL_OPEN"))
                    .andExpect(jsonPath("$.votesActive").value(2));
        }
    }

    @Nested
    @DisplayName("4. Testy odhlášení a zneplatnění JWT tokenu (Token Blacklist)")
    class TokenBlacklistTests {

        @Test
        @DisplayName("Po odhlášení je token zneplatněn na blacklistu a další požadavek s tímto tokenem selže (401)")
        void testTokenBlacklistAfterLogout() throws Exception {
            Cookie userCookie = loginAndGetCookie(regularUser1.getEmail(), "password123");

            // 1. Před odhlášením uživatel může přistupovat k /api/auth/me
            mockMvc.perform(get("/api/auth/me")
                            .cookie(userCookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(regularUser1.getEmail()));

            // 2. Provedeme odhlášení s daným tokenem
            mockMvc.perform(post("/api/auth/logout")
                            .cookie(userCookie)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(cookie().maxAge("jwt_token", 0));

            // 3. Pokus o opětovné použití původního (odhlášeného) tokenu musí být odmítnut (401)
            mockMvc.perform(get("/api/auth/me")
                            .cookie(userCookie))
                    .andExpect(status().isUnauthorized());
        }
    }
}
