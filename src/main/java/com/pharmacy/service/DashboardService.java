package com.pharmacy.service;

import com.pharmacy.dao.BatchDao;
import com.pharmacy.dao.DashboardDao;
import com.pharmacy.dao.DispensingDao;
import com.pharmacy.dao.DoctorDao;
import com.pharmacy.dao.InvoiceDao;
import com.pharmacy.dao.MedicineDao;
import com.pharmacy.dao.PatientDao;
import com.pharmacy.dao.PrescriptionDao;
import com.pharmacy.model.Dispensing;
import com.pharmacy.model.Invoice;
import com.pharmacy.model.Prescription;
import com.pharmacy.util.TransactionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Collects the dashboard figures. Nothing here is hardcoded; an empty database yields zeros. */
public class DashboardService extends BaseService {

    /** Read-only snapshot shown on the dashboard. */
    public static class Summary {
        private long patients;
        private long doctors;
        private long prescriptions;
        private long activeMedicines;
        private long lowStock;
        private long nearExpiry;
        private long expired;
        private int nearExpiryDays;
        private BigDecimal salesToday;
        private BigDecimal salesMonth;
        private List<Prescription> recentPrescriptions;
        private List<Dispensing> recentDispensings;
        private List<Invoice> recentInvoices;

        public long getPatients() {
            return patients;
        }

        public long getDoctors() {
            return doctors;
        }

        public long getPrescriptions() {
            return prescriptions;
        }

        public long getActiveMedicines() {
            return activeMedicines;
        }

        public long getLowStock() {
            return lowStock;
        }

        public long getNearExpiry() {
            return nearExpiry;
        }

        public long getExpired() {
            return expired;
        }

        public int getNearExpiryDays() {
            return nearExpiryDays;
        }

        public BigDecimal getSalesToday() {
            return salesToday;
        }

        public BigDecimal getSalesMonth() {
            return salesMonth;
        }

        public List<Prescription> getRecentPrescriptions() {
            return recentPrescriptions;
        }

        public List<Dispensing> getRecentDispensings() {
            return recentDispensings;
        }

        public List<Invoice> getRecentInvoices() {
            return recentInvoices;
        }
    }

    private final DashboardDao dashboardDao;
    private final PatientDao patientDao;
    private final DoctorDao doctorDao;
    private final PrescriptionDao prescriptionDao;
    private final MedicineDao medicineDao;
    private final BatchDao batchDao;
    private final DispensingDao dispensingDao;
    private final InvoiceDao invoiceDao;
    private final SettingsService settings;

    public DashboardService(TransactionManager tx, AuditService audit, DashboardDao dashboardDao, PatientDao patientDao,
                            DoctorDao doctorDao, PrescriptionDao prescriptionDao, MedicineDao medicineDao, BatchDao batchDao,
                            DispensingDao dispensingDao, InvoiceDao invoiceDao, SettingsService settings) {
        super(tx, audit);
        this.dashboardDao = dashboardDao;
        this.patientDao = patientDao;
        this.doctorDao = doctorDao;
        this.prescriptionDao = prescriptionDao;
        this.medicineDao = medicineDao;
        this.batchDao = batchDao;
        this.dispensingDao = dispensingDao;
        this.invoiceDao = invoiceDao;
        this.settings = settings;
    }

    public Summary getSummary() {
        LocalDate today = LocalDate.now();
        int nearDays = settings.getNearExpiryDays();
        return tx.read(con -> {
            Summary s = new Summary();
            s.patients = patientDao.countAll(con);
            s.doctors = doctorDao.countAll(con);
            s.prescriptions = prescriptionDao.countAll(con);
            s.activeMedicines = medicineDao.countActive(con);
            s.lowStock = medicineDao.countLowStock(con, today);
            s.nearExpiry = batchDao.countNearExpiry(con, today, nearDays);
            s.expired = batchDao.countExpired(con, today);
            s.nearExpiryDays = nearDays;
            s.salesToday = dashboardDao.salesBetween(con, today, today.plusDays(1));
            s.salesMonth = dashboardDao.salesBetween(con, today.withDayOfMonth(1), today.plusDays(1));
            s.recentPrescriptions = prescriptionDao.findRecent(con, 5);
            s.recentDispensings = dispensingDao.findRecent(con, 5);
            s.recentInvoices = invoiceDao.findRecent(con, 5);
            return s;
        });
    }

    /** Counts used by the notification bell. */
    public Map<String, Object> getAlerts() {
        LocalDate today = LocalDate.now();
        int nearDays = settings.getNearExpiryDays();
        return tx.read(con -> {
            Map<String, Object> alerts = new LinkedHashMap<>();
            alerts.put("lowStock", medicineDao.countLowStock(con, today));
            alerts.put("nearExpiry", batchDao.countNearExpiry(con, today, nearDays));
            alerts.put("expired", batchDao.countExpired(con, today));
            alerts.put("nearExpiryDays", nearDays);
            return alerts;
        });
    }

    /** Daily sales for the last {@code days} days, including days with zero sales. */
    public Map<String, Object> getSalesTrend(int days) {
        int span = Math.min(Math.max(days, 1), 366);
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(span - 1L);
        Map<LocalDate, BigDecimal> byDay = tx.read(con -> dashboardDao.salesByDay(con, from, today.plusDays(1)));
        List<String> labels = new ArrayList<>();
        List<BigDecimal> values = new ArrayList<>();
        boolean any = false;
        for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) {
            BigDecimal v = byDay.getOrDefault(d, BigDecimal.ZERO);
            any |= v.signum() != 0;
            labels.add(d.toString());
            values.add(v);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("labels", labels);
        result.put("values", values);
        result.put("empty", !any);
        return result;
    }

    public Map<String, Object> getStockByCategory() {
        Map<String, Long> data = tx.read(con -> dashboardDao.stockByCategory(con, LocalDate.now()));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("labels", new ArrayList<>(data.keySet()));
        result.put("values", new ArrayList<>(data.values()));
        result.put("empty", data.isEmpty());
        return result;
    }
}
