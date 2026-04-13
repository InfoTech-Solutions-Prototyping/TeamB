-- ============================================================
-- IPOS-CA Database Schema
-- Team 14 (Valinor) - InfoPharma Ordering System Client App
-- ============================================================

-- ========================
-- IPOS-CA-USER TABLES
-- ========================

-- Stores all users who can log into IPOS-CA (Pharmacist, Admin, Manager)
CREATE TABLE IF NOT EXISTS users (
    user_id     INTEGER PRIMARY KEY AUTOINCREMENT,
    username    TEXT    NOT NULL UNIQUE,
    password    TEXT    NOT NULL,
    role        TEXT    NOT NULL CHECK (role IN ('Pharmacist', 'Admin', 'Manager')),
    is_active   INTEGER NOT NULL DEFAULT 1,
    created_at  TEXT    NOT NULL DEFAULT (datetime('now'))
);

-- ========================
-- IPOS-CA-STOCK TABLES
-- ========================

-- Stores system-wide settings like VAT rate
CREATE TABLE IF NOT EXISTS system_config (
    config_key   TEXT PRIMARY KEY,
    config_value TEXT NOT NULL
);

-- Stores each product the pharmacy has in stock
CREATE TABLE IF NOT EXISTS stock_items (
    item_id         TEXT    PRIMARY KEY,
    description     TEXT    NOT NULL,
    package_type    TEXT    NOT NULL,
    unit            TEXT    NOT NULL,
    units_in_pack   INTEGER NOT NULL,
    bulk_cost       REAL    NOT NULL,
    markup_rate     REAL    NOT NULL DEFAULT 0.0,
    availability    INTEGER NOT NULL DEFAULT 0,
    stock_limit     INTEGER NOT NULL DEFAULT 0
);

-- Records every delivery received from InfoPharma
CREATE TABLE IF NOT EXISTS deliveries (
    delivery_id     INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id         TEXT    NOT NULL,
    quantity        INTEGER NOT NULL,
    delivery_date   TEXT    NOT NULL DEFAULT (datetime('now')),
    notes           TEXT,
    FOREIGN KEY (item_id) REFERENCES stock_items(item_id)
);

-- ========================
-- IPOS-CA-CUST TABLES
-- ========================

-- Stores customer account holders
CREATE TABLE IF NOT EXISTS account_holders (
    account_id          INTEGER PRIMARY KEY AUTOINCREMENT,
    first_name          TEXT    NOT NULL,
    last_name           TEXT    NOT NULL,
    address             TEXT,
    phone               TEXT,
    email               TEXT,
    credit_limit        REAL    NOT NULL DEFAULT 0.0,
    outstanding_balance REAL    NOT NULL DEFAULT 0.0,
    discount_type       TEXT    NOT NULL DEFAULT 'none' CHECK (discount_type IN ('none', 'fixed', 'flexible')),
    discount_rate       REAL    NOT NULL DEFAULT 0.0,
    account_status      TEXT    NOT NULL DEFAULT 'normal' CHECK (account_status IN ('normal', 'suspended', 'in default')),
    status_1st_reminder TEXT    NOT NULL DEFAULT 'no_need' CHECK (status_1st_reminder IN ('no_need', 'due', 'sent')),
    status_2nd_reminder TEXT    NOT NULL DEFAULT 'no_need' CHECK (status_2nd_reminder IN ('no_need', 'due', 'sent')),
    date_1st_reminder   TEXT,
    date_2nd_reminder   TEXT,
    created_at          TEXT    NOT NULL DEFAULT (datetime('now'))
);

-- ========================
-- IPOS-CA-SALES TABLES
-- ========================

-- Stores each sale transaction
CREATE TABLE IF NOT EXISTS sales (
    sale_id         INTEGER PRIMARY KEY AUTOINCREMENT,
    account_id      INTEGER,
    sale_date       TEXT    NOT NULL DEFAULT (datetime('now')),
    subtotal        REAL    NOT NULL,
    vat_amount      REAL    NOT NULL,
    discount_amount REAL    NOT NULL DEFAULT 0.0,
    total           REAL    NOT NULL,
    payment_method  TEXT    NOT NULL CHECK (payment_method IN ('cash', 'card', 'credit')),
    card_type       TEXT,
    card_first_four TEXT,
    card_last_four  TEXT,
    card_expiry     TEXT,
    is_online       INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (account_id) REFERENCES account_holders(account_id)
);

-- Stores individual items within a sale
CREATE TABLE IF NOT EXISTS sale_items (
    sale_item_id INTEGER PRIMARY KEY AUTOINCREMENT,
    sale_id      INTEGER NOT NULL,
    item_id      TEXT    NOT NULL,
    quantity     INTEGER NOT NULL,
    unit_price   REAL    NOT NULL,
    line_total   REAL    NOT NULL,
    FOREIGN KEY (sale_id) REFERENCES sales(sale_id),
    FOREIGN KEY (item_id) REFERENCES stock_items(item_id)
);

-- Stores payments received from account holders
CREATE TABLE IF NOT EXISTS payments_received (
    payment_id   INTEGER PRIMARY KEY AUTOINCREMENT,
    account_id   INTEGER NOT NULL,
    amount       REAL    NOT NULL,
    payment_date TEXT    NOT NULL DEFAULT (datetime('now')),
    method       TEXT    NOT NULL CHECK (method IN ('cash', 'card')),
    card_type       TEXT,
    card_first_four TEXT,
    card_last_four  TEXT,
    card_expiry     TEXT,
    FOREIGN KEY (account_id) REFERENCES account_holders(account_id)
);

-- ========================
-- IPOS-CA-ORD TABLES
-- ========================

-- Stores orders placed with InfoPharma (via IPOS-SA)
CREATE TABLE IF NOT EXISTS orders_to_infopharma (
    order_id     TEXT PRIMARY KEY,
    order_date   TEXT NOT NULL DEFAULT (datetime('now')),
    total_value  REAL NOT NULL,
    status       TEXT NOT NULL DEFAULT 'accepted' CHECK (status IN ('accepted', 'processing', 'dispatched', 'delivered')),
    dispatched_by   TEXT,
    dispatch_date   TEXT,
    courier         TEXT,
    courier_ref     TEXT,
    expected_delivery TEXT
);

-- Items within an order to InfoPharma
CREATE TABLE IF NOT EXISTS order_items (
    order_item_id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id      TEXT    NOT NULL,
    item_id       TEXT    NOT NULL,
    quantity      INTEGER NOT NULL,
    unit_cost     REAL    NOT NULL,
    line_total    REAL    NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders_to_infopharma(order_id),
    FOREIGN KEY (item_id) REFERENCES stock_items(item_id)
);

-- ========================
-- IPOS-CA-TEMPLATES TABLE
-- ========================

-- Stores merchant identity and document templates
CREATE TABLE IF NOT EXISTS merchant_details (
    detail_key   TEXT PRIMARY KEY,
    detail_value TEXT NOT NULL
);

-- ========================
-- INSERT DEFAULT DATA
-- ========================

-- Default admin account (password: admin123)
INSERT OR IGNORE INTO users (username, password, role) VALUES ('admin', 'admin123', 'Admin');

-- Default VAT rate of 0%
INSERT OR IGNORE INTO system_config (config_key, config_value) VALUES ('vat_rate', '0.0');

-- Default merchant details
INSERT OR IGNORE INTO merchant_details (detail_key, detail_value) VALUES ('pharmacy_name', 'My Pharmacy');
INSERT OR IGNORE INTO merchant_details (detail_key, detail_value) VALUES ('address', '123 High Street');
INSERT OR IGNORE INTO merchant_details (detail_key, detail_value) VALUES ('phone', '0208 000 0000');
INSERT OR IGNORE INTO merchant_details (detail_key, detail_value) VALUES ('email', 'info@mypharmacy.co.uk');
