package com.valinor.iposca.gui;

import com.valinor.iposca.dao.SACatalogueDAO;
import com.valinor.iposca.dao.SAConnectionManager;
import com.valinor.iposca.model.SACatalogueItem;
import com.valinor.iposca.util.AppTheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Order management panel for placing orders with SA and viewing order/invoice history.
 * Requires SA login (merchantId) to operate.
 */
public class SAOrdersPanel extends JPanel {

    private final SACatalogueDAO catalogueDAO;
    private final SAConnectionManager saConnection;

    private JTable catalogueTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JLabel statusLabel;

    private final String[] columns = {
            "Item ID", "Description", "Pkg Type", "Unit", "Units/Pack",
            "Cost/Unit (\u00a3)", "Pack Cost (\u00a3)", "SA Availability"
    };

    public SAOrdersPanel() {
        this.catalogueDAO = new SACatalogueDAO();
        this.saConnection = new SAConnectionManager();

        setLayout(new BorderLayout(0, 0));
        setBackground(AppTheme.bg());

        add(AppTheme.headerBar("Orders - InfoPharma (SA)"), BorderLayout.NORTH);

        JPanel content = AppTheme.contentPanel();

        // top bar: search + login
        Object[] sb = AppTheme.searchBar(this::performSearch, this::refreshTable);
        JPanel searchPanel = (JPanel) sb[0];
        searchField = (JTextField) sb[1];

        JButton loginBtn = AppTheme.primaryBtn("Login to SA");
        loginBtn.addActionListener(e -> doLogin());
        searchPanel.add(loginBtn);

        JButton accountBtn = AppTheme.outlineBtn("Account Info");
        accountBtn.addActionListener(e -> showAccountInfo());
        searchPanel.add(accountBtn);

        content.add(searchPanel, BorderLayout.NORTH);

        // catalogue table for selecting items to order
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        catalogueTable = new JTable(tableModel);
        catalogueTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        AppTheme.styleTable(catalogueTable);

        JScrollPane sp = new JScrollPane(catalogueTable);
        AppTheme.styleScrollPane(sp);
        content.add(sp, BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);

        // bottom bar
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(AppTheme.bg());
        bottomBar.setBorder(BorderFactory.createEmptyBorder(0, 15, 10, 15));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        btnPanel.setBackground(AppTheme.bg());

        JButton orderBtn = AppTheme.btn("Place Order (Selected Items)");
        orderBtn.addActionListener(e -> placeOrder());
        btnPanel.add(orderBtn);

        JButton viewOrdersBtn = AppTheme.btn("View My Orders");
        viewOrdersBtn.addActionListener(e -> viewOrders());
        btnPanel.add(viewOrdersBtn);

        JButton viewInvoicesBtn = AppTheme.btn("View My Invoices");
        viewInvoicesBtn.addActionListener(e -> viewInvoices());
        btnPanel.add(viewInvoicesBtn);

        JButton refreshBtn = AppTheme.btn("Refresh");
        refreshBtn.addActionListener(e -> refreshTable());
        btnPanel.add(refreshBtn);

        bottomBar.add(btnPanel, BorderLayout.CENTER);

        statusLabel = new JLabel("Not logged in to SA — click 'Login to SA' to connect");
        statusLabel.setFont(AppTheme.BODY);
        statusLabel.setForeground(AppTheme.textSec());
        bottomBar.add(statusLabel, BorderLayout.SOUTH);

        add(bottomBar, BorderLayout.SOUTH);

        refreshTable();
    }

    // ==================== LOGIN ====================

    private void doLogin() {
        if (saConnection.isLoggedIn()) {
            JOptionPane.showMessageDialog(this,
                    "Already logged in as " + saConnection.getMerchantId());
            return;
        }

        JTextField userField = new JTextField("cosymed", 15);
        JPasswordField passField = new JPasswordField("bondstreet", 15);

        JPanel loginPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        loginPanel.add(new JLabel("SA Username:"));
        loginPanel.add(userField);
        loginPanel.add(new JLabel("SA Password:"));
        loginPanel.add(passField);

        int result = JOptionPane.showConfirmDialog(this, loginPanel,
                "Login to InfoPharma (SA)", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        String user = userField.getText().trim();
        String pass = new String(passField.getPassword()).trim();

        if (saConnection.login(user, pass)) {
            statusLabel.setText("Connected as " + saConnection.getMerchantId()
                    + " | Status: " + saConnection.getAccountStatus());
            statusLabel.setForeground(AppTheme.green());
            JOptionPane.showMessageDialog(this, "Logged in successfully.",
                    "SA Login", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Login failed. Check credentials or SA connection.",
                    "SA Login Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean ensureLoggedIn() {
        if (saConnection.isLoggedIn()) return true;
        JOptionPane.showMessageDialog(this,
                "You must log in to SA first. Click 'Login to SA'.",
                "Not Logged In", JOptionPane.WARNING_MESSAGE);
        return false;
    }

    // ==================== TABLE ====================

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (SACatalogueItem item : catalogueDAO.getAll()) {
            tableModel.addRow(new Object[]{
                    item.getItemId(),
                    item.getDescription(),
                    item.getPackageType(),
                    item.getUnit(),
                    item.getUnitsPerPack(),
                    String.format("%.2f", item.getCostPerUnit()),
                    String.format("%.2f", item.getPackCost()),
                    item.getAvailability()
            });
        }
    }

    private void performSearch() {
        String kw = searchField.getText().trim();
        if (kw.isEmpty()) { refreshTable(); return; }

        tableModel.setRowCount(0);
        List<SACatalogueItem> results = catalogueDAO.search(kw);
        for (SACatalogueItem item : results) {
            tableModel.addRow(new Object[]{
                    item.getItemId(),
                    item.getDescription(),
                    item.getPackageType(),
                    item.getUnit(),
                    item.getUnitsPerPack(),
                    String.format("%.2f", item.getCostPerUnit()),
                    String.format("%.2f", item.getPackCost()),
                    item.getAvailability()
            });
        }
        if (results.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No items found matching: " + kw,
                    "Search", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // ==================== PLACE ORDER ====================

    private void placeOrder() {
        if (!ensureLoggedIn()) return;

        int[] selectedRows = catalogueTable.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this,
                    "Select one or more items from the table to order.");
            return;
        }

        List<Map<String, String>> orderLines = new ArrayList<>();
        StringBuilder summary = new StringBuilder("Order Summary:\n\n");
        double total = 0;

        for (int row : selectedRows) {
            String itemId = (String) tableModel.getValueAt(row, 0);
            String desc = (String) tableModel.getValueAt(row, 1);
            String costStr = (String) tableModel.getValueAt(row, 5);

            String qtyInput = JOptionPane.showInputDialog(this,
                    "Quantity for " + itemId + " - " + desc
                            + "\n(SA Availability: " + tableModel.getValueAt(row, 7) + ")",
                    "1");

            if (qtyInput == null) return;

            try {
                int qty = Integer.parseInt(qtyInput.trim());
                if (qty <= 0) {
                    JOptionPane.showMessageDialog(this, "Quantity must be positive.");
                    return;
                }

                Map<String, String> line = new LinkedHashMap<>();
                line.put("itemId", itemId);
                line.put("quantity", String.valueOf(qty));
                orderLines.add(line);

                double lineCost = Double.parseDouble(costStr) * qty;
                total += lineCost;
                summary.append(String.format("  %s - %s x%d = \u00a3%.2f\n",
                        itemId, desc, qty, lineCost));

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid quantity.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        summary.append(String.format("\nTotal: \u00a3%.2f", total));
        summary.append("\n\nConfirm order?");

        int confirm = JOptionPane.showConfirmDialog(this, summary.toString(),
                "Confirm Order to InfoPharma", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        String orderId = saConnection.placeOrder(orderLines);
        if (orderId != null) {
            JOptionPane.showMessageDialog(this,
                    "Order placed successfully!\nSA Order ID: " + orderId,
                    "Order Confirmed", JOptionPane.INFORMATION_MESSAGE);
            // re-sync catalogue availability
            List<SACatalogueItem> updated = saConnection.fetchCatalogue();
            if (!updated.isEmpty()) {
                catalogueDAO.replaceAll(updated);
                refreshTable();
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "Order failed. Possible reasons:\n"
                            + "- Account suspended\n- Exceeds credit limit\n- Insufficient SA stock",
                    "Order Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==================== VIEW ORDERS ====================

    private void viewOrders() {
        if (!ensureLoggedIn()) return;

        List<Map<String, String>> orders = saConnection.getOrders();
        if (orders.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No orders found.",
                    "Orders", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String[] cols = {"Order ID", "Date", "Status", "Total", "Discount", "Net", "Courier"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        for (Map<String, String> o : orders) {
            model.addRow(new Object[]{
                    o.get("orderId"),
                    o.get("orderDate"),
                    o.get("orderStatus"),
                    "\u00a3" + o.get("totalAmount"),
                    "\u00a3" + o.get("discountAmount"),
                    "\u00a3" + o.get("netAmount"),
                    o.get("courier")
            });
        }

        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        AppTheme.styleTable(table);

        JScrollPane sp = new JScrollPane(table);
        sp.setPreferredSize(new Dimension(700, 300));
        AppTheme.styleScrollPane(sp);

        JOptionPane.showMessageDialog(this, sp,
                "Orders from InfoPharma", JOptionPane.PLAIN_MESSAGE);
    }

    // ==================== VIEW INVOICES ====================

    private void viewInvoices() {
        if (!ensureLoggedIn()) return;

        List<Map<String, String>> invoices = saConnection.getInvoices();
        if (invoices.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No invoices found.",
                    "Invoices", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String[] cols = {"Invoice ID", "Order ID", "Date", "Total", "Discount", "Net", "Paid?"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        for (Map<String, String> inv : invoices) {
            model.addRow(new Object[]{
                    inv.get("invoiceId"),
                    inv.get("orderId"),
                    inv.get("invoiceDate"),
                    "\u00a3" + inv.get("totalAmount"),
                    "\u00a3" + inv.get("discount"),
                    "\u00a3" + inv.get("netAmount"),
                    "true".equals(inv.get("isPaid")) ? "Yes" : "No"
            });
        }

        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        AppTheme.styleTable(table);

        JScrollPane sp = new JScrollPane(table);
        sp.setPreferredSize(new Dimension(700, 300));
        AppTheme.styleScrollPane(sp);

        JOptionPane.showMessageDialog(this, sp,
                "Invoices from InfoPharma", JOptionPane.PLAIN_MESSAGE);
    }

    // ==================== ACCOUNT INFO ====================

    private void showAccountInfo() {
        if (!ensureLoggedIn()) return;

        Map<String, String> info = saConnection.getBalanceAndStatus();
        if (info == null) {
            JOptionPane.showMessageDialog(this, "Could not retrieve account info.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String msg = String.format(
                "Merchant ID: %s\n"
                        + "Account Name: %s\n"
                        + "Current Balance: \u00a3%s\n"
                        + "Credit Limit: \u00a3%s\n"
                        + "Account Status: %s\n"
                        + "Debtor Reminder: %s",
                info.get("merchantId"),
                info.get("accountName"),
                info.get("currentBalance"),
                info.get("creditLimit"),
                info.get("accountStatus"),
                "true".equals(info.get("showDebtorReminder")) ? "YES - payment overdue" : "No"
        );

        JOptionPane.showMessageDialog(this, msg,
                "SA Account Info", JOptionPane.INFORMATION_MESSAGE);
    }
}
