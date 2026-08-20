package hanzner.zebrakapp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import hanzner.zebrakapp.dto.AdminPlaceActionRequest;
import hanzner.zebrakapp.dto.AuthRequest;
import hanzner.zebrakapp.dto.DeleteAccountRequest;
import hanzner.zebrakapp.dto.PlaceCreateRequest;
import hanzner.zebrakapp.dto.PlaceUpdateRequest;
import hanzner.zebrakapp.dto.RegisterRequest;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("Test")
@Transactional
public class DtoValidationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Cookie authCookie;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        User testUser = userRepository.save(User.builder()
                .email("validation_user_" + System.currentTimeMillis() + "@test.cz")
                .password(passwordEncoder.encode("Password123!"))
                .nickname("ValidationUser")
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
    @DisplayName("PlaceCreateRequest: Příliš dlouhý název (> 255 znaků) selže s chybou validace (400)")
    void testPlaceCreate_TitleTooLong_IsRejected() throws Exception {
        String longTitle = "A".repeat(256);

        PlaceCreateRequest request = PlaceCreateRequest.builder()
                .title(longTitle)
                .category(Category.FOOD)
                .priceLevel(PriceLevel.LOW)
                .discountType(DiscountType.PERMANENT)
                .address("Adresa 1")
                .city("Praha")
                .latitude(50.08)
                .longitude(14.42)
                .build();

        mockMvc.perform(post("/api/places")
                        .with(csrf())
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    @DisplayName("PlaceCreateRequest: Příliš krátký název (< 2 znaky) selže s chybou validace (400)")
    void testPlaceCreate_TitleTooShort_IsRejected() throws Exception {
        PlaceCreateRequest request = PlaceCreateRequest.builder()
                .title("A")
                .category(Category.FOOD)
                .priceLevel(PriceLevel.LOW)
                .discountType(DiscountType.PERMANENT)
                .address("Adresa 1")
                .city("Praha")
                .latitude(50.08)
                .longitude(14.42)
                .build();

        mockMvc.perform(post("/api/places")
                        .with(csrf())
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    @DisplayName("PlaceCreateRequest: Příliš dlouhý popis (> 5000 znaků) selže s chybou validace (400)")
    void testPlaceCreate_DescriptionTooLong_IsRejected() throws Exception {
        String longDesc = "D".repeat(5001);

        PlaceCreateRequest request = PlaceCreateRequest.builder()
                .title("Platný název")
                .description(longDesc)
                .category(Category.FOOD)
                .priceLevel(PriceLevel.LOW)
                .discountType(DiscountType.PERMANENT)
                .address("Adresa 1")
                .city("Praha")
                .latitude(50.08)
                .longitude(14.42)
                .build();

        mockMvc.perform(post("/api/places")
                        .with(csrf())
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.description").exists());
    }

    @Test
    @DisplayName("PlaceCreateRequest: Příliš dlouhé město (> 100 znaků) a PSČ (> 20 znaků) selžou")
    void testPlaceCreate_CityAndPostalCodeTooLong_IsRejected() throws Exception {
        PlaceCreateRequest request = PlaceCreateRequest.builder()
                .title("Platný název")
                .category(Category.FOOD)
                .priceLevel(PriceLevel.LOW)
                .discountType(DiscountType.PERMANENT)
                .address("Adresa 1")
                .city("C".repeat(101))
                .postalCode("123456789012345678901") // 21 znaků
                .latitude(50.08)
                .longitude(14.42)
                .build();

        mockMvc.perform(post("/api/places")
                        .with(csrf())
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.city").exists())
                .andExpect(jsonPath("$.errors.postalCode").exists());
    }

    @Test
    @DisplayName("RegisterRequest: Příliš krátké (< 6 znaků) nebo dlouhé heslo (> 100 znaků) selže")
    void testRegister_PasswordValidation_IsEnforced() throws Exception {
        RegisterRequest shortPassReq = RegisterRequest.builder()
                .email("new_user@test.cz")
                .nickname("Tester")
                .password("12345")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shortPassReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.password").exists());

        RegisterRequest longPassReq = RegisterRequest.builder()
                .email("new_user2@test.cz")
                .nickname("Tester")
                .password("P".repeat(101))
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(longPassReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    @DisplayName("PlaceCreateRequest: Neplatné souřadnice (lat mimo [-90, 90] nebo lng mimo [-180, 180]) selžou")
    void testPlaceCreate_InvalidCoordinates_IsRejected() throws Exception {
        PlaceCreateRequest request = PlaceCreateRequest.builder()
                .title("Platný název")
                .category(Category.FOOD)
                .priceLevel(PriceLevel.LOW)
                .discountType(DiscountType.PERMANENT)
                .address("Adresa 1")
                .city("Praha")
                .latitude(95.5) // Neplatná latitude (> 90)
                .longitude(195.0) // Neplatná longitude (> 180)
                .build();

        mockMvc.perform(post("/api/places")
                        .with(csrf())
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.latitude").exists())
                .andExpect(jsonPath("$.errors.longitude").exists());
    }

    @Test
    @DisplayName("DeleteAccountRequest: Příliš dlouhé heslo (> 100 znaků) selže s chybou validace (400)")
    void testDeleteAccount_PasswordTooLong_IsRejected() throws Exception {
        DeleteAccountRequest request = DeleteAccountRequest.builder()
                .password("P".repeat(101))
                .build();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/users/me")
                        .with(csrf())
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    @DisplayName("GET /api/places s neplatným enumem kategorie vrátí 400 Bad Request se seznamem povolených hodnot")
    void testSearchPlaces_InvalidCategoryEnum_ReturnsBadRequest() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/places")
                        .param("category", "NEEXISTUJICI_KATEGORIE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Neplatná hodnota 'NEEXISTUJICI_KATEGORIE' pro parametr 'category'")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("FOOD")));
    }

    @Test
    @DisplayName("GET /api/places s neplatným enumem priceLevel vrátí 400 Bad Request se seznamem povolených hodnot")
    void testSearchPlaces_InvalidPriceLevelEnum_ReturnsBadRequest() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/places")
                        .param("priceLevel", "DRAHE_JAKO_PES"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Neplatná hodnota 'DRAHE_JAKO_PES' pro parametr 'priceLevel'")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("LOW")));
    }

    @Test
    @DisplayName("POST /api/places s neplatným enumem v JSON body vrátí 400 Bad Request a detailní zprávu")
    void testCreatePlace_InvalidEnumInJsonBody_ReturnsBadRequest() throws Exception {
        String invalidJson = """
                {
                    "title": "Krásný obchod",
                    "category": "NEPLATNA_KATEGORIE",
                    "priceLevel": "LOW",
                    "discountType": "PERMANENT",
                    "address": "Hlavní 10",
                    "city": "Praha",
                    "latitude": 50.08,
                    "longitude": 14.42
                }
                """;

        mockMvc.perform(post("/api/places")
                        .with(csrf())
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Neplatná hodnota 'NEPLATNA_KATEGORIE' pro pole 'category'")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("FOOD")));
    }
}
