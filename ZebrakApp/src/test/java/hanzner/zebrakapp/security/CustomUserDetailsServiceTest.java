package hanzner.zebrakapp.security;

import hanzner.zebrakapp.entity.Role;
import hanzner.zebrakapp.entity.User;
import hanzner.zebrakapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService & CustomUserDetails Unit Testy")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(42L)
                .email("karel@example.cz")
                .password("hashHesla123")
                .nickname("Karel")
                .role(Role.ROLE_USER)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("loadUserByUsername úspěšně načte uživatele a vrátí CustomUserDetails")
    void testLoadUserByUsername_Success() {
        when(userRepository.findByEmail("karel@example.cz")).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("karel@example.cz");

        assertNotNull(userDetails);
        assertInstanceOf(CustomUserDetails.class, userDetails);
        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;

        assertEquals(42L, customUserDetails.getId());
        assertEquals("karel@example.cz", customUserDetails.getUsername());
        assertEquals("hashHesla123", customUserDetails.getPassword());
        assertEquals("Karel", customUserDetails.getNickname());
        assertEquals(Role.ROLE_USER, customUserDetails.getUser().getRole());
        assertEquals(testUser, customUserDetails.getUser());
        assertTrue(customUserDetails.isEnabled());
        assertTrue(customUserDetails.isAccountNonExpired());
        assertTrue(customUserDetails.isAccountNonLocked());
        assertTrue(customUserDetails.isCredentialsNonExpired());

        assertTrue(customUserDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    @DisplayName("loadUserByUsername vyhodí UsernameNotFoundException pokud e-mail neexistuje")
    void testLoadUserByUsername_NotFound_ThrowsException() {
        when(userRepository.findByEmail("neexistuje@example.cz")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("neexistuje@example.cz"));
    }

    @Test
    @DisplayName("Admin uživatel má autoritu ROLE_ADMIN")
    void testAdminAuthorities() {
        testUser.setRole(Role.ROLE_ADMIN);
        CustomUserDetails details = new CustomUserDetails(testUser);

        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("Deaktivovaný uživatel má isEnabled() == false")
    void testDisabledUser() {
        testUser.setActive(false);
        CustomUserDetails details = new CustomUserDetails(testUser);

        assertFalse(details.isEnabled());
    }
}
