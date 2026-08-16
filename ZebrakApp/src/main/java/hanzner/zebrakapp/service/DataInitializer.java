package hanzner.zebrakapp.service;

import hanzner.zebrakapp.entity.Role;
import hanzner.zebrakapp.entity.User;
import hanzner.zebrakapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@zebrak.cz}")
    private String adminEmail;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    @Value("${app.admin.nickname:Administrátor Žebrák}")
    private String adminNickname;

    @Override
    @Transactional
    public void run(String @NonNull ... args) {
        userRepository.findByEmail(adminEmail).ifPresentOrElse(
                user -> {
                    boolean needsUpdate = false;
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
                    log.info("Výchozí administrátorský účet '{}' je aktivní.", adminEmail);
                },
                () -> {
                    log.info("Vytvářím výchozího administrátora: {}", adminEmail);
                    User admin = User.builder()
                            .email(adminEmail)
                            .password(passwordEncoder.encode(adminPassword))
                            .nickname(adminNickname)
                            .role(Role.ROLE_ADMIN)
                            .active(true)
                            .build();
                    userRepository.save(admin);
                    log.info("Výchozí administrátorský účet '{}' byl úspěšně vytvořen.", adminEmail);
                }
        );
    }
}
