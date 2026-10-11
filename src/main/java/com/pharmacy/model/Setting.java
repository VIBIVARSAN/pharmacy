package com.pharmacy.model;

import java.io.Serializable;

/** One configurable value (tax rate, thresholds, pharmacy details...). */
public class Setting implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String key;
    private final String value;
    private final String description;

    public Setting(String key, String value, String description) {
        this.key = key;
        this.value = value;
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }
}
