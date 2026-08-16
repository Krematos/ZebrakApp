package hanzner.zebrakapp.service;

import java.time.Duration;

/**
 * Služba pro správu blacklistu zneplatněných JWT tokenů (při odhlášení uživatele).
 */
public interface TokenBlacklistService {

    /**
     * Zařadí JWT token na blacklist s danou dobou platnosti (TTL do přirozené expirace tokenu).
     *
     * @param token JWT token
     * @param ttl   Zbývající doba platnosti tokenu
     */
    void blacklistToken(String token, Duration ttl);

    /**
     * Zjistí, zda se zadaný token nachází na blacklistu zneplatněných tokenů.
     *
     * @param token JWT token
     * @return true pokud je token zneplatněn, jinak false
     */
    boolean isBlacklisted(String token);
}
