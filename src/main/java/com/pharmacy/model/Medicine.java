package com.pharmacy.model;

/** A catalog entry. Stock is not stored here; it is the sum of the medicine's batches. */
public class Medicine extends BaseEntity implements Auditable {
    private static final long serialVersionUID = 1L;

    private String medicineCode;
    private String name;
    private String genericName;
    private String category;
    private String manufacturer;
    private String dosageForm;
    private String strength;
    private String unit;
    private int reorderLevel;
    private String description;
    private boolean active = true;

    // Derived from medicine_batches when the medicine is loaded for display.
    private int availableStock;
    private int expiredStock;

    @Override
    public String getAuditType() {
        return "MEDICINE";
    }

    @Override
    public String getAuditLabel() {
        return medicineCode;
    }

    /** Name with strength and form, e.g. "Paracetamol 500 mg (Tablet)". */
    public String getLabel() {
        return buildLabel(name, strength, dosageForm);
    }

    public static String buildLabel(String name, String strength, String dosageForm) {
        StringBuilder sb = new StringBuilder(name == null ? "" : name);
        if (strength != null && !strength.isBlank()) {
            sb.append(' ').append(strength);
        }
        if (dosageForm != null && !dosageForm.isBlank()) {
            sb.append(" (").append(dosageForm).append(')');
        }
        return sb.toString();
    }

    /** Usable (non-expired) stock is at or below the reorder level. */
    public boolean isLowStock() {
        return active && availableStock <= reorderLevel;
    }

    public String getMedicineCode() {
        return medicineCode;
    }

    public void setMedicineCode(String medicineCode) {
        this.medicineCode = medicineCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGenericName() {
        return genericName;
    }

    public void setGenericName(String genericName) {
        this.genericName = genericName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getDosageForm() {
        return dosageForm;
    }

    public void setDosageForm(String dosageForm) {
        this.dosageForm = dosageForm;
    }

    public String getStrength() {
        return strength;
    }

    public void setStrength(String strength) {
        this.strength = strength;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(int availableStock) {
        this.availableStock = availableStock;
    }

    public int getExpiredStock() {
        return expiredStock;
    }

    public void setExpiredStock(int expiredStock) {
        this.expiredStock = expiredStock;
    }
}
