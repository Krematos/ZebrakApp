package hanzner.zebrakapp.service;

import hanzner.zebrakapp.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class RateLimiterService {

    private final long minIntervalMs;
    private final int maxRequestsPerMinute;

    private static class UserRequestHistory {
        long lastRequestTime = 0;
        final ConcurrentLinkedQueue<Long> requestTimestamps = new ConcurrentLinkedQueue<>();
    }

    private final ConcurrentHashMap<Long, UserRequestHistory> userHistories = new ConcurrentHashMap<>();

    public RateLimiterService(
            @Value("${app.rate-limit.place-creation.min-interval-seconds:5}") long minIntervalSeconds,
            @Value("${app.rate-limit.place-creation.max-requests-per-minute:5}") int maxRequestsPerMinute
    ) {
        this.minIntervalMs = minIntervalSeconds * 1000L;
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    /**
     * Zkontroluje a zaznamená požadavek na vytvoření místa pro daného uživatele.
     * Pokud uživatel poruší minimální prodlevu nebo překročí limit za minutu, vyhodí RateLimitExceededException (HTTP 429).
     *
     * @param userId ID přihlášeného uživatele
     */
    public synchronized void checkPlaceCreationRateLimit(Long userId) {
        if (userId == null) {
            return;
        }

        long now = System.currentTimeMillis();
        UserRequestHistory history = userHistories.computeIfAbsent(userId, k -> new UserRequestHistory());

        // 1. Kontrola minimální prodlevy (cooldown) mezi 2 požadavky
        long timeSinceLastRequest = now - history.lastRequestTime;
        if (history.lastRequestTime > 0 && timeSinceLastRequest < minIntervalMs) {
            long remainingSeconds = (minIntervalMs - timeSinceLastRequest + 999) / 1000;
            throw new RateLimitExceededException(
                    String.format("Vytváříte místa příliš rychle. Počkejte prosím ještě %d s před zadáním dalšího místa.", remainingSeconds)
            );
        }

        // 2. Kontrola limitu v posuvném okně (max N požadavků za posledních 60 sekund)
        long oneMinuteAgo = now - 60_000L;
        while (!history.requestTimestamps.isEmpty() && history.requestTimestamps.peek() < oneMinuteAgo) {
            history.requestTimestamps.poll();
        }

        if (history.requestTimestamps.size() >= maxRequestsPerMinute) {
            throw new RateLimitExceededException(
                    String.format("Byl překročen limit pro vytváření míst (%d míst za minutu). Zkuste to prosím za chvíli.", maxRequestsPerMinute)
            );
        }

        // Zaznamenání úspěšného požadavku
        history.lastRequestTime = now;
        history.requestTimestamps.offer(now);

        // Periodické promazání starých neaktivních záznamů (starších než 10 minut)
        if (userHistories.size() > 500) {
            cleanupStaleEntries(now);
        }
    }

    private void cleanupStaleEntries(long now) {
        long tenMinutesAgo = now - 600_000L;
        userHistories.entrySet().removeIf(entry -> entry.getValue().lastRequestTime < tenMinutesAgo);
    }

    public void reset() {
        userHistories.clear();
    }
}
