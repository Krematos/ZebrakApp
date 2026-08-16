package hanzner.zebrakapp.repository;

import hanzner.zebrakapp.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("Test")
@Transactional
@DisplayName("PlaceSpecification Testy")
class PlaceSpecificationTest {

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Place place1;
    private Place place2;
    private Place place3;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .email("spec-user" + System.currentTimeMillis() + "@test.cz")
                .password("secret123")
                .nickname("SpecTester")
                .role(Role.ROLE_USER)
                .active(true)
                .build());

        place1 = placeRepository.save(Place.builder()
                .title("Levné Potraviny Bella")
                .description("Velkosklad potravin ve městě")
                .category(Category.FOOD)
                .priceLevel(PriceLevel.LOW)
                .discountType(DiscountType.PERMANENT)
                .address("Václavská 10")
                .city("Praha")
                .latitude(50.08)
                .longitude(14.43)
                .status(PlaceStatus.APPROVED)
                .votesActive(5)
                .votesClosed(0)
                .user(testUser)
                .build());

        place2 = placeRepository.save(Place.builder()
                .title("Second Hand U Kočky")
                .description("Výborné oblečení a móda se slevou")
                .category(Category.SECOND_HAND)
                .priceLevel(PriceLevel.VERY_LOW)
                .discountType(DiscountType.FLASH_SALES)
                .address("Česká 20")
                .city("Brno")
                .latitude(49.19)
                .longitude(16.61)
                .status(PlaceStatus.APPROVED)
                .votesActive(3)
                .votesClosed(0)
                .user(testUser)
                .build());

        place3 = placeRepository.save(Place.builder()
                .title("Čekající Paletový Prodej")
                .description("Vrácené balíky z e-shopů")
                .category(Category.PALLET_GOODS)
                .priceLevel(PriceLevel.EXTREME)
                .discountType(DiscountType.PERMANENT)
                .address("Slovenská 5")
                .city("Ostrava")
                .latitude(49.83)
                .longitude(18.28)
                .status(PlaceStatus.PENDING)
                .votesActive(1)
                .votesClosed(0)
                .user(testUser)
                .build());
    }

    @Nested
    @DisplayName("Filtrování podle stavu a číselníků")
    class StatusAndEnumFilterTests {

        @Test
        @DisplayName("Filtrování podle statusu APPROVED vrátí pouze schválená místa")
        void testFilterByStatus() {
            Specification<Place> spec = PlaceSpecification.filterPlaces(
                    PlaceStatus.APPROVED, null, null, null, null, null, null, null, null
            );

            List<Place> results = placeRepository.findAll(spec);

            assertTrue(results.stream().allMatch(p -> p.getStatus() == PlaceStatus.APPROVED));
            assertTrue(results.stream().anyMatch(p -> p.getId().equals(place1.getId())));
            assertTrue(results.stream().anyMatch(p -> p.getId().equals(place2.getId())));
            assertFalse(results.stream().anyMatch(p -> p.getId().equals(place3.getId())));
        }

        @Test
        @DisplayName("Filtrování podle kategorie SECOND_HAND vrátí pouze second handy")
        void testFilterByCategory() {
            Specification<Place> spec = PlaceSpecification.filterPlaces(
                    PlaceStatus.APPROVED, Category.SECOND_HAND, null, null, null, null, null, null, null
            );

            List<Place> results = placeRepository.findAll(spec);

            assertEquals(1, results.size());
            assertEquals("Second Hand U Kočky", results.get(0).getTitle());
        }

        @Test
        @DisplayName("Filtrování podle cenové hladiny LOW")
        void testFilterByPriceLevel() {
            Specification<Place> spec = PlaceSpecification.filterPlaces(
                    PlaceStatus.APPROVED, null, PriceLevel.LOW, null, null, null, null, null, null
            );

            List<Place> results = placeRepository.findAll(spec);

            assertEquals(1, results.size());
            assertEquals("Levné Potraviny Bella", results.get(0).getTitle());
        }

        @Test
        @DisplayName("Filtrování podle typu slevy FLASH_SALES")
        void testFilterByDiscountType() {
            Specification<Place> spec = PlaceSpecification.filterPlaces(
                    PlaceStatus.APPROVED, null, null, DiscountType.FLASH_SALES, null, null, null, null, null
            );

            List<Place> results = placeRepository.findAll(spec);

            assertEquals(1, results.size());
            assertEquals("Second Hand U Kočky", results.get(0).getTitle());
        }
    }

    @Nested
    @DisplayName("Filtrování podle GPS souřadnic (Bounding Box)")
    class BoundingBoxTests {

        @Test
        @DisplayName("Filtrování v GPS obdélníku pro Prahu nalezne Levné Potraviny Bella")
        void testFilterByBoundingBox_Prague() {
            Specification<Place> spec = PlaceSpecification.filterPlaces(
                    PlaceStatus.APPROVED, null, null, null,
                    49.9, 50.2, 14.2, 14.6, null
            );

            List<Place> results = placeRepository.findAll(spec);

            assertEquals(1, results.size());
            assertEquals("Levné Potraviny Bella", results.get(0).getTitle());
        }

        @Test
        @DisplayName("Filtrování v GPS obdélníku pro Brno nalezne Second Hand U Kočky")
        void testFilterByBoundingBox_Brno() {
            Specification<Place> spec = PlaceSpecification.filterPlaces(
                    PlaceStatus.APPROVED, null, null, null,
                    49.0, 49.5, 16.0, 17.0, null
            );

            List<Place> results = placeRepository.findAll(spec);

            assertEquals(1, results.size());
            assertEquals("Second Hand U Kočky", results.get(0).getTitle());
        }
    }

    @Nested
    @DisplayName("Fulltextové vyhledávání (Query)")
    class QuerySearchTests {

        @Test
        @DisplayName("Vyhledání podle názvu 'Bella'")
        void testQuery_MatchesTitle() {
            Specification<Place> spec = PlaceSpecification.filterPlaces(
                    PlaceStatus.APPROVED, null, null, null, null, null, null, null, "bella"
            );

            List<Place> results = placeRepository.findAll(spec);

            assertEquals(1, results.size());
            assertEquals("Levné Potraviny Bella", results.get(0).getTitle());
        }

        @Test
        @DisplayName("Vyhledání podle popisu 'oblečení'")
        void testQuery_MatchesDescription() {
            Specification<Place> spec = PlaceSpecification.filterPlaces(
                    PlaceStatus.APPROVED, null, null, null, null, null, null, null, "oblečení"
            );

            List<Place> results = placeRepository.findAll(spec);

            assertEquals(1, results.size());
            assertEquals("Second Hand U Kočky", results.get(0).getTitle());
        }

        @Test
        @DisplayName("Vyhledání podle města 'Praha'")
        void testQuery_MatchesCity() {
            Specification<Place> spec = PlaceSpecification.filterPlaces(
                    PlaceStatus.APPROVED, null, null, null, null, null, null, null, "Praha"
            );

            List<Place> results = placeRepository.findAll(spec);

            assertEquals(1, results.size());
            assertEquals("Levné Potraviny Bella", results.get(0).getTitle());
        }

        @Test
        @DisplayName("Vyhledání podle adresy 'Česká'")
        void testQuery_MatchesAddress() {
            Specification<Place> spec = PlaceSpecification.filterPlaces(
                    PlaceStatus.APPROVED, null, null, null, null, null, null, null, "Česká"
            );

            List<Place> results = placeRepository.findAll(spec);

            assertEquals(1, results.size());
            assertEquals("Second Hand U Kočky", results.get(0).getTitle());
        }
    }
}
