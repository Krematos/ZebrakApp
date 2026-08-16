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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("Test")
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
    @DisplayName("DataInitializer automaticky opraví heslo, pokud je v databázi poškozený hash")
    void testDataInitializerRepairsCorruptedPassword() {
        // 1. Zavedeme chybný hash k účtu admin@zebrak.cz
        User admin = userRepository.findByEmail("admin@zebrak.cz").orElseThrow();
        admin.setPassword("corrupted_hash_that_fails");
        userRepository.save(admin);

        // 2. Spustíme DataInitializer znovu
        dataInitializer.run();

        // 3. Ověříme, že se admin nyní bez problému přihlásí
        AuthRequest req = AuthRequest.builder()
                .email("admin@zebrak.cz")
                .password("admin123")
                .build();

        AuthResponse response = authService.login(req);
        assertNotNull(response);
        assertEquals(Role.ROLE_ADMIN, response.getUser().getRole());
    }
}
