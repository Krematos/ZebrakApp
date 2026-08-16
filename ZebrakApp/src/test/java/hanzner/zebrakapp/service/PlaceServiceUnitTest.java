package hanzner.zebrakapp.service;

import hanzner.zebrakapp.dto.*;
import hanzner.zebrakapp.entity.*;
import hanzner.zebrakapp.exception.*;
import hanzner.zebrakapp.repository.PlaceImageRepository;
import hanzner.zebrakapp.repository.PlaceRepository;
import hanzner.zebrakapp.repository.PlaceVerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlaceService Unit Testy")
class PlaceServiceUnitTest {

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private PlaceImageRepository imageRepository;

    @Mock
    private PlaceVerificationRepository verificationRepository;

    @Mock
    private ImageStorageService imageStorageService;

    @Mock
    private AuthService authService;

    @InjectMocks
    private PlaceService placeService;

    private User author;
    private User otherUser;
    private User adminUser;
    private Place existingPlace;

    @BeforeEach
    void setUp() {
        author = User.builder()
                .id(1L)
                .email("autor@test.cz")
                .nickname("Autor")
                .role(Role.ROLE_USER)
                .active(true)
                .build();

        otherUser = User.builder()
                .id(2L)
                .email("cizi@test.cz")
                .nickname("Cizi")
                .role(Role.ROLE_USER)
                .active(true)
                .build();

        adminUser = User.builder()
                .id(99L)
                .email("admin@test.cz")
                .nickname("Admin")
                .role(Role.ROLE_ADMIN)
                .active(true)
                .build();

        existingPlace = Place.builder()
                .id(10L)
                .title("Zlevněné Potraviny")
                .description("Velkosklad potravin za nízké ceny")
                .category(Category.FOOD)
                .priceLevel(PriceLevel.LOW)
                .discountType(DiscountType.PERMANENT)
                .address("Dlouhá 15")
                .city("Praha")
                .postalCode("11000")
                .latitude(50.087)
                .longitude(14.421)
                .openingHours("11-22")
                .status(PlaceStatus.APPROVED)
                .votesActive(5)
                .votesClosed(1)
                .user(author)
                .images(new ArrayList<>())
                .verifications(new ArrayList<>())
                .build();
    }

    @Nested
    @DisplayName("Testy pro createPlace()")
    class CreatePlaceTests {

        @Test
        @DisplayName("Vytvoření místa běžným uživatelem nastaví status PENDING a 1 aktivní hlas")
        void testCreatePlace_RegularUser_SetsPending() {
            PlaceCreateRequest request = PlaceCreateRequest.builder()
                    .title("  Outlet Móda  ")
                    .description("  Super outlet  ")
                    .category(Category.OUTLET)
                    .priceLevel(PriceLevel.VERY_LOW)
                    .discountType(DiscountType.FLASH_SALES)
                    .address("  Kratka 5  ")
                    .city("  Brno  ")
                    .postalCode("  60200  ")
                    .latitude(49.2)
                    .longitude(16.6)
                    .openingHours("  8-20  ")
                    .build();

            when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> {
                Place saved = invocation.getArgument(0);
                saved.setId(100L);
                return saved;
            });
            when(authService.mapToUserDto(any())).thenReturn(UserDto.builder().id(1L).nickname("Autor").build());

            PlaceResponse response = placeService.createPlace(request, author);

            assertNotNull(response);
            assertEquals(100L, response.getId());
            assertEquals(PlaceStatus.PENDING, response.getStatus());
            assertEquals("Outlet Móda", response.getTitle());
            assertEquals("Super outlet", response.getDescription());
            assertEquals("Kratka 5", response.getAddress());
            assertEquals("Brno", response.getCity());
            assertEquals("60200", response.getPostalCode());
            assertEquals("8-20", response.getOpeningHours());
            assertEquals(1, response.getVotesActive());
            assertEquals(0, response.getVotesClosed());
        }

        @Test
        @DisplayName("Vytvoření místa administrátorem nastaví status APPROVED")
        void testCreatePlace_AdminUser_SetsApproved() {
            PlaceCreateRequest request = PlaceCreateRequest.builder()
                    .title("Admin Místo")
                    .category(Category.PALLET_GOODS)
                    .priceLevel(PriceLevel.EXTREME)
                    .discountType(DiscountType.PERMANENT)
                    .address("Náměstí 1")
                    .city("Ostrava")
                    .latitude(49.8)
                    .longitude(18.2)
                    .build();

            when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> {
                Place saved = invocation.getArgument(0);
                saved.setId(101L);
                return saved;
            });
            when(authService.mapToUserDto(any())).thenReturn(UserDto.builder().id(99L).nickname("Admin").build());

            PlaceResponse response = placeService.createPlace(request, adminUser);

            assertNotNull(response);
            assertEquals(PlaceStatus.APPROVED, response.getStatus());
        }
    }

    @Nested
    @DisplayName("Testy pro updatePlace()")
    class UpdatePlaceTests {

        @Test
        @DisplayName("Úprava místa autorem proběhne úspěšně")
        void testUpdatePlace_Author_Success() {
            PlaceUpdateRequest updateRequest = PlaceUpdateRequest.builder()
                    .title("  Nový Název  ")
                    .description("  Nový popis  ")
                    .category(Category.SECOND_HAND)
                    .priceLevel(PriceLevel.VERY_LOW)
                    .discountType(DiscountType.PERMANENT)
                    .address("  Nová 1  ")
                    .city("  Plzeň  ")
                    .postalCode("  30100  ")
                    .latitude(49.74)
                    .longitude(13.37)
                    .openingHours("  10-23  ")
                    .build();

            when(placeRepository.findById(10L)).thenReturn(Optional.of(existingPlace));
            when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(authService.mapToUserDto(any())).thenReturn(UserDto.builder().id(1L).nickname("Autor").build());

            PlaceResponse response = placeService.updatePlace(10L, updateRequest, author);

            assertNotNull(response);
            assertEquals("Nový Název", existingPlace.getTitle());
            assertEquals("Nový popis", existingPlace.getDescription());
            assertEquals(Category.SECOND_HAND, existingPlace.getCategory());
            assertEquals("Plzeň", existingPlace.getCity());
        }

        @Test
        @DisplayName("Úprava místa administrátorem proběhne úspěšně")
        void testUpdatePlace_Admin_Success() {
            PlaceUpdateRequest updateRequest = PlaceUpdateRequest.builder()
                    .title("Upraveno Adminem")
                    .category(Category.OTHER)
                    .priceLevel(PriceLevel.LOW)
                    .discountType(DiscountType.FLASH_SALES)
                    .address("Centrální 10")
                    .city("Praha")
                    .latitude(50.0)
                    .longitude(14.0)
                    .build();

            when(placeRepository.findById(10L)).thenReturn(Optional.of(existingPlace));
            when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(authService.mapToUserDto(any())).thenReturn(UserDto.builder().id(1L).nickname("Autor").build());

            PlaceResponse response = placeService.updatePlace(10L, updateRequest, adminUser);

            assertNotNull(response);
            assertEquals("Upraveno Adminem", existingPlace.getTitle());
        }

        @Test
        @DisplayName("Úprava cizím uživatelem vyhodí UnauthorizedActionException")
        void testUpdatePlace_UnauthorizedUser_ThrowsException() {
            PlaceUpdateRequest updateRequest = PlaceUpdateRequest.builder()
                    .title("Hack")
                    .category(Category.FOOD)
                    .priceLevel(PriceLevel.LOW)
                    .discountType(DiscountType.PERMANENT)
                    .address("Adresa")
                    .city("Město")
                    .latitude(50.0)
                    .longitude(14.0)
                    .build();

            when(placeRepository.findById(10L)).thenReturn(Optional.of(existingPlace));

            assertThrows(UnauthorizedActionException.class,
                    () -> placeService.updatePlace(10L, updateRequest, otherUser));
            verify(placeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Úprava neexistujícího místa vyhodí PlaceNotFoundException")
        void testUpdatePlace_NotFound_ThrowsException() {
            PlaceUpdateRequest updateRequest = PlaceUpdateRequest.builder().build();
            when(placeRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(PlaceNotFoundException.class,
                    () -> placeService.updatePlace(999L, updateRequest, author));
        }
    }

    @Nested
    @DisplayName("Testy pro getPlaceById()")
    class GetPlaceByIdTests {

        @Test
        @DisplayName("Získání schváleného místa anonymním uživatelem")
        void testGetPlaceById_Approved_Anonymous_Success() {
            when(placeRepository.findById(10L)).thenReturn(Optional.of(existingPlace));
            when(authService.mapToUserDto(any())).thenReturn(UserDto.builder().id(1L).build());

            PlaceResponse response = placeService.getPlaceById(10L, null, "192.168.1.1");

            assertNotNull(response);
            assertEquals(10L, response.getId());
        }

        @Test
        @DisplayName("Získání neschváleného místa autorem je povoleno")
        void testGetPlaceById_Pending_ByAuthor_Success() {
            existingPlace.setStatus(PlaceStatus.PENDING);
            when(placeRepository.findById(10L)).thenReturn(Optional.of(existingPlace));
            when(authService.mapToUserDto(any())).thenReturn(UserDto.builder().id(1L).build());

            PlaceResponse response = placeService.getPlaceById(10L, author, null);

            assertNotNull(response);
            assertEquals(PlaceStatus.PENDING, response.getStatus());
        }

        @Test
        @DisplayName("Získání neschváleného místa administrátorem je povoleno")
        void testGetPlaceById_Pending_ByAdmin_Success() {
            existingPlace.setStatus(PlaceStatus.PENDING);
            when(placeRepository.findById(10L)).thenReturn(Optional.of(existingPlace));
            when(authService.mapToUserDto(any())).thenReturn(UserDto.builder().id(1L).build());

            PlaceResponse response = placeService.getPlaceById(10L, adminUser, null);

            assertNotNull(response);
            assertEquals(PlaceStatus.PENDING, response.getStatus());
        }

        @Test
        @DisplayName("Získání neschváleného místa cizím uživatelem vyhodí PlaceNotApprovedException")
        void testGetPlaceById_Pending_ByOtherUser_ThrowsException() {
            existingPlace.setStatus(PlaceStatus.PENDING);
            when(placeRepository.findById(10L)).thenReturn(Optional.of(existingPlace));

            assertThrows(PlaceNotApprovedException.class,
                    () -> placeService.getPlaceById(10L, otherUser, null));
        }

        @Test
        @DisplayName("Získání neexistujícího místa vyhodí PlaceNotFoundException")
        void testGetPlaceById_NotFound_ThrowsException() {
            when(placeRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(PlaceNotFoundException.class,
                    () -> placeService.getPlaceById(999L, null, null));
        }
    }

    @Nested
    @DisplayName("Testy pro searchApprovedPlaces() a getUserPlaces()")
    class SearchAndUserPlacesTests {

        @Test
        @SuppressWarnings("unchecked")
        @DisplayName("searchApprovedPlaces volá PlaceRepository se specifikací")
        void testSearchApprovedPlaces_CallsRepo() {
            when(placeRepository.findAll(any(Specification.class), any(Sort.class)))
                    .thenReturn(List.of(existingPlace));
            when(authService.mapToUserDto(any())).thenReturn(UserDto.builder().id(1L).build());

            List<PlaceResponse> results = placeService.searchApprovedPlaces(
                    Category.FOOD, PriceLevel.LOW, DiscountType.PERMANENT,
                    49.0, 51.0, 14.0, 16.0, "  potraviny  ", author, "127.0.0.1"
            );

            assertNotNull(results);
            assertEquals(1, results.size());
            verify(placeRepository, times(1)).findAll(any(Specification.class), any(Sort.class));
        }

        @Test
        @DisplayName("getUserPlaces vrátí všechna místa daného uživatele")
        void testGetUserPlaces_ReturnsList() {
            when(placeRepository.findByUserIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(existingPlace));
            when(authService.mapToUserDto(any())).thenReturn(UserDto.builder().id(1L).build());

            List<PlaceResponse> results = placeService.getUserPlaces(author);

            assertNotNull(results);
            assertEquals(1, results.size());
            verify(placeRepository, times(1)).findByUserIdOrderByCreatedAtDesc(1L);
        }
    }

    @Nested
    @DisplayName("Testy pro uploadImages() a deleteImage()")
    class ImageManagementTests {

        @Test
        @DisplayName("Nahrání fotografií: první obrázek je nastaven jako primární")
        void testUploadImages_FirstImageIsPrimary() {
            when(placeRepository.findById(10L)).thenReturn(Optional.of(existingPlace));
            when(imageStorageService.store(any())).thenReturn("saved-uuid.jpg");
            when(imageRepository.save(any(PlaceImage.class))).thenAnswer(invocation -> {
                PlaceImage img = invocation.getArgument(0);
                img.setId(201L);
                return img;
            });

            MockMultipartFile file1 = new MockMultipartFile("files", "menu.jpg", "image/jpeg", "content".getBytes());
            MockMultipartFile file2 = new MockMultipartFile("files", "place.png", "image/png", "content2".getBytes());
            MockMultipartFile emptyFile = new MockMultipartFile("files", "empty.jpg", "image/jpeg", new byte[0]);

            List<PlaceImageDto> result = placeService.uploadImages(10L, List.of(file1, file2, emptyFile), author);

            assertNotNull(result);
            assertEquals(2, result.size());
            assertTrue(result.get(0).isPrimary());
            assertFalse(result.get(1).isPrimary());
            verify(imageStorageService, times(2)).store(any());
        }

        @Test
        @DisplayName("Nahrání fotografií neoprávněným uživatelem vyhodí UnauthorizedActionException")
        void testUploadImages_Unauthorized_ThrowsException() {
            when(placeRepository.findById(10L)).thenReturn(Optional.of(existingPlace));
            MockMultipartFile file = new MockMultipartFile("files", "img.jpg", "image/jpeg", "data".getBytes());

            assertThrows(UnauthorizedActionException.class,
                    () -> placeService.uploadImages(10L, List.of(file), otherUser));
            verify(imageStorageService, never()).store(any());
        }

        @Test
        @DisplayName("Smazání fotografie autorem proběhne úspěšně")
        void testDeleteImage_Success() {
            PlaceImage img = PlaceImage.builder()
                    .id(50L)
                    .place(existingPlace)
                    .filename("foto1.jpg")
                    .build();

            when(placeRepository.findById(10L)).thenReturn(Optional.of(existingPlace));
            when(imageRepository.findById(50L)).thenReturn(Optional.of(img));

            placeService.deleteImage(10L, 50L, author);

            verify(imageStorageService, times(1)).delete("foto1.jpg");
            verify(imageRepository, times(1)).delete(img);
        }

        @Test
        @DisplayName("Smazání fotografie neoprávněným uživatelem vyhodí UnauthorizedActionException")
        void testDeleteImage_Unauthorized_ThrowsException() {
            when(placeRepository.findById(10L)).thenReturn(Optional.of(existingPlace));

            assertThrows(UnauthorizedActionException.class,
                    () -> placeService.deleteImage(10L, 50L, otherUser));
            verify(imageRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Smazání fotografie patřící k jinému místu vyhodí ImageDoesNotBelongToPlaceException")
        void testDeleteImage_WrongPlace_ThrowsException() {
            Place otherPlace = Place.builder().id(99L).build();
            PlaceImage img = PlaceImage.builder()
                    .id(50L)
                    .place(otherPlace)
                    .filename("foto1.jpg")
                    .build();

            when(placeRepository.findById(10L)).thenReturn(Optional.of(existingPlace));
            when(imageRepository.findById(50L)).thenReturn(Optional.of(img));

            assertThrows(ImageDoesNotBelongToPlaceException.class,
                    () -> placeService.deleteImage(10L, 50L, author));
            verify(imageRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Smazání neexistující fotografie vyhodí ImageNotFoundException")
        void testDeleteImage_ImageNotFound_ThrowsException() {
            when(placeRepository.findById(10L)).thenReturn(Optional.of(existingPlace));
            when(imageRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ImageNotFoundException.class,
                    () -> placeService.deleteImage(10L, 999L, author));
        }
    }

    @Nested
    @DisplayName("Testy pro verifyPlace()")
    class VerificationTests {

        @Test
        @DisplayName("Nový hlas STILL_OPEN od přihlášeného uživatele zvýší počet aktivních hlasů")
        void testVerifyPlace_NewVote_StillOpen_User() {
            when(placeRepository.findById(10L)).thenReturn(Optional.of(existingPlace));
            when(verificationRepository.findByPlaceIdAndUserId(10L, 2L)).thenReturn(Optional.empty());

            VerificationResponse res = placeService.verifyPlace(10L, VoteType.STILL_OPEN, otherUser, null);

            assertNotNull(res);
            assertEquals(6, existingPlace.getVotesActive());
            assertEquals(1, existingPlace.getVotesClosed());
            assertEquals(VoteType.STILL_OPEN, res.getUserVote());
            verify(verificationRepository, times(1)).save(any(PlaceVerification.class));
            verify(placeRepository, times(1)).save(existingPlace);
        }

        @Test
        @DisplayName("Nový hlas CLOSED od anonymního uživatele (IP) zvýší počet closed hlasů")
        void testVerifyPlace_NewVote_Closed_Ip() {
            when(placeRepository.findById(10L)).thenReturn(Optional.of(existingPlace));
            when(verificationRepository.findByPlaceIdAndIpAddress(10L, "10.0.0.1")).thenReturn(Optional.empty());

            VerificationResponse res = placeService.verifyPlace(10L, VoteType.CLOSED, null, "10.0.0.1");

            assertNotNull(res);
            assertEquals(5, existingPlace.getVotesActive());
            assertEquals(2, existingPlace.getVotesClosed());
            assertEquals(VoteType.CLOSED, res.getUserVote());
        }

        @Test
        @DisplayName("Opakovaný stejný hlas neprovádí žádné změny v počtech")
        void testVerifyPlace_SameVoteAgain() {
            PlaceVerification existingVote = PlaceVerification.builder()
                    .id(1L)
                    .place(existingPlace)
                    .user(otherUser)
                    .vote(VoteType.STILL_OPEN)
                    .build();

            when(placeRepository.findById(10L)).thenReturn(Optional.of(existingPlace));
            when(verificationRepository.findByPlaceIdAndUserId(10L, 2L)).thenReturn(Optional.of(existingVote));

            VerificationResponse res = placeService.verifyPlace(10L, VoteType.STILL_OPEN, otherUser, null);

            assertNotNull(res);
            assertEquals(5, existingPlace.getVotesActive());
            assertEquals(1, existingPlace.getVotesClosed());
            assertTrue(res.getMessage().contains("již byl dříve započítán"));
            verify(placeRepository, never()).save(existingPlace);
        }

        @Test
        @DisplayName("Změna hlasu z STILL_OPEN na CLOSED upraví počty hlasů")
        void testVerifyPlace_ChangeVote_FromStillOpenToClosed() {
            PlaceVerification existingVote = PlaceVerification.builder()
                    .id(1L)
                    .place(existingPlace)
                    .user(otherUser)
                    .vote(VoteType.STILL_OPEN)
                    .build();

            when(placeRepository.findById(10L)).thenReturn(Optional.of(existingPlace));
            when(verificationRepository.findByPlaceIdAndUserId(10L, 2L)).thenReturn(Optional.of(existingVote));

            VerificationResponse res = placeService.verifyPlace(10L, VoteType.CLOSED, otherUser, null);

            assertNotNull(res);
            assertEquals(4, existingPlace.getVotesActive());
            assertEquals(2, existingPlace.getVotesClosed());
            assertEquals(VoteType.CLOSED, res.getUserVote());
            assertEquals(VoteType.CLOSED, existingVote.getVote());
            verify(verificationRepository, times(1)).save(existingVote);
            verify(placeRepository, times(1)).save(existingPlace);
        }

        @Test
        @DisplayName("Změna hlasu z CLOSED na STILL_OPEN upraví počty hlasů")
        void testVerifyPlace_ChangeVote_FromClosedToStillOpen() {
            PlaceVerification existingVote = PlaceVerification.builder()
                    .id(1L)
                    .place(existingPlace)
                    .user(otherUser)
                    .vote(VoteType.CLOSED)
                    .build();

            when(placeRepository.findById(10L)).thenReturn(Optional.of(existingPlace));
            when(verificationRepository.findByPlaceIdAndUserId(10L, 2L)).thenReturn(Optional.of(existingVote));

            VerificationResponse res = placeService.verifyPlace(10L, VoteType.STILL_OPEN, otherUser, null);

            assertNotNull(res);
            assertEquals(6, existingPlace.getVotesActive());
            assertEquals(0, existingPlace.getVotesClosed());
            assertEquals(VoteType.STILL_OPEN, res.getUserVote());
            assertEquals(VoteType.STILL_OPEN, existingVote.getVote());
            verify(verificationRepository, times(1)).save(existingVote);
            verify(placeRepository, times(1)).save(existingPlace);
        }

        @Test
        @DisplayName("Hlasování na neexistujícím místě vyhodí PlaceNotFoundException")
        void testVerifyPlace_NotFound_ThrowsException() {
            when(placeRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(PlaceNotFoundException.class,
                    () -> placeService.verifyPlace(999L, VoteType.STILL_OPEN, author, null));
        }
    }
}
