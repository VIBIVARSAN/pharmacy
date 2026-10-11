package com.pharmacy.util;

import org.mindrot.jbcrypt.BCrypt;

/** BCrypt implementation of {@link PasswordEncoder} (salted, adaptive cost). */
public class BCryptPasswordEncoder implements PasswordEncoder {
    private final int cost;

    public BCryptPasswordEncoder() {
        this(12);
    }

    public BCryptPasswordEncoder(int cost) {
        this.cost = cost;
    }

    @Override
    public String hash(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(cost));
    }

    @Override
    public boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null || !storedHash.startsWith("$2")) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, storedHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
