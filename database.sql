-- =====================================================================
--  Pharmacy Prescription Management System - MySQL 8 schema
--  Import with:   mysql -u root -p < database.sql
--  The script is safe to run on an empty server. It does NOT create any
--  login account: the first administrator is created from the /setup page.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS pharmacy_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pharmacy_db;

-- ---------------------------------------------------------------------
-- users : application accounts (passwords are BCrypt hashes only)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    username       VARCHAR(50)  NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    full_name      VARCHAR(120) NOT NULL,
    email          VARCHAR(150) NULL,
    phone          VARCHAR(20)  NULL,
    role           ENUM('ADMIN','PHARMACIST') NOT NULL,
    active         TINYINT(1)   NOT NULL DEFAULT 1,
    last_login_at  DATETIME     NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- patients
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS patients (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    patient_code   VARCHAR(20)  NOT NULL,
    full_name      VARCHAR(120) NOT NULL,
    date_of_birth  DATE         NULL,
    gender         ENUM('MALE','FEMALE','OTHER') NOT NULL,
    phone          VARCHAR(20)  NOT NULL,
    email          VARCHAR(150) NULL,
    address        VARCHAR(300) NULL,
    active         TINYINT(1)   NOT NULL DEFAULT 1,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_patients_code (patient_code),
    KEY idx_patients_name (full_name),
    KEY idx_patients_phone (phone)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- doctors
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS doctors (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    doctor_code      VARCHAR(20)  NOT NULL,
    full_name        VARCHAR(120) NOT NULL,
    specialization   VARCHAR(100) NOT NULL,
    phone            VARCHAR(20)  NOT NULL,
    email            VARCHAR(150) NULL,
    hospital         VARCHAR(150) NULL,
    registration_no  VARCHAR(50)  NULL,
    active           TINYINT(1)   NOT NULL DEFAULT 1,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_doctors_code (doctor_code),
    UNIQUE KEY uk_doctors_registration (registration_no),
    KEY idx_doctors_name (full_name)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- suppliers
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS suppliers (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    supplier_code   VARCHAR(20)  NOT NULL,
    name            VARCHAR(150) NOT NULL,
    contact_person  VARCHAR(120) NULL,
    phone           VARCHAR(20)  NOT NULL,
    email           VARCHAR(150) NULL,
    address         VARCHAR(300) NULL,
    active          TINYINT(1)   NOT NULL DEFAULT 1,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_suppliers_code (supplier_code),
    KEY idx_suppliers_name (name)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- medicines : the catalog (no stock quantity here - stock lives in batches)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS medicines (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    medicine_code  VARCHAR(20)  NOT NULL,
    name           VARCHAR(150) NOT NULL,
    generic_name   VARCHAR(150) NULL,
    category       VARCHAR(80)  NOT NULL,
    manufacturer   VARCHAR(150) NULL,
    dosage_form    VARCHAR(50)  NOT NULL,
    strength       VARCHAR(50)  NULL,
    unit           VARCHAR(30)  NOT NULL,
    reorder_level  INT          NOT NULL DEFAULT 0,
    description    VARCHAR(500) NULL,
    active         TINYINT(1)   NOT NULL DEFAULT 1,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_medicines_code (medicine_code),
    KEY idx_medicines_name (name),
    KEY idx_medicines_category (category),
    CONSTRAINT chk_medicines_reorder CHECK (reorder_level >= 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- medicine_batches : one row per received batch of a medicine
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS medicine_batches (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    medicine_id     BIGINT        NOT NULL,
    batch_number    VARCHAR(50)   NOT NULL,
    expiry_date     DATE          NOT NULL,
    purchase_price  DECIMAL(12,2) NOT NULL,
    selling_price   DECIMAL(12,2) NOT NULL,
    quantity        INT           NOT NULL DEFAULT 0,
    supplier_id     BIGINT        NULL,
    received_date   DATE          NOT NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_batches_medicine_batch (medicine_id, batch_number),
    KEY idx_batches_expiry (expiry_date),
    KEY idx_batches_supplier (supplier_id),
    CONSTRAINT fk_batches_medicine FOREIGN KEY (medicine_id) REFERENCES medicines (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_batches_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_batches_quantity CHECK (quantity >= 0),
    CONSTRAINT chk_batches_prices CHECK (purchase_price >= 0 AND selling_price >= 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- prescriptions + prescription_items
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS prescriptions (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    prescription_no    VARCHAR(20)  NOT NULL,
    patient_id         BIGINT       NOT NULL,
    doctor_id          BIGINT       NOT NULL,
    prescription_date  DATE         NOT NULL,
    diagnosis          VARCHAR(500) NULL,
    notes              VARCHAR(1000) NULL,
    status             ENUM('DRAFT','ISSUED','PARTIALLY_DISPENSED','COMPLETED','CANCELLED') NOT NULL DEFAULT 'DRAFT',
    created_by         BIGINT       NOT NULL,
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_prescriptions_no (prescription_no),
    KEY idx_prescriptions_patient (patient_id),
    KEY idx_prescriptions_doctor (doctor_id),
    KEY idx_prescriptions_status (status),
    KEY idx_prescriptions_date (prescription_date),
    CONSTRAINT fk_prescriptions_patient FOREIGN KEY (patient_id) REFERENCES patients (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_prescriptions_doctor FOREIGN KEY (doctor_id) REFERENCES doctors (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_prescriptions_user FOREIGN KEY (created_by) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS prescription_items (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    prescription_id  BIGINT       NOT NULL,
    medicine_id      BIGINT       NOT NULL,
    dosage           VARCHAR(100) NOT NULL,
    frequency        VARCHAR(100) NOT NULL,
    duration         VARCHAR(100) NULL,
    instructions     VARCHAR(300) NULL,
    quantity         INT          NOT NULL,
    PRIMARY KEY (id),
    KEY idx_pitems_prescription (prescription_id),
    KEY idx_pitems_medicine (medicine_id),
    -- items belong to their prescription; they are only ever removed while it is still editable
    CONSTRAINT fk_pitems_prescription FOREIGN KEY (prescription_id) REFERENCES prescriptions (id)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT fk_pitems_medicine FOREIGN KEY (medicine_id) REFERENCES medicines (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_pitems_quantity CHECK (quantity > 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- dispensings + dispensing_items
-- request_token makes a submitted dispensing form idempotent: a repeated
-- submission of the same form cannot insert (and deduct stock) twice.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dispensings (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    dispensing_no    VARCHAR(20)  NOT NULL,
    prescription_id  BIGINT       NOT NULL,
    dispensed_by     BIGINT       NOT NULL,
    dispensed_at     DATETIME     NOT NULL,
    request_token    VARCHAR(64)  NOT NULL,
    notes            VARCHAR(300) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_dispensings_no (dispensing_no),
    UNIQUE KEY uk_dispensings_token (request_token),
    KEY idx_dispensings_prescription (prescription_id),
    KEY idx_dispensings_date (dispensed_at),
    CONSTRAINT fk_dispensings_prescription FOREIGN KEY (prescription_id) REFERENCES prescriptions (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_dispensings_user FOREIGN KEY (dispensed_by) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS dispensing_items (
    id                    BIGINT        NOT NULL AUTO_INCREMENT,
    dispensing_id         BIGINT        NOT NULL,
    prescription_item_id  BIGINT        NOT NULL,
    medicine_id           BIGINT        NOT NULL,
    batch_id              BIGINT        NOT NULL,
    quantity              INT           NOT NULL,
    unit_price            DECIMAL(12,2) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_ditems_dispensing (dispensing_id),
    KEY idx_ditems_pitem (prescription_item_id),
    KEY idx_ditems_batch (batch_id),
    CONSTRAINT fk_ditems_dispensing FOREIGN KEY (dispensing_id) REFERENCES dispensings (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_ditems_pitem FOREIGN KEY (prescription_item_id) REFERENCES prescription_items (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_ditems_medicine FOREIGN KEY (medicine_id) REFERENCES medicines (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_ditems_batch FOREIGN KEY (batch_id) REFERENCES medicine_batches (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_ditems_quantity CHECK (quantity > 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- stock_transactions : append-only ledger of every stock movement
-- quantity is always positive; txn_type gives the direction.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS stock_transactions (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    batch_id        BIGINT       NOT NULL,
    medicine_id     BIGINT       NOT NULL,
    txn_type        ENUM('STOCK_IN','STOCK_OUT','ADJUSTMENT_IN','ADJUSTMENT_OUT','DISPENSE') NOT NULL,
    quantity        INT          NOT NULL,
    balance_after   INT          NOT NULL,
    reason          VARCHAR(300) NULL,
    reference_type  VARCHAR(30)  NULL,
    reference_id    BIGINT       NULL,
    user_id         BIGINT       NOT NULL,
    created_at      DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_stx_batch (batch_id),
    KEY idx_stx_medicine (medicine_id),
    KEY idx_stx_created (created_at),
    KEY idx_stx_type (txn_type),
    CONSTRAINT fk_stx_batch FOREIGN KEY (batch_id) REFERENCES medicine_batches (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_stx_medicine FOREIGN KEY (medicine_id) REFERENCES medicines (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_stx_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_stx_quantity CHECK (quantity > 0),
    CONSTRAINT chk_stx_balance CHECK (balance_after >= 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- invoices + invoice_items + payments
-- Totals are stored on purpose: an invoice is a frozen legal record of what
-- was charged at that moment (tax rate and prices may change later). They
-- are calculated on the server and written in the same transaction as the
-- invoice items, so they cannot drift from the line items.
--
-- active_dispensing_id is NULL for cancelled invoices, so the UNIQUE key
-- guarantees at most one live invoice per dispensing (no duplicate billing)
-- while still keeping cancelled invoices for the audit trail.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS invoices (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    invoice_no       VARCHAR(20)   NOT NULL,
    patient_id       BIGINT        NOT NULL,
    dispensing_id    BIGINT        NOT NULL,
    subtotal         DECIMAL(12,2) NOT NULL,
    discount_amount  DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    tax_rate         DECIMAL(5,2)  NOT NULL,
    tax_amount       DECIMAL(12,2) NOT NULL,
    grand_total      DECIMAL(12,2) NOT NULL,
    status           ENUM('ISSUED','CANCELLED') NOT NULL DEFAULT 'ISSUED',
    payment_status   ENUM('UNPAID','PARTIALLY_PAID','PAID') NOT NULL DEFAULT 'UNPAID',
    created_by       BIGINT        NOT NULL,
    created_at       DATETIME      NOT NULL,
    cancelled_by     BIGINT        NULL,
    cancelled_at     DATETIME      NULL,
    cancel_reason    VARCHAR(300)  NULL,
    active_dispensing_id BIGINT GENERATED ALWAYS AS
        (CASE WHEN status = 'CANCELLED' THEN NULL ELSE dispensing_id END) STORED,
    PRIMARY KEY (id),
    UNIQUE KEY uk_invoices_no (invoice_no),
    UNIQUE KEY uk_invoices_active_dispensing (active_dispensing_id),
    KEY idx_invoices_patient (patient_id),
    KEY idx_invoices_dispensing (dispensing_id),
    KEY idx_invoices_created (created_at),
    KEY idx_invoices_payment_status (payment_status),
    CONSTRAINT fk_invoices_patient FOREIGN KEY (patient_id) REFERENCES patients (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_invoices_dispensing FOREIGN KEY (dispensing_id) REFERENCES dispensings (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_invoices_user FOREIGN KEY (created_by) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_invoices_cancel_user FOREIGN KEY (cancelled_by) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_invoices_amounts CHECK (subtotal >= 0 AND discount_amount >= 0 AND tax_rate >= 0
        AND tax_amount >= 0 AND grand_total >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS invoice_items (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    invoice_id          BIGINT        NOT NULL,
    dispensing_item_id  BIGINT        NOT NULL,
    medicine_id         BIGINT        NOT NULL,
    batch_id            BIGINT        NOT NULL,
    quantity            INT           NOT NULL,
    unit_price          DECIMAL(12,2) NOT NULL,
    discount_amount     DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    line_total          DECIMAL(12,2) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_iitems_invoice (invoice_id),
    KEY idx_iitems_ditem (dispensing_item_id),
    CONSTRAINT fk_iitems_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_iitems_ditem FOREIGN KEY (dispensing_item_id) REFERENCES dispensing_items (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_iitems_medicine FOREIGN KEY (medicine_id) REFERENCES medicines (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_iitems_batch FOREIGN KEY (batch_id) REFERENCES medicine_batches (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_iitems_values CHECK (quantity > 0 AND unit_price >= 0 AND discount_amount >= 0 AND line_total >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS payments (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    invoice_id   BIGINT        NOT NULL,
    amount       DECIMAL(12,2) NOT NULL,
    method       VARCHAR(40)   NOT NULL,
    reference    VARCHAR(100)  NULL,
    received_by  BIGINT        NOT NULL,
    paid_at      DATETIME      NOT NULL,
    PRIMARY KEY (id),
    KEY idx_payments_invoice (invoice_id),
    KEY idx_payments_paid_at (paid_at),
    CONSTRAINT fk_payments_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_payments_user FOREIGN KEY (received_by) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_payments_amount CHECK (amount > 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- audit_logs : who did what and when (no passwords, no clinical details)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_logs (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NULL,
    action       VARCHAR(40)  NOT NULL,
    entity_type  VARCHAR(40)  NOT NULL,
    entity_id    BIGINT       NULL,
    description  VARCHAR(300) NOT NULL,
    ip_address   VARCHAR(45)  NULL,
    created_at   DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_audit_user (user_id),
    KEY idx_audit_entity (entity_type, entity_id),
    KEY idx_audit_created (created_at),
    KEY idx_audit_action (action),
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Supporting tables (in addition to the 15 required ones)
--   settings  : configurable values (tax rate, thresholds, payment methods)
--   sequences : counters used to build readable IDs such as PAT-000001
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS settings (
    setting_key    VARCHAR(60)  NOT NULL,
    setting_value  VARCHAR(500) NULL,
    description    VARCHAR(300) NOT NULL,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (setting_key)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sequences (
    name      VARCHAR(30) NOT NULL,
    next_val  BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (name)
) ENGINE=InnoDB;

INSERT IGNORE INTO sequences (name, next_val) VALUES
    ('PATIENT', 0), ('DOCTOR', 0), ('SUPPLIER', 0), ('MEDICINE', 0),
    ('PRESCRIPTION', 0), ('DISPENSING', 0), ('INVOICE', 0);

-- tax_rate_percent is deliberately left empty: billing is blocked until an
-- administrator enters a value in Settings (enter 0 if no tax applies).
INSERT IGNORE INTO settings (setting_key, setting_value, description) VALUES
    ('pharmacy_name',      'My Pharmacy', 'Pharmacy name printed on prescriptions and invoices'),
    ('pharmacy_address',   '',            'Pharmacy address printed on invoices'),
    ('pharmacy_phone',     '',            'Pharmacy phone printed on invoices'),
    ('pharmacy_license',   '',            'Licence / registration number printed on invoices'),
    ('currency_symbol',    'Rs.',         'Currency symbol shown next to amounts'),
    ('tax_rate_percent',   NULL,          'Tax percentage applied to invoices (0 for none). Must be set before billing.'),
    ('discounts_enabled',  'false',       'Allow a discount amount on invoice line items (true/false)'),
    ('payment_methods',    'Cash,Card,UPI', 'Comma separated list of accepted payment methods'),
    ('near_expiry_days',   '90',          'Batches expiring within this many days are flagged as near expiry'),
    ('default_reorder_level', '10',       'Reorder level suggested when adding a new medicine');
