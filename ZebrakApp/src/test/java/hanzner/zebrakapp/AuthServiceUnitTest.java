package hanzner.zebrakapp;

import hanzner.zebrakapp.dto.AuthResponse;
import hanzner.zebrakapp.dto.RegisterRequest;
import hanzner.zebrakapp.entity.Role;
import hanzner.zebrakapp.entity.User;
import hanzner.zebrakapp.exception.ErrorCode;
import hanzner.zebrakapp.exception.UserAlreadyExistException;
import hanzner.zebrakapp.repository.UserRepository;
import hanzner.zebrakapp.security.CustomUserDetails;
import hanzner.zebrakapp.security.JwtTokenProvider;
import hanzner.zebrakapp.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Úspěšná registrace vytvoří uživatele s hashem hesla, rolí ROLE_USER a vygeneruje JWT token")
    void testRegister_Success() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .email("jan.novak@example.cz")
                .password("tajneHeslo123")
                .nickname("JanNovak")
                .build();

        when(userRepository.existsByEmail("jan.novak@example.cz")).thenReturn(false);
        when(passwordEncoder.encode("tajneHeslo123")).thenReturn("$2a$10$encodedPasswordHash");

        User savedUser = User.builder()
                .id(1L)
                .email("jan.novak@example.cz")
                .password("$2a$10$encodedPasswordHash")
                .nickname("JanNovak")
                .role(Role.ROLE_USER)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        Authentication authMock = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authMock);
        when(tokenProvider.generateToken(authMock)).thenReturn("mocked.jwt.token");

        // When
        AuthResponse response = authService.register(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mocked.jwt.token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getId()).isEqualTo(1L);
        assertThat(response.getUser().getEmail()).isEqualTo("jan.novak@example.cz");
        assertThat(response.getUser().getNickname()).isEqualTo("JanNovak");
        assertThat(response.getUser().getRole()).isEqualTo(Role.ROLE_USER);

        // Ověření, že do repository byl předán správně sestavený User
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();

        assertThat(capturedUser.getEmail()).isEqualTo("jan.novak@example.cz");
        assertThat(capturedUser.getPassword()).isEqualTo("$2a$10$encodedPasswordHash");
        assertThat(capturedUser.getNickname()).isEqualTo("JanNovak");
        assertThat(capturedUser.getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(capturedUser.isActive()).isTrue();
    }

    @Test
    @DisplayName("Registrace s již existujícím e-mailem vyhodí UserAlreadyExistException a neuloží uživatele")
    void testRegister_EmailAlreadyExists_ThrowsUserAlreadyExistException() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .email("existujici@example.cz")
                .password("heslo123")
                .nickname("Pepa")
                .build();

        when(userRepository.existsByEmail("existujici@example.cz")).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistException.class)
                .hasMessage("Uživatel s tímto e-mailem již existuje")
                .satisfies(ex -> {
                    UserAlreadyExistException e = (UserAlreadyExistException) ex;
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_ALREADY_EXISTS);
                });

        // Ověříme, že se neukládalo ani neautentizovalo
        verify(userRepository, never()).save(any());
        verify(authenticationManager, never()).authenticate(any());
        verify(tokenProvider, never()).generateToken(any());
    }

    @Test
    @DisplayName("Registrace ořízne bílé znaky a převede e-mail na malá písmena")
    void testRegister_TrimsAndLowercasesEmailAndNickname() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .email("   Jan.NOVAK@Example.CZ   ")
                .password("heslo123")
                .nickname("   Jan Novak   ")
                .build();

        when(userRepository.existsByEmail("jan.novak@example.cz")).thenReturn(false);
        when(passwordEncoder.encode("heslo123")).thenReturn("hashedPassword");

        User savedUser = User.builder()
                .id(2L)
                .email("jan.novak@example.cz")
                .nickname("Jan Novak")
                .role(Role.ROLE_USER)
                .active(true)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(tokenProvider.generateToken(any())).thenReturn("token123");

        // When
        AuthResponse response = authService.register(request);

        // Then
        verify(userRepository).existsByEmail("jan.novak@example.cz");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User user = userCaptor.getValue();

        assertThat(user.getEmail()).isEqualTo("jan.novak@example.cz");
        assertThat(user.getNickname()).isEqualTo("Jan Novak");

        // Ověříme, že i authenticate dostal oříznutý a lowercase email
        ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(authCaptor.capture());
        assertThat(authCaptor.getValue().getPrincipal()).isEqualTo("jan.novak@example.cz");
    }

    @Test
    @DisplayName("Registrace nikdy neukládá heslo v čistém textu")
    void testRegister_NeverSavesRawPassword() {
        // Given
        String rawPassword = "SuperSecretPassword123!";
        RegisterRequest request = RegisterRequest.builder()
                .email("test@test.cz")
                .password(rawPassword)
                .nickname("Tester")
                .build();

        when(userRepository.existsByEmail("test@test.cz")).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn("$2a$12$SecureBCryptHashValue");

        User savedUser = User.builder()
                .id(3L)
                .email("test@test.cz")
                .password("$2a$12$SecureBCryptHashValue")
                .nickname("Tester")
                .role(Role.ROLE_USER)
                .active(true)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(tokenProvider.generateToken(any())).thenReturn("tokenABC");

        // When
        authService.register(request);

        // Then
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isNotEqualTo(rawPassword);
        assertThat(captor.getValue().getPassword()).isEqualTo("$2a$12$SecureBCryptHashValue");
    }
}
