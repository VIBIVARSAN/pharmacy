package com.pharmacy.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelBehaviourTest {

    @Test
    void personSubclassesAnswerTheSameQuestionsDifferently() {
        Patient patient = new Patient(1L, "PAT-000001", "Meera Nair");
        Doctor doctor = new Doctor(1L, "DOC-000001", "Asha Rao");
        User user = new User();
        user.setUsername("admin");
        user.setFullName("Site Admin");
        List<Person> people = List.of(patient, doctor, user);           // one list, three concrete types
        assertEquals(List.of("PAT-000001", "DOC-000001", "admin"), people.stream().map(Person::getCode).toList());
        assertEquals(List.of("Meera Nair", "Dr. Asha Rao", "Site Admin"), people.stream().map(Person::getDisplayName).toList());
        assertEquals(List.of("PATIENT", "DOCTOR", "USER"), people.stream().map(Auditable::getAuditType).toList());
    }

    @Test
    void doctorTitleIsNotDuplicated() {
        assertEquals("Dr. Rao", new Doctor(1L, "DOC-1", "Dr. Rao").getDisplayName());
    }

    @Test
    void entitiesAreEqualOnlyWithTheSameTypeAndId() {
        assertEquals(new Patient(5L, "A", "x"), new Patient(5L, "B", "y"));
        assertNotEquals(new Patient(5L, "A", "x"), new Patient(6L, "A", "x"));
        assertNotEquals((Object) new Patient(5L, "A", "x"), new Doctor(5L, "A", "x"));
        assertNotEquals(new Patient(), new Patient());                   // unsaved objects are never equal
        assertTrue(new Patient().isNew());
    }

    @Test
    void patientAgeComesFromDateOfBirth() {
        Patient p = new Patient();
        assertNull(p.getAge());
        p.setDateOfBirth(LocalDate.now().minusYears(30).minusDays(1));
        assertEquals(30, p.getAge());
    }

    @Test
    void prescriptionStatusKnowsWhatIsAllowed() {
        assertTrue(PrescriptionStatus.DRAFT.isEditable());
        assertFalse(PrescriptionStatus.DRAFT.isDispensable());
        assertTrue(PrescriptionStatus.ISSUED.isDispensable());
        assertTrue(PrescriptionStatus.PARTIALLY_DISPENSED.isDispensable());
        assertFalse(PrescriptionStatus.PARTIALLY_DISPENSED.isEditable());
        assertFalse(PrescriptionStatus.COMPLETED.isCancellable());
        assertFalse(PrescriptionStatus.CANCELLED.isDispensable());
    }

    @Test
    void prescriptionIsCompleteOnlyWhenEveryItemIsFullyDispensed() {
        Prescription p = new Prescription();
        assertFalse(p.isFullyDispensed());
        PrescriptionItem a = new PrescriptionItem();
        a.setQuantity(10);
        a.setDispensedQuantity(10);
        PrescriptionItem b = new PrescriptionItem();
        b.setQuantity(5);
        b.setDispensedQuantity(2);
        p.addItem(a);
        p.addItem(b);
        assertEquals(3, b.getRemainingQuantity());
        assertFalse(p.isFullyDispensed());
        b.setDispensedQuantity(5);
        assertTrue(p.isFullyDispensed());
    }

    @Test
    void paymentStatusIsDerivedFromAmounts() {
        BigDecimal total = new BigDecimal("68.25");
        assertEquals(PaymentStatus.UNPAID, PaymentStatus.of(new BigDecimal("0.00"), total));
        assertEquals(PaymentStatus.PARTIALLY_PAID, PaymentStatus.of(new BigDecimal("20.00"), total));
        assertEquals(PaymentStatus.PAID, PaymentStatus.of(new BigDecimal("68.25"), total));
    }

    @Test
    void stockTransactionTypesKnowTheirDirection() {
        assertEquals(15, StockTransactionType.STOCK_IN.apply(10, 5));
        assertEquals(15, StockTransactionType.ADJUSTMENT_IN.apply(10, 5));
        assertEquals(5, StockTransactionType.STOCK_OUT.apply(10, 5));
        assertEquals(5, StockTransactionType.ADJUSTMENT_OUT.apply(10, 5));
        assertEquals(5, StockTransactionType.DISPENSE.apply(10, 5));
    }

    @Test
    void batchExpiryRules() {
        MedicineBatch b = new MedicineBatch();
        LocalDate today = LocalDate.now();
        b.setExpiryDate(today);
        assertFalse(b.isExpired());                       // usable up to and including the expiry date
        assertTrue(b.isExpiredOn(today.plusDays(1)));
        assertTrue(b.isNearExpiry(90));
        b.setExpiryDate(today.plusDays(200));
        assertFalse(b.isNearExpiry(90));
        b.setExpiryDate(today.minusDays(1));
        assertTrue(b.isExpired());
        assertFalse(b.isNearExpiry(90));                  // expired is reported as expired, not "near expiry"
    }

    @Test
    void medicineLabelAndLowStock() {
        Medicine m = new Medicine();
        m.setName("Paracetamol");
        m.setStrength("500 mg");
        m.setDosageForm("Tablet");
        m.setReorderLevel(10);
        m.setAvailableStock(10);
        assertEquals("Paracetamol 500 mg (Tablet)", m.getLabel());
        assertTrue(m.isLowStock());
        m.setAvailableStock(11);
        assertFalse(m.isLowStock());
    }

    @Test
    void dispensingItemLineTotalUsesExactArithmetic() {
        DispensingItem item = new DispensingItem();
        item.setQuantity(3);
        item.setUnitPrice(new BigDecimal("0.10"));
        assertEquals(new BigDecimal("0.30"), item.getLineTotal());
    }
}
