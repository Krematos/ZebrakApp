package hanzner.zebrakapp.service;

import hanzner.zebrakapp.entity.User;
import hanzner.zebrakapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserCleanupScheduledTask {

    private final UserRepository userRepository;

    /**
     * Běží každý den ve 3:00 ráno.
     * Vyhledá uživatele označené ke smazání (deleted_at) před více než 30 dny a trvale je odstraní z databáze.
     * Díky ON DELETE SET NULL na tabulce places a ON DELETE CASCADE na place_verifications zůstane integrita zachována.
     */
    @Scheduled(cron = "${app.cleanup.cron:0 0 3 * * ?}")
    @Transactional
    public void cleanupExpiredAccounts() {
        Instant cutoffDate = Instant.now().minus(30);
        log.info("Spouštím pravidelné čištění expirovaných soft-deleted uživatelů (starších než {})", cutoffDate);

        List<User> expiredUsers = userRepository.findExpiredSoftDeletedUsers(cutoffDate);

        if (expiredUsers.isEmpty()) {
            log.info("Žádné účty k trvalému smazání nebyly nalezeny.");
            return;
        }

        List<Long> idsToDelete = expiredUsers.stream().map(User::getId).toList();
        log.info("Nalezeno {} účtů ke konečnému odstranění (IDs: {}).", idsToDelete.size(), idsToDelete);

        int deletedCount = userRepository.hardDeleteUsersByIds(idsToDelete);
        log.info("Úspěšně trvale smazáno {} uživatelských účtů (splněno GDPR právo na výmaz po uplynutí 30denní lhůty).", deletedCount);
    }
}
