package hanzner.zebrakapp;

import hanzner.zebrakapp.dto.PagedResponse;
import hanzner.zebrakapp.dto.PlaceCreateRequest;
import hanzner.zebrakapp.dto.PlaceResponse;
import hanzner.zebrakapp.dto.VerificationResponse;
import hanzner.zebrakapp.entity.*;
import hanzner.zebrakapp.repository.UserRepository;
import hanzner.zebrakapp.service.AdminService;
import hanzner.zebrakapp.service.PlaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("Test")
@Transactional
class PlaceIntegrationTest {

    @Autowired
    private PlaceService placeService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserRepository userRepository;

    private User regularUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        regularUser = userRepository.save(User.builder()
                .email("user" + System.currentTimeMillis() + "@test.cz")
                .password("secret")
                .nickname("Pepa")
                .role(Role.ROLE_USER)
                .active(true)
                .build());

        adminUser = userRepository.save(User.builder()
                .email("admin" + System.currentTimeMillis() + "@test.cz")
                .password("secret")
                .nickname("Admin")
                .role(Role.ROLE_ADMIN)
                .active(true)
                .build());
    }

    @Test
    void testCreatePlaceAndAdminApprovalWorkflow() {
        PlaceCreateRequest request = PlaceCreateRequest.builder()
                .title("Levné Ovocné Centrum")
                .description("Palety s ovocem a zeleninou za zlomkové ceny")
                .category(Category.FOOD)
                .priceLevel(PriceLevel.VERY_LOW)
                .discountType(DiscountType.PERMANENT)
                .address("Polní 123")
                .city("Brno")
                .postalCode("60200")
                .latitude(49.1951)
                .longitude(16.6068)
                .openingHours("Po-Pá 8-18")
                .build();

        // 1. Uživatel vytvoří místo -> stav PENDING
        PlaceResponse created = placeService.createPlace(request, regularUser);
        assertNotNull(created.getId());
        assertEquals(PlaceStatus.PENDING, created.getStatus());

        // 2. Místo se zatím nezobrazí ve veřejném vyhledávání
        PagedResponse<PlaceResponse> publicPlaces = placeService.searchApprovedPlaces(
                Category.FOOD, null, null, null, null, null, null, "Ovocné", regularUser, null, null
        );
        assertTrue(publicPlaces.getContent().stream().noneMatch(p -> p.getId().equals(created.getId())));

        // 3. Admin vidí místo v čekárně (pending)
        PagedResponse<PlaceResponse> pendingPlaces = adminService.getPendingPlaces(null);
        assertTrue(pendingPlaces.getContent().stream().anyMatch(p -> p.getId().equals(created.getId())));

        // 4. Admin schválí místo
        PlaceResponse approved = adminService.approvePlace(created.getId());
        assertEquals(PlaceStatus.APPROVED, approved.getStatus());

        // 5. Nyní je místo viditelné ve veřejném vyhledávání
        PagedResponse<PlaceResponse> approvedPlaces = placeService.searchApprovedPlaces(
                Category.FOOD, null, null, null, null, null, null, "Ovocné", regularUser, null, null
        );
        assertTrue(approvedPlaces.getContent().stream().anyMatch(p -> p.getId().equals(created.getId())));

        // 6. Ověření hlasování (STILL_OPEN)
        VerificationResponse voteResponse = placeService.verifyPlace(created.getId(), VoteType.STILL_OPEN, regularUser, "127.0.0.1");
        assertNotNull(voteResponse);
        assertEquals(VoteType.STILL_OPEN, voteResponse.getUserVote());
    }
}
