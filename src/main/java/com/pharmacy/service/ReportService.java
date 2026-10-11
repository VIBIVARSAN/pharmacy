package com.pharmacy.service;

import com.pharmacy.dao.ReportDao;
import com.pharmacy.model.Permission;
import com.pharmacy.model.ReportResult;
import com.pharmacy.model.User;
import com.pharmacy.util.PageRequest;
import com.pharmacy.util.TransactionManager;

import java.time.LocalDate;

public class ReportService extends BaseService {
    private final ReportDao reportDao;
    private final SettingsService settings;

    public ReportService(TransactionManager tx, AuditService audit, ReportDao reportDao, SettingsService settings) {
        super(tx, audit);
        this.reportDao = reportDao;
        this.settings = settings;
    }

    public ReportResult sales(User actor, LocalDate from, LocalDate to, String groupBy, PageRequest request) {
        require(actor, Permission.REPORT_VIEW);
        return tx.read(con -> reportDao.sales(con, from, to, groupBy, request));
    }

    public ReportResult prescriptions(User actor, LocalDate from, LocalDate to, String status, PageRequest request) {
        require(actor, Permission.REPORT_VIEW);
        return tx.read(con -> reportDao.prescriptions(con, from, to, status, request));
    }

    public ReportResult inventory(User actor, PageRequest request) {
        require(actor, Permission.REPORT_VIEW);
        return tx.read(con -> reportDao.inventory(con, LocalDate.now(), request));
    }

    public ReportResult lowStock(User actor, PageRequest request) {
        require(actor, Permission.REPORT_VIEW);
        return tx.read(con -> reportDao.lowStock(con, LocalDate.now(), request));
    }

    public ReportResult expiry(User actor, String type, PageRequest request) {
        require(actor, Permission.REPORT_VIEW);
        return tx.read(con -> reportDao.expiry(con, LocalDate.now(), settings.getNearExpiryDays(), type, request));
    }

    public ReportResult stockMovements(User actor, LocalDate from, LocalDate to, String type, PageRequest request) {
        require(actor, Permission.REPORT_VIEW);
        return tx.read(con -> reportDao.stockMovements(con, from, to, type, request));
    }

    public ReportResult supplierReceipts(User actor, LocalDate from, LocalDate to, Long supplierId, PageRequest request) {
        require(actor, Permission.REPORT_VIEW);
        return tx.read(con -> reportDao.supplierReceipts(con, from, to, supplierId, request));
    }

    public ReportResult billing(User actor, LocalDate from, LocalDate to, String paymentStatus, PageRequest request) {
        require(actor, Permission.REPORT_VIEW);
        return tx.read(con -> reportDao.billing(con, from, to, paymentStatus, request));
    }
}
