package com.pharmacy.service;

import com.pharmacy.dao.AuditLogDao;
import com.pharmacy.dao.BatchDao;
import com.pharmacy.dao.DashboardDao;
import com.pharmacy.dao.DispensingDao;
import com.pharmacy.dao.DoctorDao;
import com.pharmacy.dao.InvoiceDao;
import com.pharmacy.dao.MedicineDao;
import com.pharmacy.dao.PatientDao;
import com.pharmacy.dao.PrescriptionDao;
import com.pharmacy.dao.ReportDao;
import com.pharmacy.dao.SequenceDao;
import com.pharmacy.dao.SettingDao;
import com.pharmacy.dao.StockTransactionDao;
import com.pharmacy.dao.SupplierDao;
import com.pharmacy.dao.UserDao;
import com.pharmacy.util.BCryptPasswordEncoder;
import com.pharmacy.util.ConnectionProvider;
import com.pharmacy.util.PasswordEncoder;
import com.pharmacy.util.TransactionManager;

/**
 * Builds every DAO and service once and wires them together by constructor
 * injection (plain Java, no framework). The web layer obtains services from
 * here; tests can build their own instance on a test database.
 */
public class AppContext {
    private final ConnectionProvider connectionProvider;
    private final AuditService auditService;
    private final SettingsService settingsService;
    private final AuthService authService;
    private final UserService userService;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final SupplierService supplierService;
    private final MedicineService medicineService;
    private final InventoryService inventoryService;
    private final PrescriptionService prescriptionService;
    private final DispensingService dispensingService;
    private final BillingService billingService;
    private final ReportService reportService;
    private final DashboardService dashboardService;

    public AppContext(ConnectionProvider connectionProvider) {
        this(connectionProvider, new BCryptPasswordEncoder());
    }

    public AppContext(ConnectionProvider connectionProvider, PasswordEncoder passwordEncoder) {
        this.connectionProvider = connectionProvider;
        TransactionManager tx = new TransactionManager(connectionProvider);

        UserDao userDao = new UserDao();
        PatientDao patientDao = new PatientDao();
        DoctorDao doctorDao = new DoctorDao();
        SupplierDao supplierDao = new SupplierDao();
        MedicineDao medicineDao = new MedicineDao();
        BatchDao batchDao = new BatchDao();
        StockTransactionDao stockTransactionDao = new StockTransactionDao();
        PrescriptionDao prescriptionDao = new PrescriptionDao();
        DispensingDao dispensingDao = new DispensingDao();
        InvoiceDao invoiceDao = new InvoiceDao();
        SequenceDao sequenceDao = new SequenceDao();

        this.auditService = new AuditService(tx, new AuditLogDao());
        this.settingsService = new SettingsService(tx, auditService, new SettingDao());
        this.authService = new AuthService(tx, auditService, userDao, passwordEncoder);
        this.userService = new UserService(tx, auditService, userDao, passwordEncoder);
        this.patientService = new PatientService(tx, auditService, patientDao, sequenceDao);
        this.doctorService = new DoctorService(tx, auditService, doctorDao, sequenceDao);
        this.supplierService = new SupplierService(tx, auditService, supplierDao, sequenceDao);
        this.medicineService = new MedicineService(tx, auditService, medicineDao, sequenceDao);
        this.inventoryService = new InventoryService(tx, auditService, medicineDao, batchDao, stockTransactionDao,
                supplierDao, settingsService);
        this.prescriptionService = new PrescriptionService(tx, auditService, prescriptionDao, patientDao, doctorDao,
                medicineDao, sequenceDao);
        this.dispensingService = new DispensingService(tx, auditService, prescriptionDao, dispensingDao, batchDao,
                sequenceDao, inventoryService, new FefoAllocationStrategy());
        this.billingService = new BillingService(tx, auditService, invoiceDao, dispensingDao, sequenceDao, settingsService);
        this.reportService = new ReportService(tx, auditService, new ReportDao(), settingsService);
        this.dashboardService = new DashboardService(tx, auditService, new DashboardDao(), patientDao, doctorDao,
                prescriptionDao, medicineDao, batchDao, dispensingDao, invoiceDao, settingsService);
    }

    public void close() {
        connectionProvider.close();
    }

    public AuditService getAuditService() {
        return auditService;
    }

    public SettingsService getSettingsService() {
        return settingsService;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public UserService getUserService() {
        return userService;
    }

    public PatientService getPatientService() {
        return patientService;
    }

    public DoctorService getDoctorService() {
        return doctorService;
    }

    public SupplierService getSupplierService() {
        return supplierService;
    }

    public MedicineService getMedicineService() {
        return medicineService;
    }

    public InventoryService getInventoryService() {
        return inventoryService;
    }

    public PrescriptionService getPrescriptionService() {
        return prescriptionService;
    }

    public DispensingService getDispensingService() {
        return dispensingService;
    }

    public BillingService getBillingService() {
        return billingService;
    }

    public ReportService getReportService() {
        return reportService;
    }

    public DashboardService getDashboardService() {
        return dashboardService;
    }
}
