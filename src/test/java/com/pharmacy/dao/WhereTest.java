package com.pharmacy.dao;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WhereTest {

    @Test
    void buildsConditionsWithBindParametersOnly() {
        Where where = new Where().search("meera", "full_name", "phone").andIf(true, "active = ?", true).andIf(false, "ignored = ?", 1);
        assertEquals(" WHERE (full_name LIKE ? OR phone LIKE ?) AND (active = ?)", where.sql());
        assertEquals(List.of("%meera%", "%meera%", true), where.params());
    }

    @Test
    void userTextNeverBecomesPartOfTheSql() {
        String attack = "' OR '1'='1";
        Where where = new Where().search(attack, "full_name");
        assertFalse(where.sql().contains(attack));
        assertEquals(List.of("%" + attack + "%"), where.params());
    }

    @Test
    void likeWildcardsTypedByTheUserAreEscaped() {
        assertEquals("100\\% \\_sure\\\\", Where.escapeLike("100% _sure\\"));
    }

    @Test
    void blankSearchAddsNothing() {
        assertEquals("", new Where().search("  ", "full_name").sql());
    }
}
