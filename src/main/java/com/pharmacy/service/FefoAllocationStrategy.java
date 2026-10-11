package com.pharmacy.service;

import com.pharmacy.exception.InsufficientStockException;
import com.pharmacy.model.MedicineBatch;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * First-Expiry-First-Out: use the batch that expires soonest first, so stock
 * is not left to go out of date. Expired and empty batches are never used.
 */
public class FefoAllocationStrategy implements StockAllocationStrategy {

    @Override
    public List<Allocation> allocate(List<MedicineBatch> batches, int quantity, String medicineName, LocalDate today) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }
        List<MedicineBatch> eligible = batches.stream()
                .filter(b -> b.getQuantity() > 0 && !b.isExpiredOn(today))
                .sorted(Comparator.comparing(MedicineBatch::getExpiryDate)
                        .thenComparing(MedicineBatch::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        int available = eligible.stream().mapToInt(MedicineBatch::getQuantity).sum();
        if (available < quantity) {
            throw new InsufficientStockException(medicineName, quantity, available);
        }
        List<Allocation> result = new ArrayList<>();
        int remaining = quantity;
        for (MedicineBatch batch : eligible) {
            if (remaining == 0) {
                break;
            }
            int take = Math.min(remaining, batch.getQuantity());
            result.add(new Allocation(batch, take));
            remaining -= take;
        }
        return result;
    }
}
