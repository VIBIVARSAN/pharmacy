package com.pharmacy.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Reads configuration from (highest priority first):
 * environment variables, JVM system properties, an external file named by
 * PHARMACY_CONFIG, and finally db.properties on the classpath.
 * No credentials are ever hardcoded in source.
 */
public final class AppConfig {
    private static final Properties FILE = loadFile();

    private AppConfig() {
    }

    private static Properties loadFile() {
        Properties props = new Properties();
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException ignored) {
            // an unreadable optional file simply means "no values from it"
        }
        String external = firstNonBlank(System.getenv("PHARMACY_CONFIG"), System.getProperty("PHARMACY_CONFIG"));
        if (external != null) {
            try (InputStream in = Files.newInputStream(Path.of(external))) {
                props.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read configuration file " + external, e);
            }
        }
        return props;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    public static String get(String key) {
        return firstNonBlank(System.getenv(key), System.getProperty(key), FILE.getProperty(key));
    }

    public static String get(String key, String defaultValue) {
        String v = get(key);
        return v == null ? defaultValue : v;
    }

    public static int getInt(String key, int defaultValue) {
        String v = get(key);
        if (v == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String v = get(key);
        return v == null ? defaultValue : Boolean.parseBoolean(v);
    }
}
