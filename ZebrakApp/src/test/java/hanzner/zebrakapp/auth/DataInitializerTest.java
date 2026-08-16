package hanzner.zebrakapp.auth;

import hanzner.zebrakapp.dto.AuthRequest;
import hanzner.zebrakapp.dto.AuthResponse;
import hanzner.zebrakapp.entity.Role;
import hanzner.zebrakapp.entity.User;
import hanzner.zebrakapp.repository.UserRepository;
import hanzner.zebrakapp.service.AuthService;
import hanzner.zebrakapp.service.DataInitializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("Test")
@Transactional
class DataInitializerTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DataInitializer dataInitializer;

    @Test
    @DisplayName("Výchozí administrátor se může úspěšně přihlásit s heslem admin123")
    void testLoginWithDefaultAdmin() {
        AuthRequest req = AuthRequest.builder()
                .email("admin@zebrak.cz")
                .password("admin123")
                .build();

        AuthResponse response = authService.login(req);
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("admin@zebrak.cz", response.getUser().getEmail());
        assertEquals(Role.ROLE_ADMIN, response.getUser().getRole());
    }

    @Test
    @DisplayName("DataInitializer zachovává změněné heslo administrátora a nepřepisuje ho při restartu")
    void testDataInitializerPreservesUserChangedPassword() {
        String newPassword = "MyNewSecurePassword999!";

        // 1. Administrátor si změní heslo na nové
        User admin = userRepository.findByEmail("admin@zebrak.cz").orElseThrow();
        admin.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(admin);

        // 2. Spustíme DataInitializer znovu (simulace restartu aplikace)
        dataInitializer.run();

        // 3. Ověříme, že se admin přihlásí s novým heslem a staré heslo neplatí
        AuthRequest validReq = AuthRequest.builder()
                .email("admin@zebrak.cz")
                .password(newPassword)
                .build();

        AuthResponse response = authService.login(validReq);
        assertNotNull(response);
        assertEquals("admin@zebrak.cz", response.getUser().getEmail());
        assertEquals(Role.ROLE_ADMIN, response.getUser().getRole());

        // Ověříme, že staré heslo 'admin123' bylo nahrazeno a DataInitializer ho neobnovil
        AuthRequest oldReq = AuthRequest.builder()
                .email("admin@zebrak.cz")
                .password("admin123")
                .build();

        assertThrows(Exception.class, () -> authService.login(oldReq));
    }
}
