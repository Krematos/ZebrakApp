package hanzner.zebrakapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import hanzner.zebrakapp.dto.*;
import hanzner.zebrakapp.entity.*;
import hanzner.zebrakapp.exception.PlaceNotFoundException;
import hanzner.zebrakapp.exception.UnauthorizedActionException;
import hanzner.zebrakapp.security.CustomUserDetails;
import hanzner.zebrakapp.service.PlaceService;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlaceController Unit Testy")
class PlaceControllerUnitTest {

    @Mock
    private PlaceService placeService;

    @InjectMocks
    private PlaceController placeController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private CustomUserDetails userPrincipal;
    private PlaceResponse samplePlaceResponse;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .id(1L)
                .email("user@example.cz")
                .nickname("Tester")
                .role(Role.ROLE_USER)
                .active(true)
                .build();
        userPrincipal = new CustomUserDetails(user);

        mockMvc = MockMvcBuilders
                .standaloneSetup(placeController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(@NonNull MethodParameter parameter) {
                        return parameter.getParameterType().equals(CustomUserDetails.class)
                                && parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(@NonNull MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  @NonNull NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return userPrincipal;
                    }
                })
                .build();

        samplePlaceResponse = PlaceResponse.builder()
                .id(10L)
                .title("Levné Potraviny")
                .description("Super sleva na potraviny")
                .category(Category.FOOD)
                .categoryLabel(Category.FOOD.getLabel())
                .priceLevel(PriceLevel.LOW)
                .priceLevelLabel(PriceLevel.LOW.getLabel())
                .discountType(DiscountType.PERMANENT)
                .discountTypeLabel(DiscountType.PERMANENT.getLabel())
                .address("Hlavní 10")
                .city("Brno")
                .postalCode("60200")
                .latitude(49.195)
                .longitude(16.607)
                .status(PlaceStatus.APPROVED)
                .votesActive(10)
                .votesClosed(1)
                .build();
    }

    @Nested
    @DisplayName("GET /api/places")
    class SearchPlacesEndpointTests {

        @Test
        @DisplayName("Vrátí stránkovaný seznam schválených míst a status 200 OK")
        void testSearchPlaces_Success() throws Exception {
            when(placeService.searchApprovedPlaces(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(PagedResponse.of(List.of(samplePlaceResponse), 0, 20, 1));

            mockMvc.perform(get("/api/places")
                            .param("category", "FOOD")
                            .param("priceLevel", "LOW")
                            .param("q", "Levné")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(10))
                    .andExpect(jsonPath("$.content[0].title").value("Levné Potraviny"))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20));
        }
    }

    @Nested
    @DisplayName("GET /api/places/{id}")
    class GetPlaceByIdEndpointTests {

        @Test
        @DisplayName("Vrátí detail místa a status 200 OK při existujícím ID")
        void testGetPlaceById_Success() throws Exception {
            when(placeService.getPlaceById(eq(10L), any(), any())).thenReturn(samplePlaceResponse);

            mockMvc.perform(get("/api/places/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.title").value("Levné Potraviny"));
        }

        @Test
        @DisplayName("Vrátí 404 NOT_FOUND při neexistujícím ID")
        void testGetPlaceById_NotFound() throws Exception {
            when(placeService.getPlaceById(eq(999L), any(), any()))
                    .thenThrow(new PlaceNotFoundException(999L));

            mockMvc.perform(get("/api/places/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("PLACE_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("POST /api/places")
    class CreatePlaceEndpointTests {

        @Test
        @DisplayName("Úspěšné vytvoření místa s validními daty")
        void testCreatePlace_Success() throws Exception {
            PlaceCreateRequest request = PlaceCreateRequest.builder()
                    .title("Nové Místo")
                    .description("Popis nového místa")
                    .category(Category.FOOD)
                    .priceLevel(PriceLevel.LOW)
                    .discountType(DiscountType.PERMANENT)
                    .address("Polní 1")
                    .city("Brno")
                    .latitude(49.2)
                    .longitude(16.6)
                    .build();

            when(placeService.createPlace(any(PlaceCreateRequest.class), any(User.class)))
                    .thenReturn(samplePlaceResponse);

            mockMvc.perform(post("/api/places")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10));
        }

        @Test
        @DisplayName("Vrátí 400 VALIDATION_ERROR při nevalidních datech (např. chybějící title nebo mimo rozsah GPS)")
        void testCreatePlace_ValidationError() throws Exception {
            PlaceCreateRequest invalidRequest = PlaceCreateRequest.builder()
                    .title("") // prázdný název
                    .latitude(150.0) // neplatná zeměpisná šířka (> 90)
                    .build();

            mockMvc.perform(post("/api/places")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.errors.title").isNotEmpty());
        }
    }

    @Nested
    @DisplayName("PUT /api/places/{id}")
    class UpdatePlaceEndpointTests {

        @Test
        @DisplayName("Úspěšná úprava místa")
        void testUpdatePlace_Success() throws Exception {
            PlaceUpdateRequest updateRequest = PlaceUpdateRequest.builder()
                    .title("Upravený Název")
                    .description("Upravený popis")
                    .category(Category.SECOND_HAND)
                    .priceLevel(PriceLevel.VERY_LOW)
                    .discountType(DiscountType.FLASH_SALES)
                    .address("Nová 15")
                    .city("Praha")
                    .latitude(50.0)
                    .longitude(14.0)
                    .build();

            when(placeService.updatePlace(eq(10L), any(PlaceUpdateRequest.class), any(User.class)))
                    .thenReturn(samplePlaceResponse);

            mockMvc.perform(put("/api/places/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10));
        }

        @Test
        @DisplayName("Vrátí 403 UNAUTHORIZED_ACCESS pokud uživatel není autorem ani adminem")
        void testUpdatePlace_Forbidden() throws Exception {
            PlaceUpdateRequest updateRequest = PlaceUpdateRequest.builder()
                    .title("Upravený Název")
                    .category(Category.SECOND_HAND)
                    .priceLevel(PriceLevel.VERY_LOW)
                    .discountType(DiscountType.FLASH_SALES)
                    .address("Nová 15")
                    .city("Praha")
                    .latitude(50.0)
                    .longitude(14.0)
                    .build();

            when(placeService.updatePlace(eq(10L), any(PlaceUpdateRequest.class), any(User.class)))
                    .thenThrow(new UnauthorizedActionException("Nemáte oprávnění upravovat toto místo."));

            mockMvc.perform(put("/api/places/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED_ACCESS"));
        }
    }

    @Nested
    @DisplayName("POST /api/places/{id}/images a DELETE /api/places/{placeId}/images/{imageId}")
    class ImageEndpointTests {

        @Test
        @DisplayName("Nahrání obrázků k místu vrátí 200 OK a seznam nahraných souborů")
        void testUploadImages_Success() throws Exception {
            MockMultipartFile file = new MockMultipartFile("files", "test.jpg", "image/jpeg", "image-content".getBytes());
            PlaceImageDto imageDto = PlaceImageDto.builder()
                    .id(100L)
                    .filename("uuid-test.jpg")
                    .url("/uploads/uuid-test.jpg")
                    .isPrimary(true)
                    .build();

            when(placeService.uploadImages(eq(10L), anyList(), any(User.class)))
                    .thenReturn(List.of(imageDto));

            mockMvc.perform(multipart("/api/places/10/images")
                            .file(file))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].filename").value("uuid-test.jpg"));
        }

        @Test
        @DisplayName("Smazání obrázku vrátí 204 NO_CONTENT")
        void testDeleteImage_Success() throws Exception {
            doNothing().when(placeService).deleteImage(eq(10L), eq(50L), any(User.class));

            mockMvc.perform(delete("/api/places/10/images/50"))
                    .andExpect(status().isNoContent());

            verify(placeService, times(1)).deleteImage(eq(10L), eq(50L), any(User.class));
        }
    }

    @Nested
    @DisplayName("POST /api/places/{id}/verify")
    class VerifyPlaceEndpointTests {

        @Test
        @DisplayName("Hlasování o platnosti nabídky vrátí 200 OK a VerificationResponse")
        void testVerifyPlace_Success() throws Exception {
            VerificationRequest request = new VerificationRequest(VoteType.STILL_OPEN);
            VerificationResponse verificationResponse = VerificationResponse.builder()
                    .placeId(10L)
                    .votesActive(11)
                    .votesClosed(1)
                    .userVote(VoteType.STILL_OPEN)
                    .message("Děkujeme za hlas!")
                    .build();

            when(placeService.verifyPlace(eq(10L), eq(VoteType.STILL_OPEN), any(), anyString()))
                    .thenReturn(verificationResponse);

            mockMvc.perform(post("/api/places/10/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.placeId").value(10))
                    .andExpect(jsonPath("$.userVote").value("STILL_OPEN"))
                    .andExpect(jsonPath("$.votesActive").value(11));
        }
    }
}
