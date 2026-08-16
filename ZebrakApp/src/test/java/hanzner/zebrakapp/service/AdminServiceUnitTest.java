package hanzner.zebrakapp.service;

import hanzner.zebrakapp.dto.PlaceResponse;
import hanzner.zebrakapp.entity.Category;
import hanzner.zebrakapp.entity.Place;
import hanzner.zebrakapp.entity.PlaceImage;
import hanzner.zebrakapp.entity.PlaceStatus;
import hanzner.zebrakapp.exception.PlaceNotFoundException;
import hanzner.zebrakapp.repository.PlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService Unit Testy")
class AdminServiceUnitTest {

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private PlaceService placeService;

    @Mock
    private ImageStorageService imageStorageService;

    @InjectMocks
    private AdminService adminService;

    private Place pendingPlace;
    private Place approvedPlace;
    private PlaceResponse mockResponse;

    @BeforeEach
    void setUp() {
        pendingPlace = Place.builder()
                .id(1L)
                .title("Testovací PENDING místo")
                .status(PlaceStatus.PENDING)
                .category(Category.FOOD)
                .images(new ArrayList<>())
                .build();

        approvedPlace = Place.builder()
                .id(2L)
                .title("Testovací APPROVED místo")
                .status(PlaceStatus.APPROVED)
                .category(Category.FOOD)
                .images(new ArrayList<>())
                .build();

        mockResponse = PlaceResponse.builder()
                .id(1L)
                .title("Testovací místo")
                .status(PlaceStatus.PENDING)
                .build();
    }

    @Nested
    @DisplayName("Testy pro getPendingPlaces()")
    class GetPendingPlacesTests {

        @Test
        @DisplayName("Vrátí seznam čekajících míst namapovaný na PlaceResponse")
        void testGetPendingPlaces_ReturnsMappedList() {
            when(placeRepository.findByStatusOrderByCreatedAtDesc(PlaceStatus.PENDING))
                    .thenReturn(List.of(pendingPlace));
            when(placeService.mapToPlaceResponse(eq(pendingPlace), isNull(), isNull()))
                    .thenReturn(mockResponse);

            List<PlaceResponse> result = adminService.getPendingPlaces();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(mockResponse.getId(), result.get(0).getId());
            verify(placeRepository, times(1)).findByStatusOrderByCreatedAtDesc(PlaceStatus.PENDING);
            verify(placeService, times(1)).mapToPlaceResponse(eq(pendingPlace), isNull(), isNull());
        }

        @Test
        @DisplayName("Vrátí prázdný seznam, pokud žádná čekající místa neexistují")
        void testGetPendingPlaces_EmptyList() {
            when(placeRepository.findByStatusOrderByCreatedAtDesc(PlaceStatus.PENDING))
                    .thenReturn(List.of());

            List<PlaceResponse> result = adminService.getPendingPlaces();

            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(placeService, never()).mapToPlaceResponse(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Testy pro getAllPlaces()")
    class GetAllPlacesTests {

        @Test
        @DisplayName("Vrátí filtrovaná místa podle předaného statusu")
        void testGetAllPlaces_WithStatus_ReturnsFiltered() {
            when(placeRepository.findByStatusOrderByCreatedAtDesc(PlaceStatus.APPROVED))
                    .thenReturn(List.of(approvedPlace));
            when(placeService.mapToPlaceResponse(eq(approvedPlace), isNull(), isNull()))
                    .thenReturn(mockResponse);

            List<PlaceResponse> result = adminService.getAllPlaces(PlaceStatus.APPROVED);

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(placeRepository, times(1)).findByStatusOrderByCreatedAtDesc(PlaceStatus.APPROVED);
            verify(placeRepository, never()).findAll();
        }

        @Test
        @DisplayName("Vrátí všechna místa bez ohledu na stav, pokud je status null")
        void testGetAllPlaces_NullStatus_ReturnsAll() {
            when(placeRepository.findAll()).thenReturn(List.of(pendingPlace, approvedPlace));
            when(placeService.mapToPlaceResponse(any(Place.class), isNull(), isNull()))
                    .thenReturn(mockResponse);

            List<PlaceResponse> result = adminService.getAllPlaces(null);

            assertNotNull(result);
            assertEquals(2, result.size());
            verify(placeRepository, times(1)).findAll();
            verify(placeRepository, never()).findByStatusOrderByCreatedAtDesc(any());
        }
    }

    @Nested
    @DisplayName("Testy pro approvePlace()")
    class ApprovePlaceTests {

        @Test
        @DisplayName("Úspěšně schválí místo a vyčistí důvod zamítnutí")
        void testApprovePlace_Success() {
            pendingPlace.setRejectionReason("Předchozí důvod");
            when(placeRepository.findById(1L)).thenReturn(Optional.of(pendingPlace));
            when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(placeService.mapToPlaceResponse(any(Place.class), isNull(), isNull())).thenReturn(mockResponse);

            PlaceResponse result = adminService.approvePlace(1L);

            assertNotNull(result);
            assertEquals(PlaceStatus.APPROVED, pendingPlace.getStatus());
            assertNull(pendingPlace.getRejectionReason());
            verify(placeRepository, times(1)).save(pendingPlace);
        }

        @Test
        @DisplayName("Vyhodí PlaceNotFoundException, pokud místo neexistuje")
        void testApprovePlace_NotFound_ThrowsException() {
            when(placeRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(PlaceNotFoundException.class, () -> adminService.approvePlace(999L));
            verify(placeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Testy pro rejectPlace()")
    class RejectPlaceTests {

        @Test
        @DisplayName("Úspěšně zamítne místo a nastaví důvod zamítnutí")
        void testRejectPlace_Success() {
            when(placeRepository.findById(1L)).thenReturn(Optional.of(pendingPlace));
            when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(placeService.mapToPlaceResponse(any(Place.class), isNull(), isNull())).thenReturn(mockResponse);

            PlaceResponse result = adminService.rejectPlace(1L, "Neplatná adresa");

            assertNotNull(result);
            assertEquals(PlaceStatus.REJECTED, pendingPlace.getStatus());
            assertEquals("Neplatná adresa", pendingPlace.getRejectionReason());
            verify(placeRepository, times(1)).save(pendingPlace);
        }

        @Test
        @DisplayName("Vyhodí PlaceNotFoundException při zamítnutí neexistujícího místa")
        void testRejectPlace_NotFound_ThrowsException() {
            when(placeRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(PlaceNotFoundException.class, () -> adminService.rejectPlace(999L, "Důvod"));
            verify(placeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Testy pro deletePlace()")
    class DeletePlaceTests {

        @Test
        @DisplayName("Úspěšně smaže místo včetně fyzických souborů všech přiřazených fotografií")
        void testDeletePlace_WithImages_DeletesFilesAndPlace() {
            PlaceImage img1 = PlaceImage.builder().id(10L).filename("img1.jpg").build();
            PlaceImage img2 = PlaceImage.builder().id(11L).filename("img2.png").build();
            pendingPlace.setImages(List.of(img1, img2));

            when(placeRepository.findById(1L)).thenReturn(Optional.of(pendingPlace));

            adminService.deletePlace(1L);

            verify(imageStorageService, times(1)).delete("img1.jpg");
            verify(imageStorageService, times(1)).delete("img2.png");
            verify(placeRepository, times(1)).delete(pendingPlace);
        }

        @Test
        @DisplayName("Úspěšně smaže místo s null kolekcí fotografií")
        void testDeletePlace_NullImages_DeletesPlace() {
            pendingPlace.setImages(null);
            when(placeRepository.findById(1L)).thenReturn(Optional.of(pendingPlace));

            adminService.deletePlace(1L);

            verify(imageStorageService, never()).delete(any());
            verify(placeRepository, times(1)).delete(pendingPlace);
        }

        @Test
        @DisplayName("Vyhodí PlaceNotFoundException při pokusu smazat neexistující místo")
        void testDeletePlace_NotFound_ThrowsException() {
            when(placeRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(PlaceNotFoundException.class, () -> adminService.deletePlace(999L));
            verify(placeRepository, never()).delete(any(Place.class));
        }
    }
}
