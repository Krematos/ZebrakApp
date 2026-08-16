package hanzner.zebrakapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hanzner.zebrakapp.dto.AdminPlaceActionRequest;
import hanzner.zebrakapp.dto.PlaceResponse;
import hanzner.zebrakapp.entity.Category;
import hanzner.zebrakapp.entity.PlaceStatus;
import hanzner.zebrakapp.exception.PlaceNotFoundException;
import hanzner.zebrakapp.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminController Unit Testy")
class AdminControllerUnitTest {

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminController adminController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private PlaceResponse samplePlaceResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(adminController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        samplePlaceResponse = PlaceResponse.builder()
                .id(1L)
                .title("Čekající restaurace")
                .status(PlaceStatus.PENDING)
                .category(Category.FOOD)
                .build();
    }

    @Nested
    @DisplayName("GET /api/admin/places/pending")
    class GetPendingPlacesTests {

        @Test
        @DisplayName("Vrátí seznam čekajících míst s kódem 200 OK")
        void testGetPendingPlaces_ReturnsList() throws Exception {
            when(adminService.getPendingPlaces()).thenReturn(List.of(samplePlaceResponse));

            mockMvc.perform(get("/api/admin/places/pending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].status").value("PENDING"));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/places")
    class GetAllPlacesTests {

        @Test
        @DisplayName("Vrátí filtrovaná místa podle statusu")
        void testGetAllPlaces_WithStatus_ReturnsFiltered() throws Exception {
            when(adminService.getAllPlaces(eq(PlaceStatus.APPROVED))).thenReturn(List.of(samplePlaceResponse));

            mockMvc.perform(get("/api/admin/places")
                            .param("status", "APPROVED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("Vrátí všechna místa bez specifikace statusu")
        void testGetAllPlaces_WithoutStatus_ReturnsAll() throws Exception {
            when(adminService.getAllPlaces(null)).thenReturn(List.of(samplePlaceResponse));

            mockMvc.perform(get("/api/admin/places"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("POST /api/admin/places/{id}/approve")
    class ApprovePlaceTests {

        @Test
        @DisplayName("Schválení místa vrátí 200 OK a schválené místo")
        void testApprovePlace_Success() throws Exception {
            samplePlaceResponse.setStatus(PlaceStatus.APPROVED);
            when(adminService.approvePlace(1L)).thenReturn(samplePlaceResponse);

            mockMvc.perform(post("/api/admin/places/1/approve"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.status").value("APPROVED"));
        }

        @Test
        @DisplayName("Schválení neexistujícího místa vrátí 404 NOT_FOUND")
        void testApprovePlace_NotFound() throws Exception {
            when(adminService.approvePlace(999L)).thenThrow(new PlaceNotFoundException(999L));

            mockMvc.perform(post("/api/admin/places/999/approve"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("PLACE_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("POST /api/admin/places/{id}/reject")
    class RejectPlaceTests {

        @Test
        @DisplayName("Zamítnutí místa s důvodem vrátí 200 OK")
        void testRejectPlace_WithReason_Success() throws Exception {
            AdminPlaceActionRequest request = new AdminPlaceActionRequest("Neplatné souřadnice");
            samplePlaceResponse.setStatus(PlaceStatus.REJECTED);
            samplePlaceResponse.setRejectionReason("Neplatné souřadnice");

            when(adminService.rejectPlace(1L, "Neplatné souřadnice")).thenReturn(samplePlaceResponse);

            mockMvc.perform(post("/api/admin/places/1/reject")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"))
                    .andExpect(jsonPath("$.rejectionReason").value("Neplatné souřadnice"));
        }

        @Test
        @DisplayName("Zamítnutí místa bez body použije výchozí důvod 'Nespecifikováno'")
        void testRejectPlace_WithoutBody_UsesDefaultReason() throws Exception {
            samplePlaceResponse.setStatus(PlaceStatus.REJECTED);
            samplePlaceResponse.setRejectionReason("Nespecifikováno");

            when(adminService.rejectPlace(1L, "Nespecifikováno")).thenReturn(samplePlaceResponse);

            mockMvc.perform(post("/api/admin/places/1/reject"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/admin/places/{id}")
    class DeletePlaceTests {

        @Test
        @DisplayName("Smazání místa vrátí 204 NO_CONTENT")
        void testDeletePlace_Success() throws Exception {
            doNothing().when(adminService).deletePlace(1L);

            mockMvc.perform(delete("/api/admin/places/1"))
                    .andExpect(status().isNoContent());

            verify(adminService, times(1)).deletePlace(1L);
        }

        @Test
        @DisplayName("Smazání neexistujícího místa vrátí 404 NOT_FOUND")
        void testDeletePlace_NotFound() throws Exception {
            doThrow(new PlaceNotFoundException(999L)).when(adminService).deletePlace(999L);

            mockMvc.perform(delete("/api/admin/places/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("PLACE_NOT_FOUND"));
        }
    }
}
