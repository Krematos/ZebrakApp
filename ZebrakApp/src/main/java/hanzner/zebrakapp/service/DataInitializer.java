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
        String adminPass = "admin123";

        userRepository.findByEmail(adminEmail).ifPresentOrElse(
                user -> {
                    boolean needsUpdate = false;
                    if (!passwordEncoder.matches(adminPass, user.getPassword())) {
                        user.setPassword(passwordEncoder.encode(adminPass));
                        needsUpdate = true;
                        log.info("Heslo výchozího administrátora '{}' bylo opraveno a aktualizováno.", adminEmail);
                    }
                    if (user.getRole() != Role.ROLE_ADMIN) {
                        user.setRole(Role.ROLE_ADMIN);
                        needsUpdate = true;
                    }
                    if (!user.isActive()) {
                        user.setActive(true);
                        needsUpdate = true;
                    }
                    if (needsUpdate) {
                        userRepository.save(user);
                    }
                    log.info("Výchozí administrátorský účet '{}' je připraven k použití (heslo: {}).", adminEmail, adminPass);
                },
                () -> {
                    log.info("Vytvářím výchozího administrátora: {}", adminEmail);
                    User admin = User.builder()
                            .email(adminEmail)
                            .password(passwordEncoder.encode(adminPass))
                            .nickname("Administrátor Žebrák")
                            .role(Role.ROLE_ADMIN)
                            .active(true)
                            .build();
                    userRepository.save(admin);
                    log.info("Výchozí administrátorský účet byl úspěšně inicializován (heslo: {}).", adminPass);
                }
        );
    }
}
