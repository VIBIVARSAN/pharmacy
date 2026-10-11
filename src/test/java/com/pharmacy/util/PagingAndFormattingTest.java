package com.pharmacy.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PagingAndFormattingTest {
    private static final Map<String, String> SORTS = Map.of("name", "full_name", "code", "patient_code");

    @Test
    void sortingOnlyUsesWhitelistedColumns() {
        assertEquals("full_name DESC", new PageRequest(1, 10, "name", true, null).orderBy(SORTS, "id DESC"));
        assertEquals("patient_code ASC", new PageRequest(1, 10, "code", false, null).orderBy(SORTS, "id DESC"));
        assertEquals("id DESC", new PageRequest(1, 10, "name; DROP TABLE patients", false, null).orderBy(SORTS, "id DESC"));
        assertEquals("id DESC", new PageRequest(1, 10, null, false, null).orderBy(SORTS, "id DESC"));
    }

    @Test
    void pageAndSizeAreClampedToSaneValues() {
        PageRequest pr = new PageRequest(-5, 100000, null, false, "   ");
        assertEquals(1, pr.getPage());
        assertEquals(PageRequest.MAX_SIZE, pr.getSize());
        assertNull(pr.getQuery());
        assertEquals(20, new PageRequest(3, 10, null, false, null).getOffset());
    }

    @Test
    void pageMathIsCorrect() {
        Page<String> page = new Page<>(List.of("a", "b"), 3, 10, 22);
        assertEquals(3, page.getTotalPages());
        assertEquals(21, page.getFrom());
        assertEquals(22, page.getTo());
        assertTrue(page.isHasPrevious());
        assertFalse(page.isHasNext());
        Page<String> empty = new Page<>(List.of(), 1, 10, 0);
        assertEquals(1, empty.getTotalPages());
        assertEquals(0, empty.getFrom());
        assertTrue(empty.isEmpty());
    }

    @Test
    void moneyHelpersRoundHalfUp() {
        assertEquals(new BigDecimal("7.50"), Money.multiply(new BigDecimal("2.50"), 3));
        assertEquals(new BigDecimal("1.26"), Money.percentOf(new BigDecimal("10.05"), new BigDecimal("12.5")));
        assertEquals(new BigDecimal("0.00"), Money.scale(null));
        assertEquals("1,234.50", ViewFunctions.money(new BigDecimal("1234.5")));
        assertEquals("0.00", ViewFunctions.money(null));
    }

    @Test
    void viewFunctionsFormatDatesAndLabels() {
        assertEquals("09 Oct 2026", ViewFunctions.date(LocalDate.of(2026, 10, 9)));
        assertEquals("", ViewFunctions.date(null));
        assertEquals("Partially dispensed", ViewFunctions.label("PARTIALLY_DISPENSED"));
    }

    @Test
    void jsonWriterEscapesMarkupAndQuotes() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("label", "</script><b>\"x\"");
        data.put("values", List.of(new BigDecimal("1.50"), 2, true));
        data.put("none", null);
        assertEquals("{\"label\":\"\\u003c/script\\u003e\\u003cb\\u003e\\\"x\\\"\",\"values\":[1.50,2,true],\"none\":null}",
                JsonWriter.toJson(data));
    }
}
