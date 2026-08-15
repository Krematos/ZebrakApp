package hanzner.zebrakapp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import hanzner.zebrakapp.dto.AuthRequest;
import hanzner.zebrakapp.dto.PlaceUpdateRequest;
import hanzner.zebrakapp.entity.*;
import hanzner.zebrakapp.repository.PlaceImageRepository;
import hanzner.zebrakapp.repository.PlaceRepository;
import hanzner.zebrakapp.repository.UserRepository;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("Test")
@Transactional
public class PlaceOwnershipSecurityTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private PlaceImageRepository imageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private User ownerUser;
    private User attackerUser;
    private User adminUser;

    private Cookie ownerCookie;
    private Cookie attackerCookie;
    private Cookie adminCookie;

    private Place ownerPlace;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        ownerUser = userRepository.save(User.builder()
                .email("owner_" + System.currentTimeMillis() + "@test.cz")
                .password(passwordEncoder.encode("Owner123!"))
                .nickname("OwnerUser")
                .role(Role.ROLE_USER)
                .active(true)
                .build());

        attackerUser = userRepository.save(User.builder()
                .email("attacker_" + System.currentTimeMillis() + "@test.cz")
                .password(passwordEncoder.encode("Attacker123!"))
                .nickname("AttackerUser")
                .role(Role.ROLE_USER)
                .active(true)
                .build());

        adminUser = userRepository.save(User.builder()
                .email("admin_" + System.currentTimeMillis() + "@test.cz")
                .password(passwordEncoder.encode("Admin123!"))
                .nickname("AdminUser")
                .role(Role.ROLE_ADMIN)
                .active(true)
                .build());

        ownerCookie = loginAndGetCookie(ownerUser.getEmail(), "Owner123!");
        attackerCookie = loginAndGetCookie(attackerUser.getEmail(), "Attacker123!");
        adminCookie = loginAndGetCookie(adminUser.getEmail(), "Admin123!");

        ownerPlace = placeRepository.save(Place.builder()
                .title("Majitelův Obchůdek")
                .description("Popis")
                .category(Category.FOOD)
                .priceLevel(PriceLevel.LOW)
                .discountType(DiscountType.PERMANENT)
                .address("Obchodní 10")
                .city("Plzeň")
                .latitude(49.74)
                .longitude(13.37)
                .status(PlaceStatus.APPROVED)
                .user(ownerUser)
                .build());
    }

    private Cookie loginAndGetCookie(String email, String password) throws Exception {
        AuthRequest req = AuthRequest.builder().email(email).password(password).build();
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        return res.getResponse().getCookie("jwt_token");
    }

    @Test
    @DisplayName("Útočník nemůže upravit cizí místo (IDOR ochrana) -> 403 Forbidden s UNAUTHORIZED_ACCESS")
    void testAttackerCannotUpdateAnotherUsersPlace() throws Exception {
        PlaceUpdateRequest updateReq = PlaceUpdateRequest.builder()
                .title("Pozměněný Název")
                .description("Hacked")
                .category(Category.FOOD)
                .priceLevel(PriceLevel.LOW)
                .discountType(DiscountType.PERMANENT)
                .address("Obchodní 10")
                .city("Plzeň")
                .latitude(49.74)
                .longitude(13.37)
                .build();

        mockMvc.perform(put("/api/places/" + ownerPlace.getId())
                        .with(csrf())
                        .cookie(attackerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED_ACCESS"));
    }

    @Test
    @DisplayName("Útočník nemůže nahrát obrázky k cizímu místu -> 403 Forbidden")
    void testAttackerCannotUploadImagesToAnotherUsersPlace() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files", "test.jpg", "image/jpeg", "dummy image content".getBytes()
        );

        mockMvc.perform(multipart("/api/places/" + ownerPlace.getId() + "/images")
                        .file(file)
                        .with(csrf())
                        .cookie(attackerCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED_ACCESS"));
    }

    @Test
    @DisplayName("Útočník nemůže smazat obrázek patřící k cizímu místu -> 403 Forbidden")
    void testAttackerCannotDeleteImageFromAnotherUsersPlace() throws Exception {
        PlaceImage image = imageRepository.save(PlaceImage.builder()
                .place(ownerPlace)
                .filename("test-image-1.jpg")
                .originalFilename("photo.jpg")
                .mimeType("image/jpeg")
                .fileSize(1024L)
                .isPrimary(true)
                .build());

        mockMvc.perform(delete("/api/places/" + ownerPlace.getId() + "/images/" + image.getId())
                        .with(csrf())
                        .cookie(attackerCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED_ACCESS"));
    }

    @Test
    @DisplayName("Neschválené místo (PENDING) vidí pouze jeho autor a administrátor, cizí uživatel dostane 400 PLACE_NOT_APPROVED")
    void testUnapprovedPlaceNotAccessibleByOthers() throws Exception {
        Place pendingPlace = placeRepository.save(Place.builder()
                .title("Čekající Místo")
                .category(Category.FOOD)
                .priceLevel(PriceLevel.LOW)
                .discountType(DiscountType.PERMANENT)
                .address("Ulice 2")
                .city("Praha")
                .latitude(50.08)
                .longitude(14.42)
                .status(PlaceStatus.PENDING)
                .user(ownerUser)
                .build());

        // 1. Autor ho vidí (200 OK)
        mockMvc.perform(get("/api/places/" + pendingPlace.getId()).cookie(ownerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Čekající Místo"));

        // 2. Admin ho vidí (200 OK)
        mockMvc.perform(get("/api/places/" + pendingPlace.getId()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Čekající Místo"));

        // 3. Cizí uživatel dostane chybu (PLACE_NOT_APPROVED)
        mockMvc.perform(get("/api/places/" + pendingPlace.getId()).cookie(attackerCookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("PLACE_NOT_APPROVED"));
    }
}
