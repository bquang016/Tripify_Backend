package com.example.smart_booking_system.service.ai;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BookingSessionManager {

    private final Map<String, BookingSession> sessions = new ConcurrentHashMap<>();

    private final Duration ttl = Duration.ofMinutes(15);

    public BookingSession getOrCreate(String userId) {
        return sessions.compute(userId, (k, v) -> {
            if (v == null) return new BookingSession();
            if (v.getUpdatedAt().isBefore(Instant.now().minus(ttl))) {
                return new BookingSession();
            }
            return v;
        });
    }

    public void reset(String userId) {
        BookingSession s = sessions.get(userId);
        if (s != null) s.reset();
    }

    public void clear(String userId) {
        sessions.remove(userId);
    }
}
