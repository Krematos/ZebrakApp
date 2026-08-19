package hanzner.zebrakapp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import hanzner.zebrakapp.dto.AuthRequest;
import hanzner.zebrakapp.dto.PlaceCreateRequest;
import hanzner.zebrakapp.dto.PlaceResponse;
import hanzner.zebrakapp.entity.Category;
import hanzner.zebrakapp.entity.DiscountType;
import hanzner.zebrakapp.entity.PriceLevel;
import hanzner.zebrakapp.entity.Role;
import hanzner.zebrakapp.entity.User;
import hanzner.zebrakapp.repository.PlaceRepository;
import hanzner.zebrakapp.repository.UserRepository;
import hanzner.zebrakapp.service.ImageStorageService;
import hanzner.zebrakapp.service.PlaceService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import hanzner.zebrakapp.exception.FileStorageException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("Test")
@Transactional
public class InputSanitizationSecurityTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private PlaceService placeService;

    @Autowired
    private ImageStorageService imageStorageService;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

        testUser = userRepository.save(User.builder()
                .email("sanitization_user_" + System.currentTimeMillis() + "@test.cz")
                .password(passwordEncoder.encode("Password123!"))
                .nickname("SanitizationUser")
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
    @DisplayName("Pokus o nahrání nepovoleného typu souboru (.exe, .html, .sh) je odmítnut (400 INVALID_IMAGE_FILE)")
    void testDisallowedFileExtension_IsRejected() throws Exception {
        // Nejprve vytvoříme místo
        PlaceCreateRequest createReq = PlaceCreateRequest.builder()
                .title("Místo pro test uploadu")
                .category(Category.FOOD)
                .priceLevel(PriceLevel.LOW)
                .discountType(DiscountType.PERMANENT)
                .address("Adresa 1")
                .city("Praha")
                .latitude(50.08)
                .longitude(14.42)
                .build();

        PlaceResponse createdPlace = placeService.createPlace(createReq, testUser);

        // Pokus o nahrání škodlivého HTML / skriptu místo obrázku
        MockMultipartFile maliciousFile = new MockMultipartFile(
                "files", "malicious_script.html", "text/html", "<script>alert('XSS')</script>".getBytes()
        );

        mockMvc.perform(multipart("/api/places/" + createdPlace.getId() + "/images")
                        .file(maliciousFile)
                        .with(csrf())
                        .cookie(authCookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_IMAGE_FILE"));
    }

    @Test
    @DisplayName("Pokus o Path Traversal v názvu souboru je bezpečně zneškodněn a soubor uložen s UUID")
    void testPathTraversalInFilename_IsNeutralized() {
        MockMultipartFile pathTraversalFile = new MockMultipartFile(
                "file", "../../../../etc/cron.d/malicious.jpg", "image/jpeg", "dummy-jpg-bytes".getBytes()
        );

        String storedFilename = imageStorageService.store(pathTraversalFile);

        // Ověříme, že výsledné jméno souboru neobsahuje "../" a bylo uloženo jako bezpečné UUID s příponou
        assertThat(storedFilename).doesNotContain("..");
        assertThat(storedFilename).endsWith(".jpg");
    }

    @Test
    @DisplayName("SQL Injection a XSS řetězce v názvu místa a vyhledávání jsou bezpečně zpracovány bez chyb")
    void testSqlInjectionAndXssInSearch_IsSafe() throws Exception {
        String sqlInjectionPayload = "'; DROP TABLE places; --";
        String xssPayload = "<script>alert('XSS')</script>";

        PlaceCreateRequest request = PlaceCreateRequest.builder()
                .title("Bezpečný test " + xssPayload)
                .description("Popis s " + sqlInjectionPayload)
                .category(Category.OTHER)
                .priceLevel(PriceLevel.LOW)
                .discountType(DiscountType.PERMANENT)
                .address("Ulice 1")
                .city("Praha")
                .latitude(50.08)
                .longitude(14.42)
                .build();

        // 1. Uložení místa s těmito řetězci
        mockMvc.perform(post("/api/places")
                        .with(csrf())
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Bezpečný test " + xssPayload));

        // 2. Vyhledávání s SQL injection dotazem
        mockMvc.perform(get("/api/places")
                        .param("q", "' OR 1=1 --"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // 3. Ověření, že databáze v H2 je v pořádku a tabulky nebyly poškozeny
        List<?> places = placeRepository.findAll();
        assertThat(places).isNotEmpty();
    }

    @Test
    @DisplayName("Pokus o Path Traversal v load() vyhodí FileStorageException")
    void testPathTraversalInLoad_ThrowsFileStorageException() {
        assertThatThrownBy(() -> imageStorageService.load("../../../../etc/passwd"))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("path traversal");

        assertThatThrownBy(() -> imageStorageService.load("..\\..\\secret.txt"))
                .isInstanceOf(FileStorageException.class);

        assertThatThrownBy(() -> imageStorageService.load(""))
                .isInstanceOf(FileStorageException.class);
    }

    @Test
    @DisplayName("Pokus o Path Traversal v delete() vyhodí FileStorageException a zabrání smazání")
    void testPathTraversalInDelete_ThrowsFileStorageException() {
        assertThatThrownBy(() -> imageStorageService.delete("../../../../etc/passwd"))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("path traversal");

        assertThatThrownBy(() -> imageStorageService.delete("..\\..\\secret.txt"))
                .isInstanceOf(FileStorageException.class);
    }
}
