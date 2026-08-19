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
}
