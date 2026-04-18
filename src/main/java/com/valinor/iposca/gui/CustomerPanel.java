package com.valinor.iposca.gui;

import com.valinor.iposca.dao.CustomerDAO;
import com.valinor.iposca.model.AccountHolder;
import com.valinor.iposca.util.AppTheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;


// Customer account management screen.
// Create, edit, delete customers, record payments,
// generate reminders and monthly statements.
public class CustomerPanel extends JPanel {

    private CustomerDAO customerDAO;
    private JTable customerTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private String userRole;

    private final String[] columns = {
            "Acc ID", "First Name", "Last Name", "Phone", "Email",
            "Credit Limit (£)", "Balance (£)", "Discount", "Status",
            "1st Reminder", "2nd Reminder"
    };

    public CustomerPanel(String role) {
        this.userRole = role;
        customerDAO = new CustomerDAO();
        setLayout(new BorderLayout(0, 0));
        setBackground(AppTheme.bg());

        add(AppTheme.headerBar("Customer Accounts"), BorderLayout.NORTH);

        // content area
        JPanel content = AppTheme.contentPanel();

        // search bar
        Object[] sb = AppTheme.searchBar(this::performSearch, this::refreshTable);
        content.add((JPanel) sb[0], BorderLayout.NORTH);
        searchField = (JTextField) sb[1];

        // table
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        customerTable = new JTable(tableModel);
        customerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        AppTheme.styleTable(customerTable);

        JScrollPane sp = new JScrollPane(customerTable);
        AppTheme.styleScrollPane(sp);
        content.add(sp, BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);

        // two rows of buttons
        JPanel btnArea = new JPanel(new GridLayout(2, 1, 0, 2));
        btnArea.setBackground(AppTheme.bg());
        btnArea.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // row 1: account management
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        row1.setBackground(AppTheme.bg());

        JButton addBtn = AppTheme.btn("Add Customer");
        addBtn.addActionListener(e -> showAddCustomerDialog());
        row1.add(addBtn);

        JButton editBtn = AppTheme.btn("Edit Customer");
        editBtn.addActionListener(e -> showEditCustomerDialog());
        row1.add(editBtn);

        JButton delBtn = AppTheme.btn("Delete Customer");
        delBtn.addActionListener(e -> deleteSelectedCustomer());
        row1.add(delBtn);

        JButton viewBtn = AppTheme.btn("View Details");
        viewBtn.addActionListener(e -> viewCustomerDetails());
        row1.add(viewBtn);

        JButton refBtn = AppTheme.btn("Refresh");
        refBtn.addActionListener(e -> refreshTable());
        row1.add(refBtn);

        btnArea.add(row1);

        // row 2: payments and reminders
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        row2.setBackground(AppTheme.bg());

        JButton payBtn = AppTheme.btn("Record Payment");
        payBtn.addActionListener(e -> showRecordPaymentDialog());
        row2.add(payBtn);

        JButton restoreBtn = AppTheme.btn("Restore Account");
        restoreBtn.addActionListener(e -> restoreSelectedAccount());
        row2.add(restoreBtn);

        JButton statusBtn = AppTheme.btn("Update Statuses");
        statusBtn.addActionListener(e -> runStatusUpdate());
        row2.add(statusBtn);

        JButton remBtn = AppTheme.btn("Generate Reminders");
        remBtn.addActionListener(e -> generateReminders());
        row2.add(remBtn);

        JButton stmtBtn = AppTheme.btn("Monthly Statements");
        stmtBtn.addActionListener(e -> generateStatements());
        row2.add(stmtBtn);

        btnArea.add(row2);
        add(btnArea, BorderLayout.SOUTH);

        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (AccountHolder h : customerDAO.getAllAccountHolders()) {
            String disc;
            if ("fixed".equals(h.getDiscountType())) disc = "Fixed " + h.getDiscountRate() + "%";
            else if ("flexible".equals(h.getDiscountType())) disc = "Flexible";
            else disc = "None";

            tableModel.addRow(new Object[]{
                    h.getAccountId(), h.getFirstName(), h.getLastName(),
                    h.getPhone(), h.getEmail(),
                    String.format("%.2f", h.getCreditLimit()),
                    String.format("%.2f", h.getOutstandingBalance()),
                    disc, h.getAccountStatus(),
                    h.getStatus1stReminder(), h.getStatus2ndReminder()
            });
        }
    }

    private void performSearch() {
        String kw = searchField.getText().trim();
        if (kw.isEmpty()) { refreshTable(); return; }

        tableModel.setRowCount(0);
        List<AccountHolder> results = customerDAO.searchAccountHolders(kw);
        for (AccountHolder h : results) {
            String disc;
            if ("fixed".equals(h.getDiscountType())) disc = "Fixed " + h.getDiscountRate() + "%";
            else if ("flexible".equals(h.getDiscountType())) disc = "Flexible";
            else disc = "None";

            tableModel.addRow(new Object[]{
                    h.getAccountId(), h.getFirstName(), h.getLastName(),
                    h.getPhone(), h.getEmail(),
                    String.format("%.2f", h.getCreditLimit()),
                    String.format("%.2f", h.getOutstandingBalance()),
                    disc, h.getAccountStatus(),
                    h.getStatus1stReminder(), h.getStatus2ndReminder()
            });
        }
        if (results.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No customers found matching: " + kw);
        }
    }

    private void showAddCustomerDialog() {
        JTextField firstF = new JTextField(15), lastF = new JTextField(15),
                addrF = new JTextField(20), phoneF = new JTextField(15),
                emailF = new JTextField(20), creditF = new JTextField(10),
                rateF = new JTextField("0.0", 5);
        JComboBox<String> discBox = new JComboBox<>(new String[]{"none", "fixed", "flexible"});
        rateF.setEnabled(false);
        discBox.addActionListener(e -> {
            rateF.setEnabled("fixed".equals(discBox.getSelectedItem()));
            if (!"fixed".equals(discBox.getSelectedItem())) rateF.setText("0.0");
        });

        JPanel f = new JPanel(new GridLayout(8, 2, 5, 5));
        f.add(new JLabel("First Name:*")); f.add(firstF);
        f.add(new JLabel("Last Name:*"));  f.add(lastF);
        f.add(new JLabel("Address:"));     f.add(addrF);
        f.add(new JLabel("Phone:"));       f.add(phoneF);
        f.add(new JLabel("Email:"));       f.add(emailF);
        f.add(new JLabel("Credit Limit (£):*")); f.add(creditF);
        f.add(new JLabel("Discount Type:"));     f.add(discBox);
        f.add(new JLabel("Discount Rate (%) [fixed only]:"));  f.add(rateF);

        if (JOptionPane.showConfirmDialog(this, f, "Add New Customer",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                if (firstF.getText().trim().isEmpty() || lastF.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "First and Last name are required.");
                    return;
                }
                AccountHolder h = new AccountHolder();
                h.setFirstName(firstF.getText().trim());
                h.setLastName(lastF.getText().trim());
                h.setAddress(addrF.getText().trim());
                h.setPhone(phoneF.getText().trim());
                h.setEmail(emailF.getText().trim());
                h.setCreditLimit(Double.parseDouble(creditF.getText().trim()));
                h.setDiscountType((String) discBox.getSelectedItem());
                h.setDiscountRate(Double.parseDouble(rateF.getText().trim()));

                int id = customerDAO.createAccountHolder(h);
                if (id > 0) {
                    JOptionPane.showMessageDialog(this, "Customer created. Account ID: " + id);
                    refreshTable();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to create customer.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Enter valid numbers for numeric fields.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showEditCustomerDialog() {
        int r = customerTable.getSelectedRow();
        if (r == -1) { JOptionPane.showMessageDialog(this, "Select a customer to edit."); return; }

        int id = (int) tableModel.getValueAt(r, 0);
        AccountHolder h = customerDAO.getAccountHolderById(id);
        if (h == null) { JOptionPane.showMessageDialog(this, "Customer not found."); return; }

        JTextField accIdF = new JTextField("" + h.getAccountId(), 10),
                firstF = new JTextField(h.getFirstName(), 15),
                lastF = new JTextField(h.getLastName(), 15),
                addrF = new JTextField(h.getAddress() != null ? h.getAddress() : "", 20),
                phoneF = new JTextField(h.getPhone() != null ? h.getPhone() : "", 15),
                emailF = new JTextField(h.getEmail() != null ? h.getEmail() : "", 20),
                creditF = new JTextField("" + h.getCreditLimit(), 10),
                rateF = new JTextField("" + h.getDiscountRate(), 5);
        JComboBox<String> discBox = new JComboBox<>(new String[]{"none", "fixed", "flexible"});
        discBox.setSelectedItem(h.getDiscountType());
        rateF.setEnabled("fixed".equals(h.getDiscountType()));

        JButton tiersBtn = AppTheme.btn("Edit Tiers...");
        tiersBtn.setEnabled("flexible".equals(h.getDiscountType()));

        discBox.addActionListener(e -> {
            String sel = (String) discBox.getSelectedItem();
            rateF.setEnabled("fixed".equals(sel));
            tiersBtn.setEnabled("flexible".equals(sel));
            if (!"fixed".equals(sel)) rateF.setText("0.0");
        });

        final int[] currentId = {id};
        tiersBtn.addActionListener(e -> showEditTiersDialog(currentId[0]));

        JPanel f = new JPanel(new GridLayout(10, 2, 5, 5));
        f.add(new JLabel("Account ID:"));    f.add(accIdF);
        f.add(new JLabel("First Name:"));    f.add(firstF);
        f.add(new JLabel("Last Name:"));     f.add(lastF);
        f.add(new JLabel("Address:"));       f.add(addrF);
        f.add(new JLabel("Phone:"));         f.add(phoneF);
        f.add(new JLabel("Email:"));         f.add(emailF);
        f.add(new JLabel("Credit Limit (£):")); f.add(creditF);
        f.add(new JLabel("Discount Type:"));     f.add(discBox);
        f.add(new JLabel("Fixed Rate (%):"));    f.add(rateF);
        f.add(new JLabel("Variable Tiers:"));    f.add(tiersBtn);

        if (JOptionPane.showConfirmDialog(this, f, "Edit: " + h.getFullName(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                int newId = Integer.parseInt(accIdF.getText().trim());
                if (newId != id) {
                    if (!customerDAO.changeAccountId(id, newId)) {
                        JOptionPane.showMessageDialog(this,
                                "Failed to change Account ID. It may already be in use.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    h = customerDAO.getAccountHolderById(newId);
                    if (h == null) return;
                }

                h.setFirstName(firstF.getText().trim());
                h.setLastName(lastF.getText().trim());
                h.setAddress(addrF.getText().trim());
                h.setPhone(phoneF.getText().trim());
                h.setEmail(emailF.getText().trim());
                h.setCreditLimit(Double.parseDouble(creditF.getText().trim()));
                h.setDiscountType((String) discBox.getSelectedItem());
                h.setDiscountRate(Double.parseDouble(rateF.getText().trim()));

                if (customerDAO.updateAccountHolder(h)) {
                    JOptionPane.showMessageDialog(this, "Customer updated.");
                    refreshTable();
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Enter valid numbers.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showEditTiersDialog(int accountId) {
        List<double[]> existing = customerDAO.getDiscountTiers(accountId);

        DefaultTableModel tierModel = new DefaultTableModel(
                new String[]{"Min Value (£)", "Max Value (£)", "Discount %"}, 0);

        for (double[] tier : existing) {
            tierModel.addRow(new Object[]{
                    String.format("%.2f", tier[0]),
                    tier[1] < 0 ? "No limit" : String.format("%.2f", tier[1]),
                    String.format("%.1f", tier[2])
            });
        }

        JTable tierTable = new JTable(tierModel);
        AppTheme.styleTable(tierTable);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JScrollPane sp = new JScrollPane(tierTable);
        sp.setPreferredSize(new Dimension(400, 150));
        AppTheme.styleScrollPane(sp);
        panel.add(sp, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        btns.setBackground(AppTheme.bg());

        JButton addBtn = AppTheme.btn("Add Tier");
        addBtn.addActionListener(e -> {
            JTextField minF = new JTextField(10), maxF = new JTextField(10), rateF = new JTextField(5);
            JPanel addPanel = new JPanel(new GridLayout(3, 2, 5, 5));
            addPanel.add(new JLabel("Min Value (£):"));  addPanel.add(minF);
            addPanel.add(new JLabel("Max Value (£) [blank = no limit]:"));  addPanel.add(maxF);
            addPanel.add(new JLabel("Discount Rate (%):"));  addPanel.add(rateF);

            if (JOptionPane.showConfirmDialog(panel, addPanel, "Add Tier",
                    JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                try {
                    String minStr = minF.getText().trim();
                    String maxStr = maxF.getText().trim();
                    String rateStr = rateF.getText().trim();
                    tierModel.addRow(new Object[]{
                            String.format("%.2f", Double.parseDouble(minStr)),
                            maxStr.isEmpty() ? "No limit" : String.format("%.2f", Double.parseDouble(maxStr)),
                            String.format("%.1f", Double.parseDouble(rateStr))
                    });
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(panel, "Enter valid numbers.");
                }
            }
        });
        btns.add(addBtn);

        JButton rmBtn = AppTheme.btn("Remove Selected");
        rmBtn.addActionListener(e -> {
            int row = tierTable.getSelectedRow();
            if (row >= 0) tierModel.removeRow(row);
        });
        btns.add(rmBtn);

        panel.add(btns, BorderLayout.SOUTH);

        if (JOptionPane.showConfirmDialog(this, panel, "Variable Discount Tiers",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {

            List<double[]> tiers = new ArrayList<>();
            for (int i = 0; i < tierModel.getRowCount(); i++) {
                double min = Double.parseDouble(tierModel.getValueAt(i, 0).toString().replace(",", ""));
                String maxStr = tierModel.getValueAt(i, 1).toString();
                double max = "No limit".equals(maxStr) ? -1 : Double.parseDouble(maxStr.replace(",", ""));
                double rate = Double.parseDouble(tierModel.getValueAt(i, 2).toString().replace("%", ""));
                tiers.add(new double[]{min, max, rate});
            }

            if (customerDAO.saveDiscountTiers(accountId, tiers)) {
                JOptionPane.showMessageDialog(this, "Tiers saved.");
            } else {
                JOptionPane.showMessageDialog(this, "Failed to save tiers.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }




    private void deleteSelectedCustomer() {
        int r = customerTable.getSelectedRow();
        if (r == -1) { JOptionPane.showMessageDialog(this, "Select a customer to delete."); return; }

        int id = (int) tableModel.getValueAt(r, 0);
        String name = tableModel.getValueAt(r, 1) + " " + tableModel.getValueAt(r, 2);

        if (JOptionPane.showConfirmDialog(this, "Delete " + name + " (ID: " + id + ")?",
                "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
            if (customerDAO.deleteAccountHolder(id)) {
                JOptionPane.showMessageDialog(this, "Customer deleted.");
                refreshTable();
            }
        }
    }

    private void viewCustomerDetails() {
        int r = customerTable.getSelectedRow();
        if (r == -1) { JOptionPane.showMessageDialog(this, "Select a customer to view."); return; }

        int id = (int) tableModel.getValueAt(r, 0);
        AccountHolder h = customerDAO.getAccountHolderById(id);
        if (h == null) return;

        String info = String.format(
                "Account ID: %d\nName: %s\nAddress: %s\nPhone: %s\nEmail: %s\n" +
                        "Credit Limit: £%.2f\nBalance: £%.2f\nAvailable Credit: £%.2f\n" +
                        "Discount: %s (%.1f%%)\nStatus: %s\n" +
                        "1st Reminder: %s\n2nd Reminder: %s\nCreated: %s",
                h.getAccountId(), h.getFullName(),
                h.getAddress() != null ? h.getAddress() : "N/A",
                h.getPhone() != null ? h.getPhone() : "N/A",
                h.getEmail() != null ? h.getEmail() : "N/A",
                h.getCreditLimit(), h.getOutstandingBalance(),
                h.getCreditLimit() - h.getOutstandingBalance(),
                h.getDiscountType(), h.getDiscountRate(), h.getAccountStatus(),
                h.getStatus1stReminder(), h.getStatus2ndReminder(),
                h.getCreatedAt() != null ? h.getCreatedAt() : "N/A"
        );
        JOptionPane.showMessageDialog(this, info, h.getFullName(), JOptionPane.INFORMATION_MESSAGE);
    }

    private void showRecordPaymentDialog() {
        int r = customerTable.getSelectedRow();
        if (r == -1) { JOptionPane.showMessageDialog(this, "Select a customer."); return; }

        int id = (int) tableModel.getValueAt(r, 0);
        AccountHolder h = customerDAO.getAccountHolderById(id);
        if (h == null) return;
        if (h.getOutstandingBalance() <= 0) {
            JOptionPane.showMessageDialog(this, h.getFullName() + " has no outstanding balance.");
            return;
        }

        JTextField amtF = new JTextField(String.format("%.2f", h.getOutstandingBalance()), 10);
        JComboBox<String> methodBox = new JComboBox<>(new String[]{"card", "cash"});
        JTextField cardTypeF = new JTextField(10), first4F = new JTextField(4),
                last4F = new JTextField(4), expiryF = new JTextField(7);

        JPanel f = new JPanel(new GridLayout(8, 2, 5, 5));
        f.add(new JLabel("Customer:")); f.add(new JLabel(h.getFullName()));
        f.add(new JLabel("Balance:")); f.add(new JLabel("£" + String.format("%.2f", h.getOutstandingBalance())));
        f.add(new JLabel("Amount (£):")); f.add(amtF);
        f.add(new JLabel("Method:")); f.add(methodBox);
        f.add(new JLabel("Card Type:")); f.add(cardTypeF);
        f.add(new JLabel("First 4 Digits:")); f.add(first4F);
        f.add(new JLabel("Last 4 Digits:")); f.add(last4F);
        f.add(new JLabel("Expiry (MM/YY):")); f.add(expiryF);

        if (JOptionPane.showConfirmDialog(this, f, "Record Payment",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                double amt = Double.parseDouble(amtF.getText().trim());
                if (amt <= 0 || amt > h.getOutstandingBalance()) {
                    JOptionPane.showMessageDialog(this, "Amount must be between £0.01 and £" +
                            String.format("%.2f", h.getOutstandingBalance()));
                    return;
                }
                String method = (String) methodBox.getSelectedItem();
                String ct = "cash".equals(method) ? null : cardTypeF.getText().trim();
                String cf = "cash".equals(method) ? null : first4F.getText().trim();
                String cl = "cash".equals(method) ? null : last4F.getText().trim();
                String ce = "cash".equals(method) ? null : expiryF.getText().trim();

                if (customerDAO.recordPayment(id, amt, method, ct, cf, cl, ce)) {
                    String msg = "Payment of £" + String.format("%.2f", amt) + " recorded.";
                    if (amt >= h.getOutstandingBalance() && !"in default".equals(h.getAccountStatus())) {
                        msg += "\nBalance cleared. Account restored to normal.";
                    }
                    JOptionPane.showMessageDialog(this, msg);
                    refreshTable();
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Enter a valid amount.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void restoreSelectedAccount() {
        if (!"Manager".equals(userRole) && !"Admin".equals(userRole)) {
            JOptionPane.showMessageDialog(this,
                    "Only a Manager or Admin can restore accounts.",
                    "Access Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int r = customerTable.getSelectedRow();
        if (r == -1) { JOptionPane.showMessageDialog(this, "Select a customer."); return; }

        int id = (int) tableModel.getValueAt(r, 0);
        AccountHolder h = customerDAO.getAccountHolderById(id);
        if (h == null) return;
        if ("normal".equals(h.getAccountStatus())) {
            JOptionPane.showMessageDialog(this, "Already in normal status.");
            return;
        }
        if (!"in default".equals(h.getAccountStatus())) {
            JOptionPane.showMessageDialog(this, "Account is not in default. Only accounts in default can be restored.");
            return;
        }

        // Check if outstanding balance has been cleared
        if (h.getOutstandingBalance() > 0) {
            JOptionPane.showMessageDialog(this,
                    "Cannot restore — outstanding balance of £"
                            + String.format("%.2f", h.getOutstandingBalance())
                            + " must be cleared first.\nRecord a payment before restoring.",
                    "Payment Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (JOptionPane.showConfirmDialog(this,
                "Restore " + h.getFullName() + " to normal?\nCurrent: " + h.getAccountStatus(),
                "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            if (customerDAO.restoreAccountStatus(id)) {
                JOptionPane.showMessageDialog(this, "Account restored to normal.");
                refreshTable();
            }
        }
    }


    private void runStatusUpdate() {
        if (JOptionPane.showConfirmDialog(this,
                "Update all account statuses based on today's date?\n" +
                        "Unpaid accounts may be suspended or set to default.",
                "Update Statuses", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            customerDAO.runAccountStatusUpdate();
            JOptionPane.showMessageDialog(this, "Statuses updated.");
            refreshTable();
        }
    }

    private void generateReminders() {
        List<String> reminders = customerDAO.generateReminders();
        if (reminders.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No reminders due at this time.");
            return;
        }
        StringBuilder all = new StringBuilder();
        for (String r : reminders) all.append(r).append("\n");

        JTextArea ta = new JTextArea(all.toString());
        ta.setFont(AppTheme.MONO);
        ta.setEditable(false);
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(550, 380));
        JOptionPane.showMessageDialog(this, sp, reminders.size() + " Reminder(s)", JOptionPane.INFORMATION_MESSAGE);
        refreshTable();
    }

    private void generateStatements() {
        List<String> stmts = customerDAO.generateMonthlyStatements();
        if (stmts == null) {
            JOptionPane.showMessageDialog(this, "Statements can only be generated between the 5th and 15th.",
                    "Outside Period", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (stmts.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No outstanding balances.");
            return;
        }
        StringBuilder all = new StringBuilder();
        for (String s : stmts) all.append(s).append("\n");

        JTextArea ta = new JTextArea(all.toString());
        ta.setFont(AppTheme.MONO);
        ta.setEditable(false);
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(550, 380));
        JOptionPane.showMessageDialog(this, sp, stmts.size() + " Statement(s)", JOptionPane.INFORMATION_MESSAGE);
    }
}