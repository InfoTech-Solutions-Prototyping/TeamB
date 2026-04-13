package com.valinor.iposca.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages the database connection and table creation for IPOS-CA.
 * Uses SQLite so no external database server is needed to run the prototype.
 */
public class DatabaseManager {

    // The file where SQLite stores all the data
    private static final String DB_URL = "jdbc:sqlite:ipos_ca.db";

    // Single shared connection used throughout the app
    private static Connection connection;

    /**
     * Returns the database connection. Creates one if it doesn't exist yet.
     */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            // Manually load the SQLite driver (needed for newer JDK versions)
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                throw new SQLException("SQLite JDBC driver not found. Make sure sqlite-jdbc jar is in the classpath.");
            }
            connection = DriverManager.getConnection(DB_URL);
            // Turn on foreign key support (SQLite has it off by default)
            connection.createStatement().execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    /**
     * Creates all the database tables if they don't already exist.
     * This runs once when the application starts up.
     */
    public static void initialiseDatabase() {
        try {
            Connection conn = getConnection();
            Statement stmt = conn.createStatement();

            // ==================== USERS TABLE ====================
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS users (" +
                "    user_id     INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    username    TEXT    NOT NULL UNIQUE," +
                "    password    TEXT    NOT NULL," +
                "    role        TEXT    NOT NULL CHECK (role IN ('Pharmacist', 'Admin', 'Manager'))," +
                "    created_at  TEXT    NOT NULL DEFAULT (datetime('now'))" +
                ")"
            );

            // ==================== SYSTEM CONFIG TABLE ====================
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS system_config (" +
                "    config_key   TEXT PRIMARY KEY," +
                "    config_value TEXT NOT NULL" +
                ")"
            );

            // ==================== STOCK ITEMS TABLE ====================
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS stock_items (" +
                "    item_id         TEXT    PRIMARY KEY," +
                "    description     TEXT    NOT NULL," +
                "    package_type    TEXT    NOT NULL," +
                "    unit            TEXT    NOT NULL," +
                "    units_in_pack   INTEGER NOT NULL," +
                "    bulk_cost       REAL    NOT NULL," +
                "    markup_rate     REAL    NOT NULL DEFAULT 0.0," +
                "    availability    INTEGER NOT NULL DEFAULT 0," +
                "    stock_limit     INTEGER NOT NULL DEFAULT 0" +
                ")"
            );

            // ==================== DELIVERIES TABLE ====================
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS deliveries (" +
                "    delivery_id     INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    item_id         TEXT    NOT NULL," +
                "    quantity        INTEGER NOT NULL," +
                "    delivery_date   TEXT    NOT NULL DEFAULT (datetime('now'))," +
                "    notes           TEXT," +
                "    FOREIGN KEY (item_id) REFERENCES stock_items(item_id)" +
                ")"
            );

            // ==================== ACCOUNT HOLDERS TABLE ====================
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS account_holders (" +
                "    account_id          INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    first_name          TEXT    NOT NULL," +
                "    last_name           TEXT    NOT NULL," +
                "    address             TEXT," +
                "    phone               TEXT," +
                "    email               TEXT," +
                "    credit_limit        REAL    NOT NULL DEFAULT 0.0," +
                "    outstanding_balance REAL    NOT NULL DEFAULT 0.0," +
                "    discount_type       TEXT    NOT NULL DEFAULT 'none' CHECK (discount_type IN ('none', 'fixed', 'flexible'))," +
                "    discount_rate       REAL    NOT NULL DEFAULT 0.0," +
                "    account_status      TEXT    NOT NULL DEFAULT 'normal' CHECK (account_status IN ('normal', 'suspended', 'in default'))," +
                "    status_1st_reminder TEXT    NOT NULL DEFAULT 'no_need' CHECK (status_1st_reminder IN ('no_need', 'due', 'sent'))," +
                "    status_2nd_reminder TEXT    NOT NULL DEFAULT 'no_need' CHECK (status_2nd_reminder IN ('no_need', 'due', 'sent'))," +
                "    date_1st_reminder   TEXT," +
                "    date_2nd_reminder   TEXT," +
                "    created_at          TEXT    NOT NULL DEFAULT (datetime('now'))" +
                ")"
            );

            // ==================== SALES TABLE ====================
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS sales (" +
                "    sale_id         INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    account_id      INTEGER," +
                "    sale_date       TEXT    NOT NULL DEFAULT (datetime('now'))," +
                "    subtotal        REAL    NOT NULL," +
                "    vat_amount      REAL    NOT NULL," +
                "    discount_amount REAL    NOT NULL DEFAULT 0.0," +
                "    total           REAL    NOT NULL," +
                "    payment_method  TEXT    NOT NULL CHECK (payment_method IN ('cash', 'card', 'credit'))," +
                "    card_type       TEXT," +
                "    card_first_four TEXT," +
                "    card_last_four  TEXT," +
                "    card_expiry     TEXT," +
                "    is_online       INTEGER NOT NULL DEFAULT 0," +
                "    FOREIGN KEY (account_id) REFERENCES account_holders(account_id)" +
                ")"
            );

            // ==================== SALE ITEMS TABLE ====================
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS sale_items (" +
                "    sale_item_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    sale_id      INTEGER NOT NULL," +
                "    item_id      TEXT    NOT NULL," +
                "    quantity     INTEGER NOT NULL," +
                "    unit_price   REAL    NOT NULL," +
                "    line_total   REAL    NOT NULL," +
                "    FOREIGN KEY (sale_id) REFERENCES sales(sale_id)," +
                "    FOREIGN KEY (item_id) REFERENCES stock_items(item_id)" +
                ")"
            );

            // ==================== PAYMENTS RECEIVED TABLE ====================
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS payments_received (" +
                "    payment_id   INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    account_id   INTEGER NOT NULL," +
                "    amount       REAL    NOT NULL," +
                "    payment_date TEXT    NOT NULL DEFAULT (datetime('now'))," +
                "    method       TEXT    NOT NULL CHECK (method IN ('cash', 'card'))," +
                "    card_type       TEXT," +
                "    card_first_four TEXT," +
                "    card_last_four  TEXT," +
                "    card_expiry     TEXT," +
                "    FOREIGN KEY (account_id) REFERENCES account_holders(account_id)" +
                ")"
            );

            // ==================== ORDERS TO INFOPHARMA TABLE ====================
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS orders_to_infopharma (" +
                "    order_id     TEXT PRIMARY KEY," +
                "    order_date   TEXT NOT NULL DEFAULT (datetime('now'))," +
                "    total_value  REAL NOT NULL," +
                "    status       TEXT NOT NULL DEFAULT 'accepted' CHECK (status IN ('accepted', 'processing', 'dispatched', 'delivered'))," +
                "    dispatched_by   TEXT," +
                "    dispatch_date   TEXT," +
                "    courier         TEXT," +
                "    courier_ref     TEXT," +
                "    expected_delivery TEXT" +
                ")"
            );

            // ==================== ORDER ITEMS TABLE ====================
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS order_items (" +
                "    order_item_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    order_id      TEXT    NOT NULL," +
                "    item_id       TEXT    NOT NULL," +
                "    quantity      INTEGER NOT NULL," +
                "    unit_cost     REAL    NOT NULL," +
                "    line_total    REAL    NOT NULL," +
                "    FOREIGN KEY (order_id) REFERENCES orders_to_infopharma(order_id)," +
                "    FOREIGN KEY (item_id) REFERENCES stock_items(item_id)" +
                ")"
            );

            // ==================== DISCOUNT TIERS TABLE ====================
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS discount_tiers (" +
                            "    tier_id     INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "    account_id  INTEGER NOT NULL," +
                            "    min_value   REAL    NOT NULL," +
                            "    max_value   REAL," +
                            "    discount_rate REAL  NOT NULL," +
                            "    FOREIGN KEY (account_id) REFERENCES account_holders(account_id)" +
                            ")"
            );



            // ==================== SA CATALOGUE CACHE TABLE ====================
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS sa_catalogue (" +
                            "    item_id         TEXT    PRIMARY KEY," +
                            "    description     TEXT    NOT NULL," +
                            "    package_type    TEXT    NOT NULL," +
                            "    unit            TEXT    NOT NULL," +
                            "    units_per_pack  INTEGER NOT NULL," +
                            "    cost_per_unit   REAL    NOT NULL," +
                            "    availability    INTEGER NOT NULL DEFAULT 0," +
                            "    last_synced     TEXT    NOT NULL DEFAULT (datetime('now'))" +
                            ")"
            );


            // ==================== MERCHANT DETAILS TABLE ====================
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS merchant_details (" +
                "    detail_key   TEXT PRIMARY KEY," +
                "    detail_value TEXT NOT NULL" +
                ")"
            );

            // ==================== INSERT DEFAULT DATA ====================

            // Default admin account so there's always a way to log in
            stmt.execute(
                "INSERT OR IGNORE INTO users (username, password, role) " +
                "VALUES ('admin', 'admin123', 'Admin')"
            );

            // Default VAT rate set to 0%
            stmt.execute(
                "INSERT OR IGNORE INTO system_config (config_key, config_value) " +
                "VALUES ('vat_rate', '0.0')"
            );

            // Default merchant identity details
            stmt.execute("INSERT OR IGNORE INTO merchant_details (detail_key, detail_value) VALUES ('pharmacy_name', 'My Pharmacy')");
            stmt.execute("INSERT OR IGNORE INTO merchant_details (detail_key, detail_value) VALUES ('address', '123 High Street')");
            stmt.execute("INSERT OR IGNORE INTO merchant_details (detail_key, detail_value) VALUES ('phone', '0208 000 0000')");
            stmt.execute("INSERT OR IGNORE INTO merchant_details (detail_key, detail_value) VALUES ('email', 'info@mypharmacy.co.uk')");

            stmt.close();
            System.out.println("Database initialised successfully.");

        } catch (SQLException e) {
            System.err.println("Failed to initialise database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Closes the database connection when the application shuts down.
     */
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing database: " + e.getMessage());
        }
    }
}
