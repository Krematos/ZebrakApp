package hanzner.zebrakapp.service;

import hanzner.zebrakapp.dto.DeleteAccountRequest;
import hanzner.zebrakapp.dto.UserDto;
import hanzner.zebrakapp.entity.Role;
import hanzner.zebrakapp.entity.User;
import hanzner.zebrakapp.exception.InvalidPasswordException;
import hanzner.zebrakapp.exception.UnauthorizedActionException;
import hanzner.zebrakapp.exception.UserNotFoundException;
import hanzner.zebrakapp.repository.UserRepository;
import hanzner.zebrakapp.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Testy")
class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private AuthService authService;

    private UserService userService;

    private User sampleUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, tokenBlacklistService, tokenProvider, authService);

        sampleUser = User.builder()
                .id(1L)
                .email("user@example.cz")
                .nickname("BeznyUser")
                .password("encoded_secret_password")
                .role(Role.ROLE_USER)
                .active(true)
                .createdAt(Instant.now())
                .build();

        adminUser = User.builder()
                .id(99L)
                .email("admin@example.cz")
                .nickname("Admin")
                .password("encoded_admin_password")
                .role(Role.ROLE_ADMIN)
                .active(true)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("deleteMyAccount() úspěšně provede soft-delete a zařadí JWT na blacklist")
    void testDeleteMyAccount_Success() {
        DeleteAccountRequest request = new DeleteAccountRequest("mojeHeslo123");
        String jwtToken = "valid.jwt.token";

        when(passwordEncoder.matches("mojeHeslo123", "encoded_secret_password")).thenReturn(true);
        when(tokenProvider.getRemainingExpirationMs(jwtToken)).thenReturn(3600000L);

        userService.deleteMyAccount(sampleUser, request, jwtToken);

        verify(userRepository, times(1)).delete(sampleUser);
        verify(tokenBlacklistService, times(1)).blacklistToken(eq(jwtToken), eq(Duration.ofMillis(3600000L)));
    }

    @Test
    @DisplayName("deleteMyAccount() vyhodí InvalidPasswordException při chybném heslu")
    void testDeleteMyAccount_WrongPassword_ThrowsException() {
        DeleteAccountRequest request = new DeleteAccountRequest("spatneHeslo");
        String jwtToken = "valid.jwt.token";

        when(passwordEncoder.matches("spatneHeslo", "encoded_secret_password")).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteMyAccount(sampleUser, request, jwtToken))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("Zadané heslo není správné");

        verify(userRepository, never()).delete(any());
        verifyNoInteractions(tokenBlacklistService);
    }

    @Test
    @DisplayName("deleteUserByAdmin() úspěšně smaže jiného uživatele")
    void testDeleteUserByAdmin_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        userService.deleteUserByAdmin(1L, adminUser);

        verify(userRepository, times(1)).delete(sampleUser);
    }

    @Test
    @DisplayName("deleteUserByAdmin() zabrání administrátorovi smazat sám sebe")
    void testDeleteUserByAdmin_SelfDelete_ThrowsException() {
        assertThatThrownBy(() -> userService.deleteUserByAdmin(99L, adminUser))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("nemůže smazat svůj vlastní účet");

        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteUserByAdmin() vyhodí UserNotFoundException pokud uživatel neexistuje")
    void testDeleteUserByAdmin_NotFound_ThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUserByAdmin(999L, adminUser))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("getAllUsers() vrátí namapovaný seznam uživatelů")
    void testGetAllUsers_Success() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser, adminUser));
        when(authService.mapToUserDto(sampleUser)).thenReturn(UserDto.builder().id(1L).email("user@example.cz").nickname("BeznyUser").build());
        when(authService.mapToUserDto(adminUser)).thenReturn(UserDto.builder().id(99L).email("admin@example.cz").nickname("Admin").build());

        List<UserDto> users = userService.getAllUsers();

        assertThat(users).hasSize(2);
        assertThat(users.get(0).getNickname()).isEqualTo("BeznyUser");
        assertThat(users.get(1).getNickname()).isEqualTo("Admin");
    }

    // --- EDGE CASE TESTY ---

    @Test
    @DisplayName("EDGE-CASE: deleteMyAccount() s prázdným heslem vyhodí výjimku")
    void testDeleteMyAccount_EmptyPassword() {
        DeleteAccountRequest request = new DeleteAccountRequest("");
        String jwtToken = "valid.jwt.token";

        assertThatThrownBy(() -> userService.deleteMyAccount(sampleUser, request, jwtToken))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("Zadané heslo není správné");

        verify(userRepository, never()).delete(any());
        verifyNoInteractions(tokenBlacklistService);
    }

    @Test
    @DisplayName("EDGE-CASE: deleteMyAccount() s null JWT tokenem nespadne, ale provede smazání")
    void testDeleteMyAccount_NullJwt() {
        DeleteAccountRequest request = new DeleteAccountRequest("mojeHeslo123");

        when(passwordEncoder.matches("mojeHeslo123", "encoded_secret_password")).thenReturn(true);
        // Token provider by měl vrátit 0 nebo spadnout, záleží na implementaci, předpokládáme že blacklist to ignoruje
        
        userService.deleteMyAccount(sampleUser, request, null);

        verify(userRepository, times(1)).delete(sampleUser);
        verify(tokenBlacklistService, never()).blacklistToken(any(), any());
    }

    @Test
    @DisplayName("EDGE-CASE: deleteUserByAdmin() se pokusí smazat jiného admina - mělo by to projít (nebo selhat pokud to business logika zakazuje)")
    void testDeleteUserByAdmin_DeleteAnotherAdmin() {
        User anotherAdmin = User.builder().id(100L).email("admin2@example.cz").role(Role.ROLE_ADMIN).build();
        when(userRepository.findById(100L)).thenReturn(Optional.of(anotherAdmin));

        // Metoda nekontroluje roli cílového uživatele, pouze to, zda nemaže sám sebe
        userService.deleteUserByAdmin(100L, adminUser);

        verify(userRepository, times(1)).delete(anotherAdmin);
    }

    @Test
    @DisplayName("EDGE-CASE: deleteUserByAdmin() s neexistujícím ID uživatele (např. ID = null)")
    void testDeleteUserByAdmin_NullId() {
        assertThatThrownBy(() -> userService.deleteUserByAdmin(null, adminUser))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).delete(any());
    }
}
