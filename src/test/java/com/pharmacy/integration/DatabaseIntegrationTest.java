package com.pharmacy.integration;

import com.pharmacy.exception.AuthorizationException;
import com.pharmacy.exception.BusinessRuleException;
import com.pharmacy.exception.DuplicateOperationException;
import com.pharmacy.exception.InsufficientStockException;
import com.pharmacy.exception.ValidationException;
import com.pharmacy.model.Dispensing;
import com.pharmacy.model.Doctor;
import com.pharmacy.model.Gender;
import com.pharmacy.model.Invoice;
import com.pharmacy.model.Medicine;
import com.pharmacy.model.MedicineBatch;
import com.pharmacy.model.Patient;
import com.pharmacy.model.PaymentStatus;
import com.pharmacy.model.Prescription;
import com.pharmacy.model.PrescriptionItem;
import com.pharmacy.model.PrescriptionStatus;
import com.pharmacy.model.Role;
import com.pharmacy.model.User;
import com.pharmacy.service.AppContext;
import com.pharmacy.util.BCryptPasswordEncoder;
import com.pharmacy.util.ConnectionProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * JDBC integration tests against a real MySQL database: transactions,
 * rollback, row locking under concurrency, FEFO, expiry and duplicate
 * prevention.
 *
 * <p>They are SKIPPED unless these environment variables are set:</p>
 * <pre>
 *   TEST_DB_URL=jdbc:mysql://localhost:3306/pharmacy_test
 *   TEST_DB_USER=...
 *   TEST_DB_PASSWORD=...
 * </pre>
 * The database name must contain "test": every table in it is dropped and
 * re-created from database.sql before the tests run.
 */
class DatabaseIntegrationTest {
    private static AppContext app;
    private static ConnectionProvider provider;
    private static User admin;
    private static User pharmacist;
    private static Patient patient;
    private static Doctor doctor;
    private static final AtomicInteger COUNTER = new AtomicInteger();

    // ------------------------------------------------------------------ set-up

    @BeforeAll
    static void createSchemaAndBaseData() throws Exception {
        String url = System.getenv("TEST_DB_URL");
        assumeTrue(url != null && !url.isBlank(), "TEST_DB_URL is not set - database integration tests skipped");
        if (!url.toLowerCase().contains("test")) {
            throw new IllegalStateException("Refusing to run: TEST_DB_URL must point to a database whose name contains 'test'.");
        }
        String user = System.getenv("TEST_DB_USER");
        String password = System.getenv("TEST_DB_PASSWORD");
        provider = () -> DriverManager.getConnection(url, user, password);

        try (Connection con = provider.getConnection(); Statement st = con.createStatement()) {
            st.execute("SET FOREIGN_KEY_CHECKS = 0");
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = st.executeQuery("SHOW TABLES")) {
                while (rs.next()) {
                    tables.add(rs.getString(1));
                }
            }
            for (String table : tables) {
                st.execute("DROP TABLE `" + table + "`");
            }
            st.execute("SET FOREIGN_KEY_CHECKS = 1");
            for (String statement : readSchema()) {
                st.execute(statement);
            }
        }

        admin = insertUser("it_admin", Role.ADMIN);
        pharmacist = insertUser("it_pharmacist", Role.PHARMACIST);
        app = new AppContext(provider, new BCryptPasswordEncoder(4));
        app.getSettingsService().update(admin, Map.of("tax_rate_percent", "5", "discounts_enabled", "true"));

        Patient p = new Patient();
        p.setFullName("Integration Patient");
        p.setGender(Gender.OTHER);
        p.setPhone("9000000001");
        patient = app.getPatientService().create(admin, p);

        Doctor d = new Doctor();
        d.setFullName("Integration Doctor");
        d.setSpecialization("General");
        d.setPhone("9000000002");
        doctor = app.getDoctorService().create(admin, d);
    }

    @AfterAll
    static void closePool() {
        if (app != null) {
            app.close();
        }
    }

    /** database.sql without comments and without the CREATE DATABASE / USE lines. */
    private static List<String> readSchema() throws Exception {
        Path file = Path.of(System.getProperty("project.dir", "."), "database.sql");
        StringBuilder sql = new StringBuilder();
        for (String line : Files.readAllLines(file)) {
            if (!line.trim().startsWith("--")) {
                sql.append(line).append('\n');
            }
        }
        List<String> statements = new ArrayList<>();
        for (String statement : sql.toString().split(";")) {
            String s = statement.trim();
            if (!s.isEmpty() && !s.toUpperCase().startsWith("CREATE DATABASE") && !s.toUpperCase().startsWith("USE ")) {
                statements.add(s);
            }
        }
        return statements;
    }

    private static User insertUser(String username, Role role) throws SQLException {
        try (Connection con = provider.getConnection(); PreparedStatement ps = con.prepareStatement(
                "INSERT INTO users (username, password_hash, full_name, role) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, new BCryptPasswordEncoder(4).hash("Test-pass-1"));
            ps.setString(3, username);
            ps.setString(4, role.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                User u = new User();
                u.setId(keys.getLong(1));
                u.setUsername(username);
                u.setFullName(username);
                u.setRole(role);
                u.setActive(true);
                return u;
            }
        }
    }

    // ----------------------------------------------------------------- helpers

    private static Medicine newMedicine() {
        Medicine m = new Medicine();
        m.setName("IT Medicine " + COUNTER.incrementAndGet());
        m.setCategory("Test");
        m.setDosageForm("Tablet");
        m.setUnit("tablet");
        return app.getMedicineService().create(admin, m, 0);
    }

    private static MedicineBatch receive(Medicine m, String batchNumber, int daysToExpiry, int quantity, String price) {
        MedicineBatch b = new MedicineBatch();
        b.setMedicineId(m.getId());
        b.setBatchNumber(batchNumber);
        b.setExpiryDate(LocalDate.now().plusDays(daysToExpiry));
        b.setPurchasePrice(new BigDecimal("1.00"));
        b.setSellingPrice(new BigDecimal(price));
        b.setReceivedDate(LocalDate.now());
        return app.getInventoryService().stockIn(pharmacist, b, quantity);
    }

    /** Puts an already-expired batch straight into the table (the application refuses to receive one). */
    private static long insertExpiredBatch(Medicine m, int quantity) throws SQLException {
        try (Connection con = provider.getConnection(); PreparedStatement ps = con.prepareStatement(
                "INSERT INTO medicine_batches (medicine_id, batch_number, expiry_date, purchase_price, selling_price, quantity, received_date) "
                        + "VALUES (?, 'EXPIRED', DATE_SUB(CURDATE(), INTERVAL 10 DAY), 1.00, 2.00, ?, DATE_SUB(CURDATE(), INTERVAL 400 DAY))",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, m.getId());
            ps.setInt(2, quantity);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private static Prescription prescribe(Object... medicineAndQuantity) {
        Prescription p = new Prescription();
        p.setPatient(new Patient(patient.getId(), null, null));
        p.setDoctor(new Doctor(doctor.getId(), null, null));
        p.setPrescriptionDate(LocalDate.now());
        for (int i = 0; i < medicineAndQuantity.length; i += 2) {
            PrescriptionItem item = new PrescriptionItem();
            item.setMedicineId(((Medicine) medicineAndQuantity[i]).getId());
            item.setDosage("1 unit");
            item.setFrequency("Daily");
            item.setQuantity((Integer) medicineAndQuantity[i + 1]);
            p.addItem(item);
        }
        Prescription saved = app.getPrescriptionService().create(admin, p, true);
        return app.getPrescriptionService().get(admin, saved.getId());
    }

    private static Dispensing dispense(Prescription p, int... quantities) {
        Map<Long, Integer> map = new java.util.HashMap<>();
        for (int i = 0; i < quantities.length; i++) {
            map.put(p.getItems().get(i).getId(), quantities[i]);
        }
        return app.getDispensingService().dispense(pharmacist, p.getId(), map, token(), null);
    }

    private static String token() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static int stock(Medicine m) {
        return app.getMedicineService().get(admin, m.getId()).getAvailableStock();
    }

    private static long count(String sql, Object... params) throws SQLException {
        try (Connection con = provider.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    /** Runs the tasks at the same instant on separate threads / connections and returns results or exceptions. */
    private static List<Object> runTogether(List<Callable<Object>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Object>> futures = new ArrayList<>();
        for (Callable<Object> task : tasks) {
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    return task.call();
                } catch (RuntimeException e) {
                    return e;
                }
            }));
        }
        start.countDown();
        List<Object> results = new ArrayList<>();
        for (Future<Object> f : futures) {
            results.add(f.get(60, TimeUnit.SECONDS));
        }
        pool.shutdown();
        return results;
    }

    // ------------------------------------------------------------------- tests

    @Test
    void generatedCodesAreUniqueUnderConcurrency() throws Exception {
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            int n = i;
            tasks.add(() -> {
                Patient p = new Patient();
                p.setFullName("Concurrent Patient " + n);
                p.setGender(Gender.MALE);
                p.setPhone("91111000" + String.format("%02d", n));
                return app.getPatientService().create(pharmacist, p).getPatientCode();
            });
        }
        Set<Object> codes = new HashSet<>(runTogether(tasks));
        assertEquals(12, codes.size(), "every patient must receive a different ID: " + codes);
        assertTrue(codes.stream().allMatch(c -> c instanceof String s && s.matches("PAT-\\d{6}")));
    }

    @Test
    void dispensingFollowsFefoAndWritesTheLedger() throws Exception {
        Medicine m = newMedicine();
        receive(m, "LATE", 300, 20, "3.00");
        receive(m, "SOON", 30, 10, "2.50");
        Prescription p = prescribe(m, 12);

        Dispensing d = dispense(p, 12);

        Dispensing saved = app.getDispensingService().get(pharmacist, d.getId());
        assertEquals(2, saved.getItems().size());
        assertEquals("SOON", saved.getItems().get(0).getBatchNumber());
        assertEquals(10, saved.getItems().get(0).getQuantity());
        assertEquals("LATE", saved.getItems().get(1).getBatchNumber());
        assertEquals(2, saved.getItems().get(1).getQuantity());
        assertEquals(new BigDecimal("31.00"), saved.getTotal());          // 10 x 2.50 + 2 x 3.00
        assertEquals(18, stock(m));
        assertEquals(PrescriptionStatus.COMPLETED, app.getPrescriptionService().get(admin, p.getId()).getStatus());
        assertEquals(2, count("SELECT COUNT(*) FROM stock_transactions WHERE medicine_id = ? AND txn_type = 'DISPENSE'", m.getId()));
        // ledger and batch quantities agree
        assertEquals(18, count("SELECT SUM(CASE WHEN txn_type IN ('STOCK_IN','ADJUSTMENT_IN') THEN quantity ELSE -quantity END) "
                + "FROM stock_transactions WHERE medicine_id = ?", m.getId()));
    }

    @Test
    void expiredBatchesAreNeverDispensed() throws Exception {
        Medicine m = newMedicine();
        long expiredBatchId = insertExpiredBatch(m, 100);
        receive(m, "GOOD", 60, 5, "2.00");
        assertEquals(5, stock(m), "expired stock must not count as usable");

        Prescription p = prescribe(m, 8);
        InsufficientStockException e = assertThrows(InsufficientStockException.class, () -> dispense(p, 6));
        assertEquals(5, e.getAvailable());

        dispense(p, 5);
        assertEquals(0, stock(m));
        assertEquals(100, count("SELECT quantity FROM medicine_batches WHERE id = ?", expiredBatchId), "expired batch untouched");
        assertEquals(PrescriptionStatus.PARTIALLY_DISPENSED, app.getPrescriptionService().get(admin, p.getId()).getStatus());
    }

    @Test
    void aFailedDispensingRollsBackEverything() throws Exception {
        Medicine plenty = newMedicine();
        Medicine scarce = newMedicine();
        receive(plenty, "P1", 100, 50, "1.00");
        receive(scarce, "S1", 100, 2, "1.00");
        Prescription p = prescribe(plenty, 10, scarce, 5);
        long dispensingsBefore = count("SELECT COUNT(*) FROM dispensings");
        long ledgerBefore = count("SELECT COUNT(*) FROM stock_transactions");
        long sequenceBefore = count("SELECT next_val FROM sequences WHERE name = 'DISPENSING'");

        // The first item could be served, the second cannot: nothing at all may be saved.
        assertThrows(InsufficientStockException.class, () -> dispense(p, 10, 5));

        assertEquals(50, stock(plenty), "stock of the first item must be restored by the rollback");
        assertEquals(2, stock(scarce));
        assertEquals(dispensingsBefore, count("SELECT COUNT(*) FROM dispensings"));
        assertEquals(ledgerBefore, count("SELECT COUNT(*) FROM stock_transactions"));
        assertEquals(sequenceBefore, count("SELECT next_val FROM sequences WHERE name = 'DISPENSING'"));
        assertEquals(PrescriptionStatus.ISSUED, app.getPrescriptionService().get(admin, p.getId()).getStatus());
    }

    @Test
    void concurrentDispensingNeverOversells() throws Exception {
        Medicine m = newMedicine();
        receive(m, "ONLY", 100, 10, "1.00");
        Prescription first = prescribe(m, 8);
        Prescription second = prescribe(m, 8);

        List<Object> results = runTogether(List.of(() -> dispense(first, 8), () -> dispense(second, 8)));

        long succeeded = results.stream().filter(r -> r instanceof Dispensing).count();
        long refused = results.stream().filter(r -> r instanceof InsufficientStockException).count();
        assertEquals(1, succeeded, "exactly one of the two requests can be served: " + results);
        assertEquals(1, refused, "the other must be refused for lack of stock: " + results);
        assertEquals(2, stock(m));
        assertEquals(0, count("SELECT COUNT(*) FROM medicine_batches WHERE quantity < 0"));
    }

    @Test
    void theSameFormSubmittedTwiceDeductsStockOnce() throws Exception {
        Medicine m = newMedicine();
        receive(m, "ONLY", 100, 20, "1.00");
        Prescription p = prescribe(m, 10);
        Map<Long, Integer> quantities = Map.of(p.getItems().get(0).getId(), 4);
        String sameToken = token();

        Callable<Object> submit = () -> app.getDispensingService().dispense(pharmacist, p.getId(), quantities, sameToken, null);
        List<Object> results = runTogether(List.of(submit, submit, submit));

        assertEquals(1, results.stream().filter(r -> r instanceof Dispensing).count(), results.toString());
        assertEquals(2, results.stream().filter(r -> r instanceof DuplicateOperationException).count(), results.toString());
        assertEquals(16, stock(m));
        assertEquals(1, count("SELECT COUNT(*) FROM dispensings WHERE prescription_id = ?", p.getId()));
    }

    @Test
    void dispensingIsLimitedToTheRemainingPrescribedQuantity() {
        Medicine m = newMedicine();
        receive(m, "ONLY", 100, 50, "1.00");
        Prescription p = prescribe(m, 10);
        dispense(p, 6);
        assertThrows(ValidationException.class, () -> dispense(p, 5));
        dispense(p, 4);
        assertEquals(40, stock(m));
        BusinessRuleException e = assertThrows(BusinessRuleException.class, () -> dispense(p, 1));
        assertTrue(e.getMessage().contains("completed"));
    }

    @Test
    void stockCanNeverGoNegative() throws Exception {
        Medicine m = newMedicine();
        MedicineBatch batch = receive(m, "ONLY", 100, 5, "1.00");
        assertThrows(InsufficientStockException.class,
                () -> app.getInventoryService().stockOut(pharmacist, batch.getId(), 6, "too many"));
        assertEquals(5, stock(m));
        // even a direct UPDATE is stopped by the CHECK constraint
        assertThrows(SQLException.class, () -> {
            try (Connection con = provider.getConnection(); Statement st = con.createStatement()) {
                st.executeUpdate("UPDATE medicine_batches SET quantity = -1 WHERE id = " + batch.getId());
            }
        });
    }

    @Test
    void onlyAdministratorsMayAdjustStock() {
        Medicine m = newMedicine();
        MedicineBatch batch = receive(m, "ONLY", 100, 5, "1.00");
        assertThrows(AuthorizationException.class,
                () -> app.getInventoryService().adjust(pharmacist, batch.getId(), true, 10, "not allowed"));
        app.getInventoryService().adjust(admin, batch.getId(), true, 10, "count correction");
        assertEquals(15, stock(m));
    }

    @Test
    void invoiceTotalsAreCalculatedAndStoredConsistently() throws Exception {
        Medicine m = newMedicine();
        receive(m, "ONLY", 100, 20, "7.25");
        Prescription p = prescribe(m, 4);
        Dispensing d = app.getDispensingService().get(pharmacist, dispense(p, 4).getId());
        long itemId = d.getItems().get(0).getId();

        Invoice created = app.getBillingService().createInvoice(pharmacist, d.getId(), Map.of(itemId, new BigDecimal("4.00")),
                "Cash", new BigDecimal("10.00"), null);
        Invoice invoice = app.getBillingService().get(pharmacist, created.getId());

        assertEquals(new BigDecimal("29.00"), invoice.getSubtotal());       // 4 x 7.25
        assertEquals(new BigDecimal("4.00"), invoice.getDiscountAmount());
        assertEquals(new BigDecimal("1.25"), invoice.getTaxAmount());       // 5% of 25.00
        assertEquals(new BigDecimal("26.25"), invoice.getGrandTotal());
        assertEquals(PaymentStatus.PARTIALLY_PAID, invoice.getPaymentStatus());
        assertEquals(new BigDecimal("16.25"), invoice.getBalanceDue());
        assertEquals(16, stock(m), "billing must not deduct stock a second time");
        // stored header totals equal the sum of the stored lines
        assertEquals(1, count("SELECT COUNT(*) FROM invoices i WHERE i.id = ? AND i.subtotal - i.discount_amount = "
                + "(SELECT SUM(line_total) FROM invoice_items WHERE invoice_id = i.id)", invoice.getId()));

        assertThrows(ValidationException.class,
                () -> app.getBillingService().addPayment(pharmacist, invoice.getId(), new BigDecimal("16.26"), "Cash", null));
        app.getBillingService().addPayment(pharmacist, invoice.getId(), new BigDecimal("16.25"), "Card", "ref");
        assertEquals(PaymentStatus.PAID, app.getBillingService().get(pharmacist, invoice.getId()).getPaymentStatus());
        assertThrows(BusinessRuleException.class, () -> app.getBillingService().cancel(admin, invoice.getId(), "has payments"));
    }

    @Test
    void aFailedInvoiceLeavesNothingBehind() throws Exception {
        Medicine m = newMedicine();
        receive(m, "ONLY", 100, 20, "2.00");
        Prescription p = prescribe(m, 2);
        Dispensing d = dispense(p, 2);
        long invoicesBefore = count("SELECT COUNT(*) FROM invoices");
        long itemsBefore = count("SELECT COUNT(*) FROM invoice_items");

        // The invoice and its items are inserted first; the invalid payment then aborts the whole transaction.
        assertThrows(ValidationException.class, () -> app.getBillingService().createInvoice(pharmacist, d.getId(), Map.of(),
                "NotAMethod", new BigDecimal("1.00"), null));

        assertEquals(invoicesBefore, count("SELECT COUNT(*) FROM invoices"));
        assertEquals(itemsBefore, count("SELECT COUNT(*) FROM invoice_items"));
    }

    @Test
    void aDispensingCannotBeBilledTwiceEvenConcurrently() throws Exception {
        Medicine m = newMedicine();
        receive(m, "ONLY", 100, 20, "2.00");
        Prescription p = prescribe(m, 3);
        Dispensing d = dispense(p, 3);

        Callable<Object> bill = () -> app.getBillingService().createInvoice(pharmacist, d.getId(), Map.of(), null, null, null);
        List<Object> results = runTogether(List.of(bill, bill, bill));

        assertEquals(1, results.stream().filter(r -> r instanceof Invoice).count(), results.toString());
        assertEquals(2, results.stream().filter(r -> r instanceof DuplicateOperationException).count(), results.toString());
        assertEquals(1, count("SELECT COUNT(*) FROM invoices WHERE dispensing_id = ?", d.getId()));

        // After an administrator cancels the unpaid invoice it stays on record and billing is possible again.
        Invoice first = (Invoice) results.stream().filter(r -> r instanceof Invoice).findFirst().orElseThrow();
        assertThrows(AuthorizationException.class, () -> app.getBillingService().cancel(pharmacist, first.getId(), "no"));
        app.getBillingService().cancel(admin, first.getId(), "wrong visit");
        Invoice second = app.getBillingService().createInvoice(pharmacist, d.getId(), Map.of(), null, null, null);
        assertNotEquals(first.getId(), second.getId());
        assertEquals(2, count("SELECT COUNT(*) FROM invoices WHERE dispensing_id = ?", d.getId()));
        assertEquals(1, count("SELECT COUNT(*) FROM invoices WHERE dispensing_id = ? AND status = 'CANCELLED'", d.getId()));
    }

    @Test
    void importantOperationsAreAudited() throws Exception {
        Medicine m = newMedicine();
        receive(m, "ONLY", 100, 5, "1.00");
        Prescription p = prescribe(m, 1);
        dispense(p, 1);
        assertTrue(count("SELECT COUNT(*) FROM audit_logs WHERE action = 'MEDICINE_CREATED' AND entity_id = ?", m.getId()) >= 1);
        assertTrue(count("SELECT COUNT(*) FROM audit_logs WHERE action = 'PRESCRIPTION_CREATED' AND entity_id = ?", p.getId()) >= 1);
        assertTrue(count("SELECT COUNT(*) FROM audit_logs WHERE action = 'DISPENSED' AND user_id = ?", pharmacist.getId()) >= 1);
        assertEquals(0, count("SELECT COUNT(*) FROM audit_logs WHERE description LIKE '%$2a$%' OR description LIKE '%Test-pass-1%'"));
    }
}
