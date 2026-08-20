package hanzner.zebrakapp.controller;

import hanzner.zebrakapp.dto.DeleteAccountRequest;
import hanzner.zebrakapp.dto.PagedResponse;
import hanzner.zebrakapp.dto.PlaceResponse;
import hanzner.zebrakapp.entity.Category;
import hanzner.zebrakapp.entity.PlaceStatus;
import hanzner.zebrakapp.entity.Role;
import hanzner.zebrakapp.entity.User;
import hanzner.zebrakapp.exception.InvalidPasswordException;
import hanzner.zebrakapp.security.CustomUserDetails;
import hanzner.zebrakapp.security.JwtTokenProvider;
import hanzner.zebrakapp.service.PlaceService;
import hanzner.zebrakapp.service.UserService;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Unit Testy")
class UserControllerUnitTest {

    @Mock
    private PlaceService placeService;

    @Mock
    private UserService userService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private CustomUserDetails userPrincipal;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .id(1L)
                .email("moje@konto.cz")
                .nickname("Majitel")
                .role(Role.ROLE_USER)
                .active(true)
                .build();
        userPrincipal = new CustomUserDetails(user);

        mockMvc = MockMvcBuilders
                .standaloneSetup(userController)
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
    }

    @Test
    @DisplayName("GET /api/users/my-places vrátí místa přihlášeného uživatele")
    void testGetMyPlaces_ReturnsUserPlaces() throws Exception {
        PlaceResponse myPlace = PlaceResponse.builder()
                .id(5L)
                .title("Moje oblíbené potraviny")
                .status(PlaceStatus.APPROVED)
                .category(Category.FOOD)
                .build();

        when(placeService.getUserPlaces(any(User.class), any())).thenReturn(PagedResponse.of(List.of(myPlace), 0, 20, 1));

        mockMvc.perform(get("/api/users/my-places"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(5))
                .andExpect(jsonPath("$.content[0].title").value("Moje oblíbené potraviny"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("DELETE /api/users/me úspěšně smaže účet (204 No Content)")
    void testDeleteMyAccount_Success() throws Exception {
        DeleteAccountRequest request = new DeleteAccountRequest("spravneHeslo123");
        when(jwtTokenProvider.createCleanJwtCookie()).thenReturn(
                ResponseCookie.from("jwt_token", "").maxAge(0).build()
        );

        mockMvc.perform(delete("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteMyAccount(any(User.class), eq(request), any());
    }

    @Test
    @DisplayName("DELETE /api/users/me se špatným heslem vrátí 400 Bad Request")
    void testDeleteMyAccount_WrongPassword_Returns400() throws Exception {
        DeleteAccountRequest request = new DeleteAccountRequest("spatneHeslo");
        doThrow(new InvalidPasswordException("Zadané heslo není správné."))
                .when(userService).deleteMyAccount(any(User.class), eq(request), any());

        mockMvc.perform(delete("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Zadané heslo není správné."));
    }

    // --- EDGE CASE TESTY ---

    @Test
    @DisplayName("EDGE-CASE: DELETE /api/users/me bez payloadu (body) vrátí 400 Bad Request")
    void testDeleteMyAccount_NoBody_Returns400() throws Exception {
        mockMvc.perform(delete("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("EDGE-CASE: DELETE /api/users/me s prázdným heslem vyhodí 400 z důvodu validace (NotBlank)")
    void testDeleteMyAccount_EmptyPassword_Returns400() throws Exception {
        DeleteAccountRequest request = new DeleteAccountRequest(""); // Prázdné heslo

        mockMvc.perform(delete("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
                // Validace @NotBlank (pokud ji máme v DTO) by to měla zastavit před voláním služby.
                // Kdyby náhodou prošla, chytne to UserService jak je testováno výše.
    }

    @Test
    @DisplayName("getMyPlaces() - neplatné parametry stránkování jsou ošetřeny normalizací na povolený rozsah (200 OK)")
    void testGetMyPlaces_InvalidPagination_HandledGracefully() throws Exception {
        PagedResponse<PlaceResponse> pagedResponse = PagedResponse.<PlaceResponse>builder()
                .content(java.util.List.of())
                .page(0)
                .size(20)
                .totalElements(0)
                .totalPages(0)
                .first(true)
                .last(true)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(placeService.getUserPlaces(any(User.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(pagedResponse);

        mockMvc.perform(get("/api/users/my-places")
                        .param("page", "-1")
                        .param("size", "-5"))
                .andExpect(status().isOk()); 
    }
}
