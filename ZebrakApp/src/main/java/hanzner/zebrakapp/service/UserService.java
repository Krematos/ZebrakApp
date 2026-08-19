package hanzner.zebrakapp.service;

import hanzner.zebrakapp.dto.DeleteAccountRequest;
import hanzner.zebrakapp.dto.PagedResponse;
import hanzner.zebrakapp.dto.UserDto;
import hanzner.zebrakapp.entity.User;
import hanzner.zebrakapp.exception.InvalidPasswordException;
import hanzner.zebrakapp.exception.UnauthorizedActionException;
import hanzner.zebrakapp.exception.UserNotFoundException;
import hanzner.zebrakapp.repository.UserRepository;
import hanzner.zebrakapp.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtTokenProvider tokenProvider;
    private final AuthService authService;

    @Transactional
    public void deleteMyAccount(User currentUser, DeleteAccountRequest request, String currentJwtToken) {
        if (!passwordEncoder.matches(request.getPassword(), currentUser.getPassword())) {
            log.warn("Neplatné heslo při pokusu o smazání účtu uživatele ID: {}", currentUser.getId());
            throw new InvalidPasswordException("Zadané heslo není správné.");
        }

        userRepository.delete(currentUser);
        log.info("Uživatel s ID {} byl úspěšně označen jako smazaný (soft-delete).", currentUser.getId());

        if (currentJwtToken != null && !currentJwtToken.isBlank()) {
            long remainingMs = tokenProvider.getRemainingExpirationMs(currentJwtToken);
            if (remainingMs > 0) {
                tokenBlacklistService.blacklistToken(currentJwtToken, Duration.ofMillis(remainingMs));
            }
        }
    }

    @Transactional
    public void deleteUserByAdmin(Long targetUserId, User adminUser) {
        if (adminUser != null && adminUser.getId().equals(targetUserId)) {
            throw new UnauthorizedActionException("Administrátor nemůže smazat svůj vlastní účet přes administrátorské rozhraní.");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));

        userRepository.delete(targetUser);
        log.info("Uživatel s ID {} byl soft-deleted administrátorem ID {}.", targetUserId, adminUser != null ? adminUser.getId() : null);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserDto> getAllUsers(Pageable pageable) {
        Pageable effectivePageable = (pageable != null)
                ? pageable
                : PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<User> usersPage = userRepository.findAll(effectivePageable);
        return PagedResponse.of(usersPage, authService::mapToUserDto);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(authService::mapToUserDto)
                .toList();
    }
}
