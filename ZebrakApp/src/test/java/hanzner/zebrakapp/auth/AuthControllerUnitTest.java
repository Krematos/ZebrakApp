package hanzner.zebrakapp.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import hanzner.zebrakapp.controller.AuthController;
import hanzner.zebrakapp.controller.GlobalExceptionHandler;
import hanzner.zebrakapp.dto.AuthResponse;
import hanzner.zebrakapp.dto.RegisterRequest;
import hanzner.zebrakapp.dto.UserDto;
import hanzner.zebrakapp.entity.Role;
import hanzner.zebrakapp.exception.UserAlreadyExistException;
import hanzner.zebrakapp.security.JwtTokenProvider;
import hanzner.zebrakapp.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerUnitTest {

    @Mock
    private AuthService authService;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/auth/register vrátí 200 OK, Set-Cookie a AuthResponse při validních datech")
    void testRegisterEndpoint_Success() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("jan.novak@example.cz")
                .password("tajneHeslo123")
                .nickname("JanNovak")
                .build();

        UserDto userDto = UserDto.builder()
                .id(1L)
                .email("jan.novak@example.cz")
                .nickname("JanNovak")
                .role(Role.ROLE_USER)
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .token("jwt.token.value")
                .tokenType("Bearer")
                .user(userDto)
                .build();

        ResponseCookie mockCookie = ResponseCookie.from("jwt_token", "jwt.token.value")
                .httpOnly(true)
                .path("/")
                .maxAge(86400)
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);
        when(tokenProvider.createJwtCookie("jwt.token.value")).thenReturn(mockCookie);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.token").value("jwt.token.value"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("jan.novak@example.cz"))
                .andExpect(jsonPath("$.user.nickname").value("JanNovak"));
    }

    @Test
    @DisplayName("POST /api/auth/register vrátí 409 Conflict s ErrorCode.USER_ALREADY_EXISTS při duplicitním e-mailu")
    void testRegisterEndpoint_DuplicateEmail_Returns409() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("duplicitni@example.cz")
                .password("tajneHeslo123")
                .nickname("Duplicita")
                .build();

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new UserAlreadyExistException("Uživatel s tímto e-mailem již existuje"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("USER_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Uživatel s tímto e-mailem již existuje"));
    }

    @Nested
    @DisplayName("Validace vstupních dat při registraci")
    class ValidationTests {

        @Test
        @DisplayName("Neplatný formát e-mailu vrátí 400 Bad Request a VALIDATION_ERROR")
        void testRegister_InvalidEmail_ReturnsBadRequest() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .email("neplatny-email")
                    .password("heslo123")
                    .nickname("Tester")
                    .build();

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.errors.email").isNotEmpty());
        }

        @Test
        @DisplayName("Krátké heslo (< 6 znaků) vrátí 400 Bad Request")
        void testRegister_ShortPassword_ReturnsBadRequest() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .email("jan@example.cz")
                    .password("12345")
                    .nickname("Tester")
                    .build();

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.errors.password").isNotEmpty());
        }

        @Test
        @DisplayName("Prázdná přezdívka vrátí 400 Bad Request")
        void testRegister_EmptyNickname_ReturnsBadRequest() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .email("jan@example.cz")
                    .password("heslo123")
                    .nickname("")
                    .build();

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.errors.nickname").isNotEmpty());
        }
    }
}
