package hanzner.zebrakapp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementace blacklistu JWT tokenů pro testovací prostředí.
 * Zajišťuje spolehlivé spouštění testů bez nutnosti běžící instance Redisu.
 */
@Service
@Profile("test | Test")
@Slf4j
public class InMemoryTokenBlacklistService implements TokenBlacklistService {

    private final Map<String, Instant> blacklist = new ConcurrentHashMap<>();

    @Override
    public void blacklistToken(String token, Duration ttl) {
        if (token == null || token.isBlank() || ttl == null || ttl.isNegative() || ttl.isZero()) {
            return;
        }
        Instant expiryTime = Instant.now().plus(ttl);
        blacklist.put(token, expiryTime);
        log.info("In-memory blacklist: token zařazen s expirací v {}", expiryTime);
    }

    @Override
    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        Instant expiry = blacklist.get(token);
        if (expiry == null) {
            return false;
        }
        if (Instant.now().isAfter(expiry)) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }

    public void clear() {
        blacklist.clear();
    }
}
