package com.pharmacy.service;

import com.pharmacy.dao.SettingDao;
import com.pharmacy.model.Permission;
import com.pharmacy.model.Setting;
import com.pharmacy.model.User;
import com.pharmacy.util.TransactionManager;
import com.pharmacy.util.Validator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Configurable values stored in the settings table (tax rate, thresholds, payment methods...). */
public class SettingsService extends BaseService {
    public static final String PHARMACY_NAME = "pharmacy_name";
    public static final String CURRENCY = "currency_symbol";
    public static final String TAX_RATE = "tax_rate_percent";
    public static final String DISCOUNTS_ENABLED = "discounts_enabled";
    public static final String PAYMENT_METHODS = "payment_methods";
    public static final String NEAR_EXPIRY_DAYS = "near_expiry_days";
    public static final String DEFAULT_REORDER = "default_reorder_level";

    private final SettingDao settingDao;
    private volatile Map<String, String> cache;

    public SettingsService(TransactionManager tx, AuditService audit, SettingDao settingDao) {
        super(tx, audit);
        this.settingDao = settingDao;
    }

    public List<Setting> findAll() {
        return tx.read(settingDao::findAll);
    }

    /** All settings as key/value pairs (cached; refreshed whenever they are saved). */
    public Map<String, String> asMap() {
        Map<String, String> local = cache;
        if (local == null) {
            local = new LinkedHashMap<>();
            for (Setting s : findAll()) {
                local.put(s.getKey(), s.getValue() == null ? "" : s.getValue());
            }
            cache = local;
        }
        return local;
    }

    public String get(String key) {
        return asMap().getOrDefault(key, "");
    }

    public int getInt(String key, int fallback) {
        try {
            return Integer.parseInt(get(key).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Empty until an administrator has configured a tax rate; billing refuses to guess one. */
    public Optional<BigDecimal> getTaxRate() {
        String v = get(TAX_RATE).trim();
        if (v.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BigDecimal(v));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public boolean isDiscountsEnabled() {
        return Boolean.parseBoolean(get(DISCOUNTS_ENABLED).trim());
    }

    public int getNearExpiryDays() {
        return getInt(NEAR_EXPIRY_DAYS, 90);
    }

    public List<String> getPaymentMethods() {
        List<String> methods = new ArrayList<>();
        Arrays.stream(get(PAYMENT_METHODS).split(",")).map(String::trim).filter(s -> !s.isEmpty()).forEach(methods::add);
        return methods;
    }

    public void update(User actor, Map<String, String> submitted) {
        require(actor, Permission.SETTINGS_MANAGE);
        Map<String, String> values = new LinkedHashMap<>();
        Validator v = new Validator();
        for (Setting s : findAll()) {
            if (!submitted.containsKey(s.getKey())) {
                continue;
            }
            String value = submitted.get(s.getKey()) == null ? "" : submitted.get(s.getKey()).trim();
            v.maxLength(s.getKey(), value, 500, "Value");
            switch (s.getKey()) {
                case PHARMACY_NAME -> v.required(s.getKey(), value, "Pharmacy name");
                case CURRENCY -> v.check(!value.isEmpty() && value.length() <= 5, s.getKey(), "Currency symbol must be 1-5 characters.");
                case TAX_RATE -> v.check(value.isEmpty() || isDecimalBetween(value, 0, 100), s.getKey(),
                        "Tax rate must be a number between 0 and 100 (leave empty only if billing is not used yet).");
                case DISCOUNTS_ENABLED -> v.check(value.equals("true") || value.equals("false"), s.getKey(), "Choose yes or no.");
                case PAYMENT_METHODS -> v.check(Arrays.stream(value.split(",")).anyMatch(m -> !m.isBlank()), s.getKey(),
                        "Enter at least one payment method.");
                case NEAR_EXPIRY_DAYS -> v.check(isIntBetween(value, 1, 730), s.getKey(), "Enter a number of days between 1 and 730.");
                case DEFAULT_REORDER -> v.check(isIntBetween(value, 0, 100000), s.getKey(), "Enter a whole number of zero or more.");
                default -> {
                    // free text settings (address, phone, licence)
                }
            }
            values.put(s.getKey(), value);
        }
        v.throwIfInvalid();
        tx.inTransaction(con -> {
            for (Map.Entry<String, String> e : values.entrySet()) {
                settingDao.updateValue(con, e.getKey(), e.getValue().isEmpty() ? null : e.getValue());
            }
            audit.log(con, actor.getId(), "SETTINGS_UPDATED", "SETTINGS", null,
                    "Updated settings: " + String.join(", ", values.keySet()));
            return null;
        });
        cache = null;
    }

    private static boolean isIntBetween(String value, int min, int max) {
        try {
            int n = Integer.parseInt(value);
            return n >= min && n <= max;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isDecimalBetween(String value, int min, int max) {
        try {
            BigDecimal n = new BigDecimal(value);
            return n.compareTo(BigDecimal.valueOf(min)) >= 0 && n.compareTo(BigDecimal.valueOf(max)) <= 0 && n.scale() <= 2;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
