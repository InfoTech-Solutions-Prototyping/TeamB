package com.valinor.iposca.gui;

import com.valinor.iposca.dao.PUOrderDAO;
import com.valinor.iposca.util.AppTheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class PUOrderPanel extends JPanel {

    private final PUOrderDAO dao = new PUOrderDAO();
    private final DefaultTableModel model;
    private final JTable table;

    private static final String[] STATUSES = {
            "Accepted", "Ready for Shipment", "Shipped", "Delivered"
    };

    public PUOrderPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(AppTheme.bg());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top bar
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        topBar.setBackground(AppTheme.bg());

        JButton newOrderBtn = AppTheme.primaryBtn("New Order");
        newOrderBtn.addActionListener(e -> showNewOrderDialog());
        topBar.add(newOrderBtn);

        JButton statusBtn = AppTheme.btn("Update Status");
        statusBtn.addActionListener(e -> updateSelectedStatus());
        topBar.add(statusBtn);

        JButton deleteBtn = AppTheme.btn("Delete Order");
        deleteBtn.addActionListener(e -> deleteSelectedOrder());
        topBar.add(deleteBtn);

        JButton refreshBtn = AppTheme.btn("Refresh");
        refreshBtn.addActionListener(e -> loadOrders());
        topBar.add(refreshBtn);

        add(topBar, BorderLayout.NORTH);

        // Orders table
        String[] columns = {"Order ID", "Customer", "Email", "Items", "Date", "Status"};
        model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        AppTheme.styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(130);
        table.getColumnModel().getColumn(2).setPreferredWidth(160);
        table.getColumnModel().getColumn(3).setPreferredWidth(300);
        table.getColumnModel().getColumn(4).setPreferredWidth(90);
        table.getColumnModel().getColumn(5).setPreferredWidth(130);

        JScrollPane scroll = new JScrollPane(table);
        add(scroll, BorderLayout.CENTER);

        // Details area
        JTextArea detailsArea = new JTextArea(4, 0);
        detailsArea.setEditable(false);
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        detailsArea.setBackground(AppTheme.surface());
        detailsArea.setForeground(AppTheme.text());
        detailsArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        detailsArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    List<String[]> orders = dao.getAllOrders();
                    if (row < orders.size()) {
                        String notes = orders.get(row)[6];
                        detailsArea.setText("Items: " + model.getValueAt(row, 3)
                                + "\nNotes: " + (notes != null ? notes : "None"));
                    }
                }
            }
        });

        JScrollPane detailScroll = new JScrollPane(detailsArea);
        detailScroll.setPreferredSize(new Dimension(0, 100));
        add(detailScroll, BorderLayout.SOUTH);

        loadOrders();
    }

    private void loadOrders() {
        model.setRowCount(0);
        List<String[]> orders = dao.getAllOrders();
        for (String[] o : orders) {
            model.addRow(new Object[]{o[0], o[1], o[2], o[3], o[4], o[5]});
        }
    }

    private void showNewOrderDialog() {
        JPanel form = new JPanel(new BorderLayout(8, 8));

        // customer details
        JPanel custPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        JTextField nameF = new JTextField();
        JTextField emailF = new JTextField();
        custPanel.add(new JLabel("Customer Name:"));   custPanel.add(nameF);
        custPanel.add(new JLabel("Customer Email:"));   custPanel.add(emailF);
        form.add(custPanel, BorderLayout.NORTH);

        String[] itemCols = {"Item ID", "Description", "Quantity", "Unit Cost (£)", "Total (£)"};
        DefaultTableModel itemModel = new DefaultTableModel(itemCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c != 4; }
        };
        JTable itemTable = new JTable(itemModel);
        itemTable.setRowHeight(28);

        // Auto calculate total when quantity or unit cost changes
        itemModel.addTableModelListener(e -> {
            int row = e.getFirstRow();
            int col = e.getColumn();
            if ((col == 2 || col == 3) && row >= 0 && row < itemModel.getRowCount()) {
                try {
                    double qty = Double.parseDouble(itemModel.getValueAt(row, 2).toString());
                    double cost = Double.parseDouble(itemModel.getValueAt(row, 3).toString());
                    itemModel.setValueAt(String.format("%.2f", qty * cost), row, 4);
                } catch (NumberFormatException ignored) {}
            }
        });

        JScrollPane tableScroll = new JScrollPane(itemTable);
        tableScroll.setPreferredSize(new Dimension(500, 150));

        JPanel itemBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addRowBtn = new JButton("Add Item");
        addRowBtn.addActionListener(e -> itemModel.addRow(new Object[]{"", "", "1", "0.00", "0.00"}));
        JButton removeRowBtn = new JButton("Remove Item");
        removeRowBtn.addActionListener(e -> {
            int sel = itemTable.getSelectedRow();
            if (sel >= 0) itemModel.removeRow(sel);
        });
        itemBtnPanel.add(addRowBtn);
        itemBtnPanel.add(removeRowBtn);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(new JLabel("Order Items:"), BorderLayout.NORTH);
        centerPanel.add(tableScroll, BorderLayout.CENTER);
        centerPanel.add(itemBtnPanel, BorderLayout.SOUTH);
        form.add(centerPanel, BorderLayout.CENTER);

        JTextArea notesF = new JTextArea(2, 20);
        notesF.setLineWrap(true);
        JPanel notesPanel = new JPanel(new BorderLayout());
        notesPanel.add(new JLabel("Notes:"), BorderLayout.NORTH);
        notesPanel.add(new JScrollPane(notesF), BorderLayout.CENTER);
        form.add(notesPanel, BorderLayout.SOUTH);

        // Add one empty row to start
        itemModel.addRow(new Object[]{"", "", "1", "0.00", "0.00"});

        int result = JOptionPane.showConfirmDialog(this, form,
                "New PU Order", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String name = nameF.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Customer name is required.");
                return;
            }
            if (itemModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "Add at least one item.");
                return;
            }

            // Build items string from table
            StringBuilder sb = new StringBuilder();
            double orderTotal = 0;
            for (int i = 0; i < itemModel.getRowCount(); i++) {
                String itemId = itemModel.getValueAt(i, 0).toString().trim();
                String desc = itemModel.getValueAt(i, 1).toString().trim();
                String qty = itemModel.getValueAt(i, 2).toString().trim();
                String cost = itemModel.getValueAt(i, 3).toString().trim();
                String total = itemModel.getValueAt(i, 4).toString().trim();
                if (itemId.isEmpty() && desc.isEmpty()) continue;
                if (sb.length() > 0) sb.append("\n");
                sb.append(itemId).append(" | ").append(desc)
                        .append(" | Qty: ").append(qty)
                        .append(" | £").append(cost)
                        .append(" | Total: £").append(total);
                try { orderTotal += Double.parseDouble(total); } catch (NumberFormatException ignored) {}
            }
            sb.append("\n--- Order Total: £").append(String.format("%.2f", orderTotal)).append(" ---");

            String items = sb.toString();
            if (items.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Add at least one item.");
                return;
            }

            int id = dao.createOrder(name, emailF.getText().trim(), items, notesF.getText().trim());
            if (id > 0) {
                JOptionPane.showMessageDialog(this, "Order #" + id + " created (Accepted).\nTotal: £"
                        + String.format("%.2f", orderTotal));
                loadOrders();
            }
        }
    }


    private void updateSelectedStatus() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an order first.");
            return;
        }

        String currentStatus = model.getValueAt(row, 5).toString();
        int currentIndex = -1;
        for (int i = 0; i < STATUSES.length; i++) {
            if (STATUSES[i].equals(currentStatus)) { currentIndex = i; break; }
        }

        if (currentIndex == STATUSES.length - 1) {
            JOptionPane.showMessageDialog(this, "Order is already Delivered.");
            return;
        }

        String nextStatus = STATUSES[currentIndex + 1];
        int confirm = JOptionPane.showConfirmDialog(this,
                "Change status from \"" + currentStatus + "\" to \"" + nextStatus + "\"?",
                "Update Status", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            int orderId = Integer.parseInt(model.getValueAt(row, 0).toString());
            if (dao.updateStatus(orderId, nextStatus)) {
                loadOrders();
            }
        }
    }

    private void deleteSelectedOrder() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an order first.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete this order?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            int orderId = Integer.parseInt(model.getValueAt(row, 0).toString());
            dao.deleteOrder(orderId);
            loadOrders();
        }
    }
}
