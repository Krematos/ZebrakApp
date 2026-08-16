package hanzner.zebrakapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Redis implementace blacklistu JWT tokenů.
 * Ukládá SHA-256 hash tokenu do in-memory databáze Redis s automatickým TTL do expirace JWT.
 */
@Service
@org.springframework.context.annotation.Profile("!test & !Test")
@RequiredArgsConstructor
@Slf4j
public class RedisTokenBlacklistService implements TokenBlacklistService {

    private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";
    private final StringRedisTemplate redisTemplate;

    @Override
    public void blacklistToken(String token, Duration ttl) {
        if (token == null || token.isBlank() || ttl == null || ttl.isNegative() || ttl.isZero()) {
            return;
        }

        try {
            String key = buildKey(token);
            redisTemplate.opsForValue().set(key, "blacklisted", ttl);
            log.info("JWT token byl zařazen na Redis blacklist s TTL: {}s", ttl.toSeconds());
        } catch (Exception e) {
            log.error("Nepodařilo se zařadit JWT token do Redis blacklistu: {}", e.getMessage());
        }
    }

    @Override
    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            String key = buildKey(token);
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Chyba při ověřování JWT tokenu v Redis blacklistu: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Vytváří kompaktní klíč zkrácením celého JWT tokenu pomocí SHA-256 hashe.
     */
    private String buildKey(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return BLACKLIST_KEY_PREFIX + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return BLACKLIST_KEY_PREFIX + token;
        }
    }
}
