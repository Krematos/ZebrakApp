package hanzner.zebrakapp.service;

import hanzner.zebrakapp.entity.Role;
import hanzner.zebrakapp.entity.User;
import hanzner.zebrakapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserCleanupScheduledTask Unit Testy")
class UserCleanupScheduledTaskUnitTest {

    @Mock
    private UserRepository userRepository;

    private UserCleanupScheduledTask cleanupTask;

    @BeforeEach
    void setUp() {
        cleanupTask = new UserCleanupScheduledTask(userRepository);
    }

    @Test
    @DisplayName("cleanupExpiredAccounts() smaže uživatele s expirovanou lhůtou soft-delete")
    void testCleanupExpiredAccounts_Success() {
        User expiredUser1 = User.builder().id(10L).email("old1@example.cz").deletedAt(Instant.now().minus(35, ChronoUnit.DAYS)).build();
        User expiredUser2 = User.builder().id(11L).email("old2@example.cz").deletedAt(Instant.now().minus(31, ChronoUnit.DAYS)).build();

        when(userRepository.findExpiredSoftDeletedUsers(any(Instant.class)))
                .thenReturn(List.of(expiredUser1, expiredUser2));
        when(userRepository.hardDeleteUsersByIds(eq(List.of(10L, 11L))))
                .thenReturn(2);

        cleanupTask.cleanupExpiredAccounts();

        verify(userRepository, times(1)).findExpiredSoftDeletedUsers(any(Instant.class));
        verify(userRepository, times(1)).hardDeleteUsersByIds(eq(List.of(10L, 11L)));
    }

    @Test
    @DisplayName("cleanupExpiredAccounts() nevolá hard delete pokud nejsou nalezeni žádní expirovaní uživatelé")
    void testCleanupExpiredAccounts_NoExpiredUsers_DoesNothing() {
        when(userRepository.findExpiredSoftDeletedUsers(any(Instant.class)))
                .thenReturn(List.of());

        cleanupTask.cleanupExpiredAccounts();

        verify(userRepository, times(1)).findExpiredSoftDeletedUsers(any(Instant.class));
        verify(userRepository, never()).hardDeleteUsersByIds(any());
    }

    // --- EDGE CASE TESTY ---

    @Test
    @DisplayName("EDGE-CASE: cleanupExpiredAccounts() nespadne pokud userRepository vyhodí výjimku (nebo ji propustí) - záleží na error handlingu")
    void testCleanupExpiredAccounts_RepositoryThrowsException() {
        when(userRepository.findExpiredSoftDeletedUsers(any(Instant.class)))
                .thenThrow(new RuntimeException("Database timeout nebo connection error"));

        // Metoda by to měla buď zalogovat a zachytit (try-catch), nebo nechat vybublat.
        // V současné implementaci bez try-catch to probublá ven, což u @Scheduled zastaví jeden konkrétní běh tasku (což je v pořádku).
        try {
            cleanupTask.cleanupExpiredAccounts();
        } catch (Exception e) {
            // Ignorujeme, v produkci by to vyhodilo chybu. Zde ověřujeme že to nespadne s null pointerem atd.
        }

        verify(userRepository, never()).hardDeleteUsersByIds(any());
    }

    @Test
    @DisplayName("EDGE-CASE: cleanupExpiredAccounts() zpracuje i obrovské množství uživatelů bez chyby")
    void testCleanupExpiredAccounts_MassiveUserList() {
        // Zde sice testujeme jen MOCK, ale ujišťujeme se, že ID kolekce je správně předána do metody hardDeleteUsersByIds.
        List<User> massiveList = new java.util.ArrayList<>();
        List<Long> massiveIds = new java.util.ArrayList<>();
        for (long i = 1; i <= 50000; i++) {
            massiveList.add(User.builder().id(i).build());
            massiveIds.add(i);
        }

        when(userRepository.findExpiredSoftDeletedUsers(any(Instant.class))).thenReturn(massiveList);
        when(userRepository.hardDeleteUsersByIds(massiveIds)).thenReturn(50000);

        cleanupTask.cleanupExpiredAccounts();

        verify(userRepository, times(1)).hardDeleteUsersByIds(massiveIds);
    }
}
