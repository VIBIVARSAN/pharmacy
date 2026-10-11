package com.pharmacy.service;

import com.pharmacy.model.MedicineBatch;

import java.time.LocalDate;
import java.util.List;

/**
 * Decides which batches a requested quantity is taken from.
 * The dispensing service depends only on this interface, so the picking
 * policy can be swapped without touching the dispensing workflow.
 */
public interface StockAllocationStrategy {

    /** Quantity to take from one batch. */
    record Allocation(MedicineBatch batch, int quantity) {
    }

    /**
     * @param batches      candidate batches of ONE medicine (already locked by the caller)
     * @param quantity     units required
     * @param medicineName used in error messages
     * @param today        date used to decide whether a batch has expired
     * @return allocations whose quantities add up to exactly {@code quantity}
     * @throws com.pharmacy.exception.InsufficientStockException when eligible stock is not enough
     */
    List<Allocation> allocate(List<MedicineBatch> batches, int quantity, String medicineName, LocalDate today);
}
