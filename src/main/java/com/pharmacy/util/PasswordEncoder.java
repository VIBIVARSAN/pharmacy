package com.pharmacy.util;

/** Contract for one-way password hashing. */
public interface PasswordEncoder {
    String hash(String rawPassword);

    boolean matches(String rawPassword, String storedHash);
}
