package com.valinor.iposca.dao;

import com.valinor.iposca.db.DatabaseManager;
import com.valinor.iposca.model.AccountHolder;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.sql.Types;


/**
 * Handles all database operations for customer account holders.
 * Includes the automatic account status logic and reminder generation
 * as specified in the IPOS-CA-CUST requirements.
 */
public class CustomerDAO {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Creates a new account holder in the database.
     * Returns the generated account ID, or -1 if it fails.
     */
    public int createAccountHolder(AccountHolder holder) {
        String sql = "INSERT INTO account_holders (first_name, last_name, address, phone, email, " +
                     "credit_limit, discount_type, discount_rate, account_status, " +
                     "status_1st_reminder, status_2nd_reminder) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'normal', 'no_need', 'no_need')";

        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            pstmt.setString(1, holder.getFirstName());
            pstmt.setString(2, holder.getLastName());
            pstmt.setString(3, holder.getAddress());
            pstmt.setString(4, holder.getPhone());
            pstmt.setString(5, holder.getEmail());
            pstmt.setDouble(6, holder.getCreditLimit());
            pstmt.setString(7, holder.getDiscountType());
            pstmt.setDouble(8, holder.getDiscountRate());

            pstmt.executeUpdate();

            // Get the auto-generated account ID
            ResultSet keys = pstmt.getGeneratedKeys();
            int newId = -1;
            if (keys.next()) {
                newId = keys.getInt(1);
            }

            keys.close();
            pstmt.close();
            return newId;

        } catch (SQLException e) {
            System.err.println("Error creating account holder: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Updates an existing account holder's details.
     */
    public boolean updateAccountHolder(AccountHolder holder) {
        String sql = "UPDATE account_holders SET first_name = ?, last_name = ?, address = ?, " +
                     "phone = ?, email = ?, credit_limit = ?, discount_type = ?, discount_rate = ? " +
                     "WHERE account_id = ?";

        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, holder.getFirstName());
            pstmt.setString(2, holder.getLastName());
            pstmt.setString(3, holder.getAddress());
            pstmt.setString(4, holder.getPhone());
            pstmt.setString(5, holder.getEmail());
            pstmt.setDouble(6, holder.getCreditLimit());
            pstmt.setString(7, holder.getDiscountType());
            pstmt.setDouble(8, holder.getDiscountRate());
            pstmt.setInt(9, holder.getAccountId());

            pstmt.executeUpdate();
            pstmt.close();
            return true;

        } catch (SQLException e) {
            System.err.println("Error updating account holder: " + e.getMessage());
            return false;
        }
    }

    /**
     * Changes a customer's account ID.
     * Updates all related records (sales, sale_items, payments) to match.
     */
    public boolean changeAccountId(int oldId, int newId) {
        try {
            Connection conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            try {
                PreparedStatement check = conn.prepareStatement(
                        "SELECT account_id FROM account_holders WHERE account_id = ?");
                check.setInt(1, newId);
                ResultSet rs = check.executeQuery();
                if (rs.next()) {
                    rs.close();
                    check.close();
                    conn.rollback();
                    return false;
                }
                rs.close();
                check.close();

                conn.createStatement().execute("PRAGMA foreign_keys = OFF");

                PreparedStatement p1 = conn.prepareStatement(
                        "UPDATE account_holders SET account_id = ? WHERE account_id = ?");
                p1.setInt(1, newId);
                p1.setInt(2, oldId);
                p1.executeUpdate();
                p1.close();

                PreparedStatement p2 = conn.prepareStatement(
                        "UPDATE sales SET account_id = ? WHERE account_id = ?");
                p2.setInt(1, newId);
                p2.setInt(2, oldId);
                p2.executeUpdate();
                p2.close();

                PreparedStatement p3 = conn.prepareStatement(
                        "UPDATE payments_received SET account_id = ? WHERE account_id = ?");
                p3.setInt(1, newId);
                p3.setInt(2, oldId);
                p3.executeUpdate();
                p3.close();

                conn.createStatement().execute("PRAGMA foreign_keys = ON");

                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                conn.createStatement().execute("PRAGMA foreign_keys = ON");
                System.err.println("Error changing account ID: " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("Error with database connection: " + e.getMessage());
            return false;
        }
    }

    /**
     * Saves variable discount tiers for an account.
     * Deletes existing tiers first, then inserts the new ones.
     */
    public boolean saveDiscountTiers(int accountId, List<double[]> tiers) {
        try {
            Connection conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            try {
                PreparedStatement del = conn.prepareStatement(
                        "DELETE FROM discount_tiers WHERE account_id = ?");
                del.setInt(1, accountId);
                del.executeUpdate();
                del.close();

                PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO discount_tiers (account_id, min_value, max_value, discount_rate) " +
                                "VALUES (?, ?, ?, ?)");

                for (double[] tier : tiers) {
                    ins.setInt(1, accountId);
                    ins.setDouble(2, tier[0]);
                    if (tier[1] < 0) {
                        ins.setNull(3, Types.REAL);
                    } else {
                        ins.setDouble(3, tier[1]);
                    }
                    ins.setDouble(4, tier[2]);
                    ins.executeUpdate();
                }
                ins.close();

                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Error saving discount tiers: " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("Error with database connection: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets discount tiers for an account.
     * Returns list of {minValue, maxValue, discountRate}. maxValue = -1 means unlimited.
     */
    public List<double[]> getDiscountTiers(int accountId) {
        List<double[]> tiers = new ArrayList<>();
        String sql = "SELECT min_value, max_value, discount_rate FROM discount_tiers " +
                "WHERE account_id = ? ORDER BY min_value";

        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, accountId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                double max = rs.getDouble("max_value");
                if (rs.wasNull()) max = -1;
                tiers.add(new double[]{
                        rs.getDouble("min_value"), max, rs.getDouble("discount_rate")
                });
            }
            rs.close();
            pstmt.close();

        } catch (SQLException e) {
            System.err.println("Error getting discount tiers: " + e.getMessage());
        }

        return tiers;
    }

    /**
     * Calculates the variable discount rate for a given subtotal.
     * Looks up which tier the subtotal falls into.
     */
    public double getVariableDiscountRate(int accountId, double subtotal) {
        List<double[]> tiers = getDiscountTiers(accountId);
        for (double[] tier : tiers) {
            double min = tier[0];
            double max = tier[1];
            if (subtotal >= min && (max < 0 || subtotal <= max)) {
                return tier[2];
            }
        }
        return 0.0;
    }





    /**
     * Deletes an account holder and all their related records from the database.
     * Removes sale items, sales, and payments linked to this account first.
     */
    public boolean deleteAccountHolder(int accountId) {
        try {
            Connection conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            try {
                // Delete sale_items for all sales belonging to this account
                PreparedStatement pstmt1 = conn.prepareStatement(
                        "DELETE FROM sale_items WHERE sale_id IN " +
                                "(SELECT sale_id FROM sales WHERE account_id = ?)");
                pstmt1.setInt(1, accountId);
                pstmt1.executeUpdate();
                pstmt1.close();

                // Delete sales for this account
                PreparedStatement pstmt2 = conn.prepareStatement(
                        "DELETE FROM sales WHERE account_id = ?");
                pstmt2.setInt(1, accountId);
                pstmt2.executeUpdate();
                pstmt2.close();

                // Delete payments for this account
                PreparedStatement pstmt3 = conn.prepareStatement(
                        "DELETE FROM payments_received WHERE account_id = ?");
                pstmt3.setInt(1, accountId);
                pstmt3.executeUpdate();
                pstmt3.close();

                // Delete the account holder
                PreparedStatement pstmt4 = conn.prepareStatement(
                        "DELETE FROM account_holders WHERE account_id = ?");
                pstmt4.setInt(1, accountId);
                pstmt4.executeUpdate();
                pstmt4.close();

                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Error deleting account holder: " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("Error with database connection: " + e.getMessage());
            return false;
        }
    }


    /**
     * Gets a single account holder by their ID.
     */
    public AccountHolder getAccountHolderById(int accountId) {
        String sql = "SELECT * FROM account_holders WHERE account_id = ?";

        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, accountId);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                AccountHolder holder = extractAccountHolderFromResultSet(rs);
                rs.close();
                pstmt.close();
                return holder;
            }

            rs.close();
            pstmt.close();

        } catch (SQLException e) {
            System.err.println("Error getting account holder: " + e.getMessage());
        }

        return null;
    }

    /**
     * Returns all account holders from the database.
     */
    public List<AccountHolder> getAllAccountHolders() {
        String sql = "SELECT * FROM account_holders ORDER BY last_name, first_name";
        List<AccountHolder> holders = new ArrayList<>();

        try {
            Connection conn = DatabaseManager.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                holders.add(extractAccountHolderFromResultSet(rs));
            }

            rs.close();
            stmt.close();

        } catch (SQLException e) {
            System.err.println("Error getting all account holders: " + e.getMessage());
        }

        return holders;
    }

    /**
     * Searches account holders by name or account ID.
     */
    public List<AccountHolder> searchAccountHolders(String keyword) {
        String sql = "SELECT * FROM account_holders WHERE " +
                     "first_name LIKE ? OR last_name LIKE ? OR CAST(account_id AS TEXT) LIKE ? " +
                     "ORDER BY last_name, first_name";
        List<AccountHolder> holders = new ArrayList<>();

        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            String searchTerm = "%" + keyword + "%";
            pstmt.setString(1, searchTerm);
            pstmt.setString(2, searchTerm);
            pstmt.setString(3, searchTerm);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                holders.add(extractAccountHolderFromResultSet(rs));
            }

            rs.close();
            pstmt.close();

        } catch (SQLException e) {
            System.err.println("Error searching account holders: " + e.getMessage());
        }

        return holders;
    }

    /**
     * Records a payment from an account holder.
     * Reduces their outstanding balance and updates reminder flags if fully paid.
     * This follows the algorithm from the brief:
     *   if account_status != 'in default':
     *       status_1stReminder = 'no_need'
     *       status_2ndReminder = 'no_need'
     */
    public boolean recordPayment(int accountId, double amount, String method,
                                 String cardType, String cardFirstFour,
                                 String cardLastFour, String cardExpiry) {

        String insertPayment = "INSERT INTO payments_received (account_id, amount, method, " +
                               "card_type, card_first_four, card_last_four, card_expiry) " +
                               "VALUES (?, ?, ?, ?, ?, ?, ?)";

        String updateBalance = "UPDATE account_holders SET outstanding_balance = outstanding_balance - ? " +
                               "WHERE account_id = ?";

        try {
            Connection conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            try {
                // Record the payment
                PreparedStatement pstmt1 = conn.prepareStatement(insertPayment);
                pstmt1.setInt(1, accountId);
                pstmt1.setDouble(2, amount);
                pstmt1.setString(3, method);
                pstmt1.setString(4, cardType);
                pstmt1.setString(5, cardFirstFour);
                pstmt1.setString(6, cardLastFour);
                pstmt1.setString(7, cardExpiry);
                pstmt1.executeUpdate();
                pstmt1.close();

                // Reduce the balance
                PreparedStatement pstmt2 = conn.prepareStatement(updateBalance);
                pstmt2.setDouble(1, amount);
                pstmt2.setInt(2, accountId);
                pstmt2.executeUpdate();
                pstmt2.close();

                // Check if balance is now zero or less (fully paid)
                AccountHolder holder = getAccountHolderById(accountId);
                if (holder != null && holder.getOutstandingBalance() <= 0) {
                    // Reset balance to exactly 0 if it went negative
                    if (holder.getOutstandingBalance() < 0) {
                        PreparedStatement fixBalance = conn.prepareStatement(
                            "UPDATE account_holders SET outstanding_balance = 0 WHERE account_id = ?");
                        fixBalance.setInt(1, accountId);
                        fixBalance.executeUpdate();
                        fixBalance.close();
                    }

                    // Apply the payment-received algorithm from the brief
                    if (!"in default".equals(holder.getAccountStatus())) {
                        PreparedStatement resetReminders = conn.prepareStatement(
                            "UPDATE account_holders SET " +
                            "status_1st_reminder = 'no_need', " +
                            "status_2nd_reminder = 'no_need', " +
                            "account_status = 'normal' " +
                            "WHERE account_id = ?");
                        resetReminders.setInt(1, accountId);
                        resetReminders.executeUpdate();
                        resetReminders.close();
                    }
                }

                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Error recording payment: " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("Error with database connection: " + e.getMessage());
            return false;
        }
    }

    /**
     * Restores an account from "in default" or "suspended" back to "normal".
     * This can only be done by a Manager through explicit intervention.
     */
    public boolean restoreAccountStatus(int accountId) {
        String sql = "UPDATE account_holders SET account_status = 'normal', " +
                     "status_1st_reminder = 'no_need', status_2nd_reminder = 'no_need' " +
                     "WHERE account_id = ?";

        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, accountId);
            pstmt.executeUpdate();
            pstmt.close();
            return true;

        } catch (SQLException e) {
            System.err.println("Error restoring account status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Runs the automatic account status update process.
     * This checks all accounts and updates their status based on the rules in the brief:
     *
     * - End of calendar month: if balance > 0, payment is due (status stays normal)
     * - 15th of next month: if still unpaid, status changes to "suspended", 1st reminder = "due"
     * - End of next month: if still unpaid, status changes to "in default", 2nd reminder = "due"
     *
     * In a real production system this would run automatically on a timer.
     * In our prototype the user triggers it manually.
     */
    public void runAccountStatusUpdate() {
        LocalDate today = LocalDate.now();
        int dayOfMonth = today.getDayOfMonth();

        List<AccountHolder> allHolders = getAllAccountHolders();

        for (AccountHolder holder : allHolders) {
            // Skip accounts with no debt
            if (holder.getOutstandingBalance() <= 0) {
                continue;
            }

            // Skip accounts already in default (only Manager can restore these)
            if ("in default".equals(holder.getAccountStatus())) {
                continue;
            }

            // If we're past the 15th and account is still normal with debt, suspend it
            if ("normal".equals(holder.getAccountStatus()) && dayOfMonth >= 15) {
                updateStatusTo(holder.getAccountId(), "suspended", "due", null);
            }

            // If we're at end of month (day 28+) and account is suspended, set to in default
            if ("suspended".equals(holder.getAccountStatus()) && dayOfMonth >= 28) {
                updateStatusTo(holder.getAccountId(), "in default", null, "due");
            }
        }
    }

    /**
     * Helper method to update account status and reminder flags.
     */
    private void updateStatusTo(int accountId, String newStatus,
                                String new1stReminder, String new2ndReminder) {
        StringBuilder sql = new StringBuilder("UPDATE account_holders SET account_status = ?");

        if (new1stReminder != null) {
            sql.append(", status_1st_reminder = '").append(new1stReminder).append("'");
        }
        if (new2ndReminder != null) {
            sql.append(", status_2nd_reminder = '").append(new2ndReminder).append("'");
        }
        sql.append(" WHERE account_id = ?");

        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql.toString());
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, accountId);
            pstmt.executeUpdate();
            pstmt.close();

        } catch (SQLException e) {
            System.err.println("Error updating account status: " + e.getMessage());
        }
    }

    /**
     * Generates reminders for account holders who have reminders due.
     * Follows the pseudo-code algorithm from the brief exactly:
     *
     * if (status_1stReminder == 'due'):
     *     generate 1st reminder (payment date = current date + 7 days)
     *     status_1stReminder = 'sent'
     *     date_2ndReminder = current date + 15 days
     *
     * if (status_2ndReminder == 'due'):
     *     if (date_2ndReminder <= current date):
     *         generate 2nd reminder (payment date = current date + 7 days)
     *         status_2ndReminder = 'sent'
     *
     * Returns a list of reminder messages that were generated.
     */
    public List<String> generateReminders() {
        List<String> generatedReminders = new ArrayList<>();
        List<AccountHolder> allHolders = getAllAccountHolders();
        LocalDate today = LocalDate.now();

        for (AccountHolder holder : allHolders) {
            // Check if 1st reminder is due
            if ("due".equals(holder.getStatus1stReminder())) {
                LocalDate paymentDate = today.plusDays(7);
                LocalDate secondReminderDate = today.plusDays(15);

                String reminder = generateFirstReminderText(holder, paymentDate);
                generatedReminders.add(reminder);

                // Update: mark 1st reminder as sent, schedule 2nd reminder
                update1stReminderSent(holder.getAccountId(), secondReminderDate.format(DATE_FORMAT));
            }

            // Check if 2nd reminder is due
            if ("due".equals(holder.getStatus2ndReminder())) {
                String dateStr = holder.getDate2ndReminder();

                // Only send if the scheduled date has arrived
                boolean shouldSend = false;
                if (dateStr == null || dateStr.isEmpty() || "now".equals(dateStr)) {
                    shouldSend = true;
                } else {
                    try {
                        LocalDate scheduledDate = LocalDate.parse(dateStr, DATE_FORMAT);
                        shouldSend = !today.isBefore(scheduledDate);
                    } catch (Exception e) {
                        shouldSend = true;
                    }
                }

                if (shouldSend) {
                    LocalDate paymentDate = today.plusDays(7);
                    String reminder = generateSecondReminderText(holder, paymentDate);
                    generatedReminders.add(reminder);

                    // Update: mark 2nd reminder as sent
                    update2ndReminderSent(holder.getAccountId());
                }
            }
        }

        return generatedReminders;
    }

    /**
     * Marks the 1st reminder as sent and schedules the 2nd reminder date.
     */
    private void update1stReminderSent(int accountId, String secondReminderDate) {
        String sql = "UPDATE account_holders SET status_1st_reminder = 'sent', " +
                     "date_2nd_reminder = ? WHERE account_id = ?";

        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, secondReminderDate);
            pstmt.setInt(2, accountId);
            pstmt.executeUpdate();
            pstmt.close();

        } catch (SQLException e) {
            System.err.println("Error updating 1st reminder status: " + e.getMessage());
        }
    }

    /**
     * Marks the 2nd reminder as sent.
     */
    private void update2ndReminderSent(int accountId) {
        String sql = "UPDATE account_holders SET status_2nd_reminder = 'sent' WHERE account_id = ?";

        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, accountId);
            pstmt.executeUpdate();
            pstmt.close();

        } catch (SQLException e) {
            System.err.println("Error updating 2nd reminder status: " + e.getMessage());
        }
    }

    /**
     * Gets all account holders who have outstanding balances (for statement generation).
     */
    public List<AccountHolder> getAccountHoldersWithBalance() {
        String sql = "SELECT * FROM account_holders WHERE outstanding_balance > 0 " +
                     "ORDER BY last_name, first_name";
        List<AccountHolder> holders = new ArrayList<>();

        try {
            Connection conn = DatabaseManager.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                holders.add(extractAccountHolderFromResultSet(rs));
            }

            rs.close();
            stmt.close();

        } catch (SQLException e) {
            System.err.println("Error getting account holders with balance: " + e.getMessage());
        }

        return holders;
    }

    /**
     * Generates the text for a 1st payment reminder.
     * Layout follows Appendix 6 of the brief.
     */
    private String generateFirstReminderText(AccountHolder holder, LocalDate paymentDate) {
        return String.format(
            "========== 1ST REMINDER ==========\n" +
            "To: %s\n" +
            "    %s\n\n" +
            "Date: %s\n\n" +
            "Dear %s,\n\n" +
            "REMINDER - Account No: %d\n" +
            "Outstanding Amount: £%.2f\n\n" +
            "According to our records, it appears that we have not yet received\n" +
            "payment for your outstanding balance.\n\n" +
            "We would appreciate payment by %s.\n\n" +
            "If you have already sent a payment to us recently, please accept\n" +
            "our apologies.\n\n" +
            "Yours sincerely,\n" +
            "The Management\n" +
            "==================================\n",
            holder.getFullName(),
            holder.getAddress() != null ? holder.getAddress() : "",
            LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
            holder.getFullName(),
            holder.getAccountId(),
            holder.getOutstandingBalance(),
            paymentDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        );
    }

    /**
     * Generates the text for a 2nd payment reminder.
     * Layout follows Appendix 6 of the brief.
     */
    private String generateSecondReminderText(AccountHolder holder, LocalDate paymentDate) {
        return String.format(
            "========= 2ND REMINDER ==========\n" +
            "To: %s\n" +
            "    %s\n\n" +
            "Date: %s\n\n" +
            "Dear %s,\n\n" +
            "SECOND REMINDER - Account No: %d\n" +
            "Outstanding Amount: £%.2f\n\n" +
            "It appears that we still have not yet received payment for your\n" +
            "outstanding balance, despite the reminder previously sent to you.\n\n" +
            "We would appreciate it if you would settle this in full by %s.\n\n" +
            "If you have already sent a payment to us recently, please accept\n" +
            "our apologies.\n\n" +
            "Yours sincerely,\n" +
            "The Management\n" +
            "==================================\n",
            holder.getFullName(),
            holder.getAddress() != null ? holder.getAddress() : "",
            LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
            holder.getFullName(),
            holder.getAccountId(),
            holder.getOutstandingBalance(),
            paymentDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        );
    }

    /**
     * Generates monthly statements for all account holders with outstanding balances.
     * Can only be used between the 5th and 15th of the month as per the brief.
     * Returns a list of statement texts, or null if outside the allowed period.
     */
    public List<String> generateMonthlyStatements() {
        LocalDate today = LocalDate.now();
        int day = today.getDayOfMonth();

        // Check if we're within the allowed period (5th to 15th)
        if (day < 5 || day > 15) {
            return null;
        }

        List<String> statements = new ArrayList<>();
        List<AccountHolder> debtors = getAccountHoldersWithBalance();

        for (AccountHolder holder : debtors) {
            String statement = String.format(
                "======== MONTHLY STATEMENT ========\n" +
                "To: %s\n" +
                "    %s\n\n" +
                "Date: %s\n\n" +
                "Account No: %d\n" +
                "Outstanding Balance: £%.2f\n\n" +
                "Please pay the full amount by the 15th of this month.\n\n" +
                "Thank you for your valued custom.\n\n" +
                "Yours sincerely,\n" +
                "The Management\n" +
                "===================================\n",
                holder.getFullName(),
                holder.getAddress() != null ? holder.getAddress() : "",
                today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                holder.getAccountId(),
                holder.getOutstandingBalance()
            );

            statements.add(statement);
        }

        return statements;
    }

    /**
     * Helper method that reads one row from the database and turns it into an AccountHolder object.
     */
    private AccountHolder extractAccountHolderFromResultSet(ResultSet rs) throws SQLException {
        AccountHolder holder = new AccountHolder();
        holder.setAccountId(rs.getInt("account_id"));
        holder.setFirstName(rs.getString("first_name"));
        holder.setLastName(rs.getString("last_name"));
        holder.setAddress(rs.getString("address"));
        holder.setPhone(rs.getString("phone"));
        holder.setEmail(rs.getString("email"));
        holder.setCreditLimit(rs.getDouble("credit_limit"));
        holder.setOutstandingBalance(rs.getDouble("outstanding_balance"));
        holder.setDiscountType(rs.getString("discount_type"));
        holder.setDiscountRate(rs.getDouble("discount_rate"));
        holder.setAccountStatus(rs.getString("account_status"));
        holder.setStatus1stReminder(rs.getString("status_1st_reminder"));
        holder.setStatus2ndReminder(rs.getString("status_2nd_reminder"));
        holder.setDate1stReminder(rs.getString("date_1st_reminder"));
        holder.setDate2ndReminder(rs.getString("date_2nd_reminder"));
        holder.setCreatedAt(rs.getString("created_at"));
        return holder;
    }
}
