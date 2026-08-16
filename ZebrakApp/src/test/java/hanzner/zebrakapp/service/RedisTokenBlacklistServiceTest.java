package hanzner.zebrakapp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisTokenBlacklistService Unit Testy")
class RedisTokenBlacklistServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisTokenBlacklistService blacklistService;

    @BeforeEach
    void setUp() {
        blacklistService = new RedisTokenBlacklistService(redisTemplate);
    }

    @Test
    @DisplayName("blacklistToken() uloží hash tokenu do Redisu s nastaveným TTL")
    void testBlacklistToken_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String sampleJwt = "header.payload.signature";
        Duration ttl = Duration.ofMinutes(15);

        blacklistService.blacklistToken(sampleJwt, ttl);

        verify(valueOperations, times(1)).set(
                startsWith("jwt:blacklist:"),
                eq("blacklisted"),
                eq(ttl)
        );
    }

    @Test
    @DisplayName("blacklistToken() ignoruje null nebo prázdný token a neplatné TTL")
    void testBlacklistToken_InvalidInputs_DoesNothing() {
        blacklistService.blacklistToken(null, Duration.ofMinutes(5));
        blacklistService.blacklistToken("", Duration.ofMinutes(5));
        blacklistService.blacklistToken("token", Duration.ZERO);
        blacklistService.blacklistToken("token", Duration.ofSeconds(-10));
        blacklistService.blacklistToken("token", null);

        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("isBlacklisted() vrátí true pokud klíč v Redisu existuje")
    void testIsBlacklisted_True() {
        when(redisTemplate.hasKey(startsWith("jwt:blacklist:"))).thenReturn(Boolean.TRUE);

        boolean result = blacklistService.isBlacklisted("test.jwt.token");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isBlacklisted() vrátí false pokud klíč v Redisu neexistuje")
    void testIsBlacklisted_False() {
        when(redisTemplate.hasKey(startsWith("jwt:blacklist:"))).thenReturn(Boolean.FALSE);

        boolean result = blacklistService.isBlacklisted("test.jwt.token");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isBlacklisted() vrátí false pro prázdný nebo null token")
    void testIsBlacklisted_NullOrEmpty_ReturnsFalse() {
        assertThat(blacklistService.isBlacklisted(null)).isFalse();
        assertThat(blacklistService.isBlacklisted("")).isFalse();
        assertThat(blacklistService.isBlacklisted("   ")).isFalse();

        verifyNoInteractions(redisTemplate);
    }
}
