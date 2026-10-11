package com.pharmacy.service;

import com.pharmacy.exception.InsufficientStockException;
import com.pharmacy.model.MedicineBatch;
import com.pharmacy.service.StockAllocationStrategy.Allocation;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FefoAllocationStrategyTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);
    private final StockAllocationStrategy strategy = new FefoAllocationStrategy();

    private static MedicineBatch batch(long id, String number, LocalDate expiry, int quantity) {
        MedicineBatch b = new MedicineBatch();
        b.setId(id);
        b.setBatchNumber(number);
        b.setExpiryDate(expiry);
        b.setQuantity(quantity);
        return b;
    }

    @Test
    void takesFromTheEarliestExpiringBatchFirst() {
        List<MedicineBatch> batches = List.of(
                batch(1, "LATE", TODAY.plusDays(300), 50),
                batch(2, "SOON", TODAY.plusDays(20), 50));
        List<Allocation> result = strategy.allocate(batches, 10, "Med", TODAY);
        assertEquals(1, result.size());
        assertEquals("SOON", result.get(0).batch().getBatchNumber());
        assertEquals(10, result.get(0).quantity());
    }

    @Test
    void spillsOverIntoTheNextBatchWhenTheFirstIsNotEnough() {
        List<MedicineBatch> batches = List.of(
                batch(1, "LATE", TODAY.plusDays(300), 50),
                batch(2, "SOON", TODAY.plusDays(20), 4),
                batch(3, "MID", TODAY.plusDays(90), 3));
        List<Allocation> result = strategy.allocate(batches, 10, "Med", TODAY);
        assertEquals(List.of("SOON", "MID", "LATE"), result.stream().map(a -> a.batch().getBatchNumber()).toList());
        assertEquals(List.of(4, 3, 3), result.stream().map(Allocation::quantity).toList());
    }

    @Test
    void neverUsesExpiredOrEmptyBatches() {
        List<MedicineBatch> batches = List.of(
                batch(1, "EXPIRED", TODAY.minusDays(1), 100),
                batch(2, "EMPTY", TODAY.plusDays(5), 0),
                batch(3, "GOOD", TODAY.plusDays(60), 6));
        List<Allocation> result = strategy.allocate(batches, 6, "Med", TODAY);
        assertEquals(1, result.size());
        assertEquals("GOOD", result.get(0).batch().getBatchNumber());
    }

    @Test
    void expiredStockDoesNotCountTowardsAvailability() {
        List<MedicineBatch> batches = List.of(
                batch(1, "EXPIRED", TODAY.minusDays(1), 100),
                batch(2, "GOOD", TODAY.plusDays(60), 6));
        InsufficientStockException e = assertThrows(InsufficientStockException.class,
                () -> strategy.allocate(batches, 7, "Med", TODAY));
        assertEquals(7, e.getRequested());
        assertEquals(6, e.getAvailable());
    }

    @Test
    void aBatchIsStillUsableOnItsExpiryDate() {
        List<Allocation> result = strategy.allocate(List.of(batch(1, "TODAY", TODAY, 5)), 5, "Med", TODAY);
        assertEquals(5, result.get(0).quantity());
    }

    @Test
    void equalExpiryDatesAreUsedInBatchIdOrder() {
        List<MedicineBatch> batches = List.of(batch(9, "SECOND", TODAY.plusDays(30), 5), batch(4, "FIRST", TODAY.plusDays(30), 5));
        List<Allocation> result = strategy.allocate(batches, 7, "Med", TODAY);
        assertEquals("FIRST", result.get(0).batch().getBatchNumber());
        assertEquals(5, result.get(0).quantity());
        assertEquals(2, result.get(1).quantity());
    }

    @Test
    void rejectsNonPositiveQuantities() {
        assertThrows(IllegalArgumentException.class, () -> strategy.allocate(List.of(), 0, "Med", TODAY));
    }
}
