package com.pharmacy.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Slows down password guessing: after too many failures an account name is locked for a few minutes. */
public class LoginAttemptTracker {
    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_TIME = Duration.ofMinutes(5);

    private static final class Attempts {
        private int failures;
        private Instant lockedUntil;
    }

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    public boolean isLocked(String username) {
        Attempts a = attempts.get(key(username));
        if (a == null) {
            return false;
        }
        synchronized (a) {
            if (a.lockedUntil != null && Instant.now().isAfter(a.lockedUntil)) {
                attempts.remove(key(username));
                return false;
            }
            return a.lockedUntil != null;
        }
    }

    public void recordFailure(String username) {
        if (attempts.size() > 10_000) {
            attempts.clear(); // bound memory if someone floods random usernames
        }
        Attempts a = attempts.computeIfAbsent(key(username), k -> new Attempts());
        synchronized (a) {
            a.failures++;
            if (a.failures >= MAX_FAILURES) {
                a.lockedUntil = Instant.now().plus(LOCK_TIME);
            }
        }
    }

    public void recordSuccess(String username) {
        attempts.remove(key(username));
    }

    private static String key(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }
}
