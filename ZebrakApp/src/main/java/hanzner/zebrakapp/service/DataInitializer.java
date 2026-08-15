package hanzner.zebrakapp.service;

import hanzner.zebrakapp.entity.Role;
import hanzner.zebrakapp.entity.User;
import hanzner.zebrakapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String @NonNull ... args) {
        String adminEmail = "admin@zebrak.cz";
        userRepository.findByEmail(adminEmail).ifPresentOrElse(
                user -> log.info("Výchozí administrátorský účet '{}' je připraven.", adminEmail),
                () -> {
                    log.info("Vytvářím výchozího administrátora: {}", adminEmail);
                    User admin = User.builder()
                            .email(adminEmail)
                            .password(passwordEncoder.encode("admin123"))
                            .nickname("Administrátor Žebrák")
                            .role(Role.ROLE_ADMIN)
                            .active(true)
                            .build();
                    userRepository.save(admin);
                    log.info("Výchozí administrátorský účet byl úspěšně inicializován.");
                }
        );
    }
}
